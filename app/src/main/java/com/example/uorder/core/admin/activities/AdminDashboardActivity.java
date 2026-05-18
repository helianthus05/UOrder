package com.example.uorder.core.admin.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uorder.R;
import com.example.uorder.core.auth.SelectionActivity;
import com.example.uorder.core.auth.SessionManager;
import com.example.uorder.core.admin.adapters.OrderRequestAdapter;
import com.example.uorder.core.models.Order;
import com.example.uorder.core.utils.NotificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private OrderRequestAdapter adapter;
    private List<Order> pendingOrders = new ArrayList<>();
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        sessionManager = new SessionManager(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ListView lvOrders = findViewById(R.id.lv_orders);
        
        adapter = new OrderRequestAdapter(this, pendingOrders);
        lvOrders.setAdapter(adapter);

        loadPendingOrders();
        checkStallSetup();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.admin_nav_orders) {
                return true;
            } else if (id == R.id.admin_nav_menu) {
                startActivity(new Intent(this, AdminMenuManageActivity.class));
                finish();
                return true;
            } else if (id == R.id.admin_nav_analytics) {
                startActivity(new Intent(this, AdminAnalyticsActivity.class));
                return true;
            } else if (id == R.id.admin_nav_account) {
                startActivity(new Intent(this, com.example.uorder.core.activities.AccountSettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    private void loadPendingOrders() {
        String stallId = mAuth.getUid();
        if (stallId == null) return;

        db.collection("orders")
                .whereEqualTo("stallId", stallId)
                .whereIn("orderStatus", Arrays.asList("Pending", "Accepted", "Rejected"))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED && !isFirstLoad) {
                                Order newOrder = dc.getDocument().toObject(Order.class);
                                if ("Pending".equalsIgnoreCase(newOrder.getOrderStatus())) {
                                    NotificationHelper.showNotification(this,
                                            "New Order Received",
                                            "You have a new order from " + newOrder.getCustomerName());
                                }
                            }
                        }

                        pendingOrders.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Order order = doc.toObject(Order.class);
                            order.setOrderId(doc.getId());
                            pendingOrders.add(order);
                        }
                        adapter.notifyDataSetChanged();
                        isFirstLoad = false;
                    }
                });
    }

    private void checkStallSetup() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("stalls").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists() || documentSnapshot.getString("stallName") == null || documentSnapshot.getString("stallName").isEmpty()) {
                showInitialSetupDialog();
            }
        });
    }

    private void showInitialSetupDialog() {
        android.widget.EditText etStallName = new android.widget.EditText(this);
        etStallName.setHint(R.string.enter_stall_name);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        etStallName.setPadding(padding, padding, padding, padding);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.setup_stall)
                .setView(etStallName)
                .setCancelable(false)
                .setPositiveButton(R.string.save, null) // Set to null to override later
                .show()
                .getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = etStallName.getText().toString().trim();
                    if (name.isEmpty()) {
                        etStallName.setError(getString(R.string.error_name_required));
                        return;
                    }
                    checkStallNameUniqueness(name, etStallName);
                });
    }

    private void checkStallNameUniqueness(String name, android.widget.EditText etStallName) {
        db.collection("stalls").whereEqualTo("stallName", name).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) {
                saveInitialStallName(name);
            } else {
                etStallName.setError(getString(R.string.error_stall_name_taken));
            }
        });
    }

    private void saveInitialStallName(String name) {
        String uid = mAuth.getUid();
        java.util.Map<String, Object> stall = new java.util.HashMap<>();
        stall.put("stallName", name);
        stall.put("ownerId", uid);
        stall.put("stallEmail", mAuth.getCurrentUser().getEmail());
        stall.put("stallPhone", "");
        stall.put("stallLogoUrl", "");
        stall.put("stallDescription", "");

        db.collection("stalls").document(uid).set(stall, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Update user record too
                    db.collection("users").document(uid).update("userName", name);
                    android.widget.Toast.makeText(this, "Stall Setup Complete", android.widget.Toast.LENGTH_SHORT).show();
                    // Close dialog happens automatically if we used a standard listener, 
                    // but here we manually handle it. Actually the dialog will dismiss if we get the AlertDialog object.
                    recreate(); // Simple way to refresh and dismiss
                });
    }

    private void showLogoutConfirmation() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_confirm_msg)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    mAuth.signOut();
                    sessionManager.logoutUser();
                    Intent logoutIntent = new Intent(this, SelectionActivity.class);
                    logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(logoutIntent);
                    finish();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
}
