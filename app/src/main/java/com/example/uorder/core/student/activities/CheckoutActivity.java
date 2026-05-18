package com.example.uorder.core.student.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.uorder.R;
import com.example.uorder.core.auth.SessionManager;
import com.example.uorder.core.models.Product;
import com.example.uorder.core.models.Order;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private RecyclerView rv;
    private TextView tvTotal;
    private List<Product> cartItems = new ArrayList<>();
    private CartAdapter adapter;

    private View viewCheckout;
    private View viewOrderDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        viewCheckout  = findViewById(R.id.layout_checkout);
        viewOrderDone = findViewById(R.id.layout_order_done);

        rv      = findViewById(R.id.rv_cart_list);
        tvTotal = findViewById(R.id.tv_total_price);

        ArrayList<Product> received =
                getIntent().getParcelableArrayListExtra("selectedFoods");
        if (received != null) {
            cartItems.clear();
            cartItems.addAll(received);
            Order.setCart(cartItems);
        } else {
            cartItems.addAll(Order.getCart());
        }

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter();
        rv.setAdapter(adapter);
        updateTotal();

        // Checkout screen
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.btn_clear).setOnClickListener(v -> {
            cartItems.clear();
            Order.clearCart();
            adapter.notifyDataSetChanged();
            updateTotal();
        });

        findViewById(R.id.btn_make_order).setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_cart_empty), Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = FirebaseAuth.getInstance().getUid();
            String userName = new SessionManager(this).getUsername();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 1. Group items by stallId to split the order if necessary
            java.util.Map<String, java.util.List<Product>> groupedItems = new java.util.HashMap<>();
            for (Product p : cartItems) {
                String sid = p.getStallId();
                if (sid == null) sid = "unknown_stall";
                if (!groupedItems.containsKey(sid)) {
                    groupedItems.put(sid, new java.util.ArrayList<>());
                }
                groupedItems.get(sid).add(p);
            }

            final int totalStalls = groupedItems.size();
            final java.util.concurrent.atomic.AtomicInteger processedStalls = new java.util.concurrent.atomic.AtomicInteger(0);
            final java.util.concurrent.atomic.AtomicBoolean hasError = new java.util.concurrent.atomic.AtomicBoolean(false);

            // Generate a common parent order ID if there are multiple stalls
            final String parentOrderId = (totalStalls > 1) ? db.collection("orders").document().getId() : null;

            for (java.util.Map.Entry<String, java.util.List<Product>> entry : groupedItems.entrySet()) {
                String stallId = entry.getKey();
                java.util.List<Product> stallProducts = entry.getValue();

                db.collection("stalls").document(stallId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            String stallName = documentSnapshot.getString("stallName");
                            if (stallName == null) stallName = "Unknown Stall";

                            java.util.Map<String, Integer> itemsMap = new java.util.HashMap<>();
                            double stallTotal = 0;
                            for (Product p : stallProducts) {
                                itemsMap.put(p.getProductName(), p.getQuantity());
                                stallTotal += p.getProductPrice() * p.getQuantity();
                            }

                            Order finalOrder = new Order(userId, userName, stallId, itemsMap, stallTotal);
                            finalOrder.setStallName(stallName);
                            finalOrder.setParentOrderId(parentOrderId);

                            String orderId = db.collection("orders").document().getId();
                            finalOrder.setOrderId(orderId);

                            db.collection("orders").document(orderId).set(finalOrder)
                                    .addOnSuccessListener(aVoid -> {
                                        if (processedStalls.incrementAndGet() == totalStalls) {
                                            if (!hasError.get()) {
                                                completeOrderUI();
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        hasError.set(true);
                                        Toast.makeText(this, "Failed to place order for " + finalOrder.getStallName(), Toast.LENGTH_SHORT).show();
                                        if (processedStalls.incrementAndGet() == totalStalls) {
                                            // Final attempt even if some failed
                                        }
                                    });
                        })
                        .addOnFailureListener(e -> {
                            hasError.set(true);
                            Toast.makeText(this, "Error fetching stall info", Toast.LENGTH_SHORT).show();
                            if (processedStalls.incrementAndGet() == totalStalls) {
                                // Final attempt even if some failed
                            }
                        });
            }
        });

        findViewById(R.id.btn_return_home).setOnClickListener(v -> {
            finish();
        });

        // Setup bottom nav
        View bottomNavContainer = findViewById(R.id.bottom_navigation);
        if (bottomNavContainer != null) {
            BottomNavigationView nav = (BottomNavigationView) bottomNavContainer;
            nav.setSelectedItemId(R.id.nav_menu);
            nav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_menu) {
                    finish();
                    return true;
                } else {
                    Intent intent = new Intent(this, UserDashboardActivity.class);
                    intent.putExtra("target_fragment", id);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                }
            });
        }
    }

    private void completeOrderUI() {
        // Clear the cart state and the selection/quantity in the model
        Order.clearCart();
        cartItems.clear();
        adapter.notifyDataSetChanged();

        // Swap to "Order Done" view and hide the summary card
        viewCheckout.setVisibility(View.GONE);
        View summaryCard = findViewById(R.id.summary_card);
        if (summaryCard != null) {
            summaryCard.setVisibility(View.GONE);
        }
        viewOrderDone.setVisibility(View.VISIBLE);
    }

    private double computeTotal() {
        double sum = 0;
        for (Product item : cartItems) {
            sum += item.getProductPrice() * item.getQuantity();
        }
        return sum;
    }

    private void updateTotal() {
        tvTotal.setText(getString(R.string.price_format, computeTotal()));
    }

    private class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            ImageView img;
            TextView name, price, desc;
            ImageButton removeBtn;
            Button minus, plus;
            EditText qtyInput;

            VH(View v) {
                super(v);
                img       = v.findViewById(R.id.iv_cart_image);
                name      = v.findViewById(R.id.tv_cart_name);
                price     = v.findViewById(R.id.tv_cart_price);
                desc      = v.findViewById(R.id.tv_cart_description);
                removeBtn = v.findViewById(R.id.ib_remove_item);
                minus     = v.findViewById(R.id.btn_minus);
                plus      = v.findViewById(R.id.btn_plus);
                qtyInput  = v.findViewById(R.id.et_quantity_input);
            }
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_cart_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int i) {
            Product item = cartItems.get(i);

            if (item.getProductImageUrl() != null) {
                Glide.with(h.img.getContext())
                        .load(item.getProductImageUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(h.img);
            } else {
                Glide.with(h.img.getContext())
                        .load(R.drawable.ic_launcher_background)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(h.img);
            }
            h.name.setText(item.getProductName());
            h.price.setText(getString(R.string.price_format, item.getProductPrice()));
            h.desc.setText(item.getProductDescription());
            h.qtyInput.setText(String.valueOf(item.getQuantity()));

            // Remove
            h.removeBtn.setOnClickListener(v -> {
                int pos = h.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                cartItems.remove(pos);
                Order.setCart(cartItems);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, cartItems.size());
                updateTotal();
            });

            // Plus
            h.plus.setOnClickListener(v -> {
                int qty = parseQty(h.qtyInput.getText().toString()) + 1;
                item.setQuantity(qty);
                h.qtyInput.setText(String.valueOf(qty));
                updateTotal();
            });

            // Minus
            h.minus.setOnClickListener(v -> {
                int qty = parseQty(h.qtyInput.getText().toString());
                if (qty > 1) {
                    item.setQuantity(--qty);
                    h.qtyInput.setText(String.valueOf(qty));
                    updateTotal();
                }
            });
        }

        @Override
        public int getItemCount() { return cartItems.size(); }
    }

    private int parseQty(String s) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return 1; }
    }
}
