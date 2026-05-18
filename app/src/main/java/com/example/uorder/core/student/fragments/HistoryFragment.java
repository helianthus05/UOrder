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
import java.util.Arrays;
import java.util.List;

public class HistoryFragment extends Fragment {

    private OrderAdapter adapter;
    private final List<Order> historyOrders = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history, container, false);
        RecyclerView rv = v.findViewById(R.id.rv_history);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new OrderAdapter(historyOrders);
        rv.setAdapter(adapter);

        loadHistory();

        return v;
    }

    private void loadHistory() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("customerId", userId)
                .whereIn("orderStatus", Arrays.asList("Accepted", "Rejected", "Completed", "Cancelled"))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    historyOrders.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        historyOrders.add(doc.toObject(Order.class));
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
