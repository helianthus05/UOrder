package com.example.uorder.core.admin.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import java.io.File;
import java.io.FileOutputStream;
import androidx.core.content.FileProvider;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.OutputStream;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.uorder.R;
import com.example.uorder.core.models.Order;
import com.example.uorder.core.models.Product;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class AdminAnalyticsActivity extends AppCompatActivity {

    private BarChart chartSales;
    private LineChart chartHours;
    private TextView tvTotalProfit;
    private LinearLayout layoutPopularItems;
    private Spinner spinnerType;
    private android.widget.ProgressBar progressBar;
    private FirebaseFirestore db;
    private String stallId;
    
    private List<Order> allOrders = new ArrayList<>();
    private Map<String, String> productTypeMap = new HashMap<>(); // productName -> type
    private String csvContentToSave = "";

    private final ActivityResultLauncher<Intent> saveCsvLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        saveFileToUri(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_analytics);

        db = FirebaseFirestore.getInstance();
        stallId = FirebaseAuth.getInstance().getUid();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        chartSales = findViewById(R.id.chart_sales);
        chartHours = findViewById(R.id.chart_hours);
        tvTotalProfit = findViewById(R.id.tv_total_profit);
        layoutPopularItems = findViewById(R.id.layout_popular_items);
        spinnerType = findViewById(R.id.spinner_type_filter);
        progressBar = findViewById(R.id.progress_bar);

        if (stallId == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupSpinner();
        setupCharts();
        
        fetchProductTypesThenOrders();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.admin_nav_analytics);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.admin_nav_orders) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.admin_nav_menu) {
                startActivity(new Intent(this, AdminMenuManageActivity.class));
                finish();
                return true;
            } else if (id == R.id.admin_nav_analytics) {
                return true;
            } else if (id == R.id.admin_nav_account) {
                startActivity(new Intent(this, com.example.uorder.core.activities.AccountSettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.analytics_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_export_csv) {
            exportToCSV();
            return true;
        } else if (id == R.id.action_clear_analytics) {
            showClearAnalyticsConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showClearAnalyticsConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Analytics")
                .setMessage("Are you sure you want to clear all analytics data? This will permanently delete all order records for your stall.")
                .setPositiveButton("Clear", (dialog, which) -> clearAnalytics())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAnalytics() {
        if (allOrders.isEmpty()) {
            Toast.makeText(this, "No data to clear", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        WriteBatch batch = db.batch();
        for (Order order : allOrders) {
            if (order.getOrderId() != null) {
                batch.delete(db.collection("orders").document(order.getOrderId()));
            }
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Analytics cleared successfully", Toast.LENGTH_SHORT).show();
            allOrders.clear();
            processData();
        }).addOnFailureListener(e -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to clear analytics: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void exportToCSV() {
        if (allOrders.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csvData = new StringBuilder();
        csvData.append("Order ID,Date,Time,Total Amount,Status,Items\n");

        for (Order order : allOrders) {
            StringBuilder items = new StringBuilder();
            if (order.getOrderItems() != null) {
                for (Map.Entry<String, Integer> entry : order.getOrderItems().entrySet()) {
                    items.append(entry.getKey()).append(" (").append(entry.getValue()).append("); ");
                }
            }
            
            String itemsStr = items.toString().replace(",", ";"); 
            csvData.append(String.format(Locale.getDefault(), "%s,%s/%s,%s,%.2f,%s,\"%s\"\n",
                    order.getOrderId() != null ? order.getOrderId() : "N/A",
                    order.getDayString() != null ? order.getDayString() : "00",
                    order.getMonthString() != null ? order.getMonthString() : "00",
                    order.getHourString() != null ? order.getHourString() : "00",
                    order.getTotalAmount(),
                    order.getOrderStatus() != null ? order.getOrderStatus() : "Unknown",
                    itemsStr));
        }

        csvContentToSave = csvData.toString();
        
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "Analytics_" + System.currentTimeMillis() + ".csv");
        saveCsvLauncher.launch(intent);
    }

    private void saveFileToUri(Uri uri) {
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                outputStream.write(csvContentToSave.getBytes());
                outputStream.close();
                Toast.makeText(this, "CSV exported successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSpinner() {
        String[] types = {"All", "Meal", "Snack", "Beverage", "Dessert", "Side Dish"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                processData();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupCharts() {
        chartSales.getDescription().setEnabled(false);
        chartSales.getLegend().setEnabled(false);
        chartHours.getDescription().setEnabled(false);
        chartHours.getLegend().setEnabled(false);
    }

    private void fetchProductTypesThenOrders() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        db.collection("products")
                .whereEqualTo("stallId", stallId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Product p = doc.toObject(Product.class);
                        if (p != null && p.getProductName() != null) {
                            productTypeMap.put(p.getProductName(), p.getProductType());
                        }
                    }
                    fetchOrders();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading product info", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchOrders() {
        db.collection("orders")
                .whereEqualTo("stallId", stallId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    allOrders.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setOrderId(doc.getId());
                        allOrders.add(order);
                    }
                    processData();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error fetching orders", Toast.LENGTH_SHORT).show();
                });
    }

    private void processData() {
        if (spinnerType.getSelectedItem() == null) return;
        String selectedType = spinnerType.getSelectedItem().toString();
        double totalProfit = 0;
        Map<String, Integer> itemCounts = new HashMap<>();
        Map<String, Double> dailyRevenue = new TreeMap<>(); // Date string -> Revenue
        Map<Integer, Integer> hourlyActivity = new TreeMap<>(); // Hour -> Count

        for (Order order : allOrders) {
            if (order == null || order.getOrderItems() == null) continue;

            boolean matchesFilter = "All".equals(selectedType);
            for (Map.Entry<String, Integer> entry : order.getOrderItems().entrySet()) {
                String itemName = entry.getKey();
                int qty = entry.getValue();
                String type = productTypeMap.get(itemName);

                if ("All".equals(selectedType) || (type != null && type.equalsIgnoreCase(selectedType))) {
                    matchesFilter = true;
                    itemCounts.put(itemName, itemCounts.getOrDefault(itemName, 0) + qty);
                }
            }

            if (matchesFilter) {
                if ("Accepted".equals(order.getOrderStatus()) || "Completed".equals(order.getOrderStatus())) {
                    totalProfit += order.getTotalAmount();
                    
                    String day = order.getDayString();
                    String month = order.getMonthString();
                    if (day != null && month != null) {
                        String date = day + "/" + month;
                        dailyRevenue.put(date, dailyRevenue.getOrDefault(date, 0.0) + order.getTotalAmount());
                    }
                }

                String hourStr = order.getHourString();
                if (hourStr != null) {
                    try {
                        int hour = Integer.parseInt(hourStr);
                        hourlyActivity.put(hour, hourlyActivity.getOrDefault(hour, 0) + 1);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        tvTotalProfit.setText(String.format(Locale.getDefault(), "₱%.2f", totalProfit));
        updateSalesChart(dailyRevenue);
        updateHoursChart(hourlyActivity);
        updatePopularItems(itemCounts);
    }

    private void updateSalesChart(Map<String, Double> data) {
        if (data.isEmpty()) {
            chartSales.clear();
            return;
        }
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            entries.add(new BarEntry(i, entry.getValue().floatValue()));
            labels.add(entry.getKey());
            i++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Daily Revenue");
        dataSet.setColor(ContextCompat.getColor(this, R.color.primary));
        BarData barData = new BarData(dataSet);
        chartSales.setData(barData);
        chartSales.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartSales.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartSales.getXAxis().setGranularity(1f);
        chartSales.invalidate();
    }

    private void updateHoursChart(Map<Integer, Integer> data) {
        if (data.isEmpty()) {
            chartHours.clear();
            return;
        }
        List<Entry> entries = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            entries.add(new Entry(h, data.getOrDefault(h, 0)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Hourly Activity");
        dataSet.setColor(ContextCompat.getColor(this, R.color.primary));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary));
        dataSet.setLineWidth(2f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        LineData lineData = new LineData(dataSet);
        chartHours.setData(lineData);
        chartHours.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartHours.getXAxis().setLabelCount(12);
        chartHours.invalidate();
    }

    private void updatePopularItems(Map<String, Integer> itemCounts) {
        layoutPopularItems.removeAllViews();
        List<Map.Entry<String, Integer>> list = new ArrayList<>(itemCounts.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int count = 0;
        for (Map.Entry<String, Integer> entry : list) {
            if (count >= 5) break;
            TextView tv = new TextView(this);
            tv.setText((count + 1) + ". " + entry.getKey() + " (" + entry.getValue() + " sold)");
            tv.setPadding(0, 8, 0, 8);
            tv.setTextSize(14);
            layoutPopularItems.addView(tv);
            count++;
        }
        if (list.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No data available");
            layoutPopularItems.addView(tv);
        }
    }
}
