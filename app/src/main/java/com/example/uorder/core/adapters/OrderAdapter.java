package com.example.uorder.core.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uorder.R;
import com.example.uorder.core.models.Order;
import com.example.uorder.core.utils.NotificationHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private final List<Order> orderList;
    private final java.util.Map<String, ListenerRegistration> listeners = new java.util.HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());

    public OrderAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_history, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        // Remove old listener if it exists for this holder
        String holderKey = String.valueOf(holder.hashCode());
        if (listeners.containsKey(holderKey)) {
            listeners.get(holderKey).remove();
        }

        // Attach a real-time listener to the order document
        ListenerRegistration registration = FirebaseFirestore.getInstance()
                .collection("orders")
                .document(order.getOrderId())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;
                    
                    String updatedStatus = snapshot.getString("orderStatus");
                    if (updatedStatus != null && !updatedStatus.equals(order.getOrderStatus())) {
                        order.setOrderStatus(updatedStatus);
                        // Update UI components for status
                        updateStatusUI(holder, updatedStatus);
                        // Handle cancel button visibility
                        holder.btnCancel.setVisibility("Pending".equalsIgnoreCase(updatedStatus) ? View.VISIBLE : View.GONE);
                    }
                });
        listeners.put(holderKey, registration);

        // Stall Name (Fetch if missing)
        if (order.getStallName() != null) {
            holder.tvStallName.setText(order.getStallName());
        } else if (order.getStallId() != null) {
            holder.tvStallName.setText("Loading...");
            FirebaseFirestore.getInstance().collection("stalls").document(order.getStallId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = documentSnapshot.getString("stallName");
                        if (name != null) {
                            order.setStallName(name);
                            notifyItemChanged(position);
                        }
                    });
        }

        // Initial Status Setup
        updateStatusUI(holder, order.getOrderStatus());
        holder.btnCancel.setVisibility("Pending".equalsIgnoreCase(order.getOrderStatus()) ? View.VISIBLE : View.GONE);

        holder.btnCancel.setOnClickListener(v -> {
            FirebaseFirestore.getInstance().collection("orders")
                    .document(order.getOrderId())
                    .update("orderStatus", "Cancelled")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(holder.itemView.getContext(), "Order cancelled", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(holder.itemView.getContext(), "Failed to cancel order", Toast.LENGTH_SHORT).show();
                    });
        });
        
        // Timestamp
        if (order.getTimestamp() != null) {
            holder.tvTimestamp.setText(dateFormat.format(order.getTimestamp().toDate()));
        }

        // Items List
        StringBuilder itemsBuilder = new StringBuilder();
        Map<String, Integer> items = order.getOrderItems();
        if (items != null) {
            for (Map.Entry<String, Integer> entry : items.entrySet()) {
                itemsBuilder.append("• ").append(entry.getValue()).append("x ").append(entry.getKey()).append("\n");
            }
            if (itemsBuilder.length() > 0) {
                itemsBuilder.setLength(itemsBuilder.length() - 1); // Remove last newline
            }
        }
        holder.tvItems.setText(itemsBuilder.toString());

        // Truncated Order ID
        String fullId = order.getOrderId();
        String truncatedId = (fullId != null && fullId.length() > 4) 
                ? fullId.substring(fullId.length() - 4) 
                : fullId;
        holder.tvOrderIdTruncated.setText(String.format("Order ID: #%s", truncatedId));

        // Total Price
        holder.tvTotalPrice.setText(String.format(Locale.getDefault(), "Total: ₱%.2f", order.getTotalAmount()));

        // Indicate if part of a split order
        if (order.getParentOrderId() != null) {
            holder.tvStallName.setText(holder.itemView.getContext().getString(R.string.split_order_label, order.getStallName()));
        }
    }

    private void updateStatusUI(OrderViewHolder holder, String status) {
        holder.tvStatus.setText(String.format("( %s )", status != null ? status.toUpperCase() : "PENDING"));
        
        int statusColor;
        if ("Accepted".equalsIgnoreCase(status)) {
            statusColor = holder.itemView.getContext().getColor(R.color.success);
        } else if ("Rejected".equalsIgnoreCase(status)) {
            statusColor = holder.itemView.getContext().getColor(R.color.error);
        } else if ("Completed".equalsIgnoreCase(status)) {
            statusColor = holder.itemView.getContext().getColor(R.color.success);
        } else if ("Cancelled".equalsIgnoreCase(status)) {
            statusColor = holder.itemView.getContext().getColor(R.color.error);
        } else {
            statusColor = holder.itemView.getContext().getColor(R.color.text_secondary);
        }
        holder.tvStatus.setTextColor(statusColor);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        for (ListenerRegistration lr : listeners.values()) {
            lr.remove();
        }
        listeners.clear();
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvStallName, tvStatus, tvTimestamp, tvItems, tvOrderIdTruncated, tvTotalPrice;
        Button btnCancel;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStallName = itemView.findViewById(R.id.tv_stall_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvItems = itemView.findViewById(R.id.tv_items);
            tvOrderIdTruncated = itemView.findViewById(R.id.tv_order_id_truncated);
            tvTotalPrice = itemView.findViewById(R.id.tv_total_price);
            btnCancel = itemView.findViewById(R.id.btn_cancel_order);
        }
    }
}
