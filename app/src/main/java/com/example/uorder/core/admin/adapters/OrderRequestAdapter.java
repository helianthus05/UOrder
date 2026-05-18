package com.example.uorder.core.admin.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.uorder.R;
import com.example.uorder.core.models.Order;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public class OrderRequestAdapter extends ArrayAdapter<Order> {

    public OrderRequestAdapter(@NonNull Context context, @NonNull List<Order> orders) {
        super(context, 0, orders);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_order_request, parent, false);
        }

        Order order = getItem(position);
        if (order != null) {
            TextView tvCustName = convertView.findViewById(R.id.tv_customer_name);
            TextView tvOrderId = convertView.findViewById(R.id.tv_order_id);
            TextView tvItems = convertView.findViewById(R.id.tv_order_items);
            TextView tvTotalDisplay = convertView.findViewById(R.id.tv_total_price_calc);
            Button btnAccept = convertView.findViewById(R.id.btn_accept_order);
            Button btnReject = convertView.findViewById(R.id.btn_reject_order);
            TextView tvStatusBadge = convertView.findViewById(R.id.tv_status_badge);
            LinearLayout layoutActions = convertView.findViewById(R.id.layout_actions);
            ImageButton btnClose = convertView.findViewById(R.id.btn_close_request);

            tvCustName.setText(order.getCustomerName() != null ? order.getCustomerName() : "Unknown Customer");
            
            // Format Order ID (Last 4 digits)
            String rawId = order.getOrderId();
            String displayId = (rawId != null && rawId.length() > 4) 
                ? "#" + rawId.substring(rawId.length() - 4).toUpperCase() 
                : "#" + rawId;
            tvOrderId.setText(displayId);

            // Format Items List
            StringBuilder itemsBuilder = new StringBuilder();
            if (order.getOrderItems() != null) {
                for (java.util.Map.Entry<String, Integer> entry : order.getOrderItems().entrySet()) {
                    itemsBuilder.append("• ").append(entry.getKey())
                            .append(" x").append(entry.getValue())
                            .append("\n");
                }
            }
            tvItems.setText(itemsBuilder.toString().trim());

            tvTotalDisplay.setText(getContext().getString(R.string.price_format, order.getTotalAmount()));

            // Update UI based on status
            String status = order.getOrderStatus();
            if ("Pending".equalsIgnoreCase(status)) {
                layoutActions.setVisibility(View.VISIBLE);
                tvStatusBadge.setVisibility(View.GONE);
            } else {
                layoutActions.setVisibility(View.GONE);
                tvStatusBadge.setVisibility(View.VISIBLE);
                tvStatusBadge.setText(status.toUpperCase());
                int color = "Accepted".equalsIgnoreCase(status) 
                    ? getContext().getColor(R.color.success) 
                    : getContext().getColor(R.color.error);
                tvStatusBadge.setTextColor(color);
            }

            btnAccept.setOnClickListener(v -> {
                FirebaseFirestore.getInstance().collection("orders")
                        .document(order.getOrderId())
                        .update("orderStatus", "Accepted")
                        .addOnSuccessListener(aVoid -> {
                            order.setOrderStatus("Accepted");
                            notifyDataSetChanged();
                            Toast.makeText(getContext(), getContext().getString(R.string.msg_order_accepted), Toast.LENGTH_SHORT).show();
                        });
            });

            btnReject.setOnClickListener(v -> {
                FirebaseFirestore.getInstance().collection("orders")
                        .document(order.getOrderId())
                        .update("orderStatus", "Rejected")
                        .addOnSuccessListener(aVoid -> {
                            order.setOrderStatus("Rejected");
                            notifyDataSetChanged();
                            Toast.makeText(getContext(), getContext().getString(R.string.msg_order_rejected), Toast.LENGTH_SHORT).show();
                        });
            });

            btnClose.setOnClickListener(v -> {
                FirebaseFirestore.getInstance().collection("orders")
                        .document(order.getOrderId())
                        .update("adminDismissed", true)
                        .addOnSuccessListener(aVoid -> {
                            remove(order);
                            notifyDataSetChanged();
                        });
            });
        }

        return convertView;
    }
}
