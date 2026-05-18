package com.example.uorder.core.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.uorder.R;
import com.example.uorder.core.models.Order;

import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends ArrayAdapter<Order> {

    public OrderHistoryAdapter(@NonNull Context context, @NonNull List<Order> orders) {
        super(context, 0, orders);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_order_history, parent, false);
        }

        Order order = getItem(position);
        if (order != null) {
            TextView tvOrderId = convertView.findViewById(R.id.tv_order_id);
            TextView tvStatus = convertView.findViewById(R.id.tv_status);
            TextView tvCustomerName = convertView.findViewById(R.id.tv_customer_name);
            TextView tvTotalPrice = convertView.findViewById(R.id.tv_total_price);

            tvOrderId.setText(order.getOrderId());
            tvStatus.setText(order.getOrderStatus());
            // Since Order model no longer contains customerName (only customerId), we might need to display ID or fetch name.
            // For now, let's display the Order ID as a placeholder for the title if needed, 
            // but the schema uses orderItems to detail what was bought.
            tvCustomerName.setText("Order #" + order.getOrderId());
            tvTotalPrice.setText(String.format(Locale.getDefault(), "Total: ₱%.2f", order.getTotalAmount()));

            if ("Accepted".equalsIgnoreCase(order.getOrderStatus())) {
                tvStatus.setTextColor(Color.GREEN);
            } else if ("Rejected".equalsIgnoreCase(order.getOrderStatus())) {
                tvStatus.setTextColor(Color.RED);
            } else {
                tvStatus.setTextColor(Color.GRAY);
            }
        }

        return convertView;
    }
}
