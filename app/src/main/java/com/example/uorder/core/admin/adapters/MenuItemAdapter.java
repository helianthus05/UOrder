package com.example.uorder.core.admin.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.uorder.R;
import com.example.uorder.core.models.Product;

import java.util.ArrayList;
import java.util.Locale;

public class MenuItemAdapter extends ArrayAdapter<Product> {

    private final Context context;
    private final ArrayList<Product> items;
    private OnRemoveClickListener removeListener;

    public interface OnRemoveClickListener {
        void onRemoveClick(int position);
    }

    public void setOnRemoveClickListener(OnRemoveClickListener listener) {
        this.removeListener = listener;
    }

    public MenuItemAdapter(Context context, ArrayList<Product> items) {
        super(context, 0, items);
        this.context = context;
        this.items   = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_admin_menu_list_item, parent, false);
        }

        Product item = items.get(position);

        TextView tvName  = convertView.findViewById(R.id.tv_item_name);
        TextView tvPrice = convertView.findViewById(R.id.tv_item_price);
        TextView tvDesc  = convertView.findViewById(R.id.tv_item_desc);
        TextView tvStatus = convertView.findViewById(R.id.tv_item_status);
        ImageView ivImg  = convertView.findViewById(R.id.iv_item_image);
        Button btnRemove = convertView.findViewById(R.id.btn_remove_item);

        // Set item data
        tvName.setText(item.getProductName());
        tvPrice.setText(context.getString(R.string.price_format, item.getProductPrice()));
        tvDesc.setText(item.getProductDescription());

        if (tvStatus != null) {
            if (!item.isAvailable()) {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText(context.getString(R.string.hidden));
                convertView.setAlpha(0.6f);
            } else {
                tvStatus.setVisibility(View.GONE);
                convertView.setAlpha(1.0f);
            }
        }

        // Load image
        if (ivImg != null) {
            String uri = item.getProductImageUrl();
            if (uri != null && !uri.isEmpty()) {
                Glide.with(context)
                        .load(uri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(ivImg);
            } else {
                Glide.with(context)
                        .load(R.drawable.ic_launcher_background)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(ivImg);
            }
        }

        // Handle the row click to open Edit Screen
        convertView.setOnClickListener(v -> {
            if (parent instanceof ListView) {
                ((ListView) parent).performItemClick(v, position, getItemId(position));
            }
        });

        // Handle the remove button click
        btnRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemoveClick(position);
            }
        });

        return convertView;
    }
}
