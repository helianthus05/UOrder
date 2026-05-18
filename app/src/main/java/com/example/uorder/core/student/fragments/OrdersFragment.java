package com.example.uorder.core.student.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uorder.R;
import com.example.uorder.core.adapters.OrderAdapter;
import com.example.uorder.core.models.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment {

    private OrderAdapter adapter;
    private final List<Order> pendingOrders = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_orders, container, false);
        RecyclerView rv = v.findViewById(R.id.rv_orders);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new OrderAdapter(pendingOrders);
        rv.setAdapter(adapter);

        loadOrders();

        return v;
    }

    private void loadOrders() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("customerId", userId)
                .whereEqualTo("orderStatus", "Pending")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    pendingOrders.clear();
                    java.util.Map<String, List<Order>> groupedByParent = new java.util.HashMap<>();
                    List<Order> singleOrders = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : value) {
                        Order order = doc.toObject(Order.class);
                        if (order.getParentOrderId() != null) {
                            if (!groupedByParent.containsKey(order.getParentOrderId())) {
                                groupedByParent.put(order.getParentOrderId(), new ArrayList<>());
                            }
                            groupedByParent.get(order.getParentOrderId()).add(order);
                        } else {
                            singleOrders.add(order);
                        }
                    }

                    // Process grouped orders: merge items if from multiple stalls
                    for (List<Order> group : groupedByParent.values()) {
                        if (group.size() > 1) {
                            // Split order logic: represent as multiple entries or a combined one?
                            // "putting the orders under the stall it belonged to" 
                            // suggests keeping them distinct but perhaps indicating they are part of a split.
                            // But "Do not split the order info if placed from one stall"
                            // implies that for multi-stall, they SHOULD be visible as separate stall components.
                            pendingOrders.addAll(group);
                        } else {
                            pendingOrders.addAll(group);
                        }
                    }
                    pendingOrders.addAll(singleOrders);
                    
                    // Sort by timestamp after merging
                    pendingOrders.sort((a, b) -> {
                        if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
                        return b.getTimestamp().compareTo(a.getTimestamp());
                    });

                    adapter.notifyDataSetChanged();
                });
    }
}
