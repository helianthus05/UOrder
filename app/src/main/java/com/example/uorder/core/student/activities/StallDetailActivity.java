package com.example.uorder.core.student.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.uorder.R;
import com.example.uorder.core.models.Product;
import com.example.uorder.core.models.Stall;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StallDetailActivity extends AppCompatActivity {

    private String stallId;
    private FirebaseFirestore db;
    private List<Product> foodItems = new ArrayList<>();
    
    private ImageView ivBanner;
    private TextView tvPhone, tvEmail, tvFacebook;
    private RecyclerView rvItems;
    private CollapsingToolbarLayout collapsingToolbar;
    private View layoutFacebook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stall_detail);

        stallId = getIntent().getStringExtra("stallId");
        if (stallId == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        initViews();
        fetchStallDetails();
        fetchStallItems();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        ivBanner = findViewById(R.id.iv_stall_banner);
        tvPhone = findViewById(R.id.tv_stall_phone);
        tvEmail = findViewById(R.id.tv_stall_email);
        tvFacebook = findViewById(R.id.tv_stall_facebook);
        layoutFacebook = findViewById(R.id.layout_facebook);
        rvItems = findViewById(R.id.rv_stall_items);

        rvItems.setLayoutManager(new LinearLayoutManager(this));
    }

    private void fetchStallDetails() {
        db.collection("stalls").document(stallId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Stall stall = documentSnapshot.toObject(Stall.class);
                    if (stall != null) {
                        collapsingToolbar.setTitle(stall.getStallName());
                        tvPhone.setText(stall.getStallPhone() != null ? stall.getStallPhone() : "No phone provided");
                        tvEmail.setText(stall.getStallEmail() != null ? stall.getStallEmail() : "No email provided");

                        if (stall.getFacebookLink() != null && !stall.getFacebookLink().isEmpty()) {
                            layoutFacebook.setVisibility(View.VISIBLE);
                            layoutFacebook.setOnClickListener(v -> {
                                String url = stall.getFacebookLink();
                                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                    url = "https://" + url;
                                }
                                try {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    startActivity(intent);
                                } catch (Exception e) {
                                    Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            layoutFacebook.setVisibility(View.GONE);
                        }

                        if (stall.getStallLogoUrl() != null && !stall.getStallLogoUrl().isEmpty()) {
                            Glide.with(this)
                                    .load(stall.getStallLogoUrl())
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(ivBanner);
                        }

                        findViewById(R.id.layout_phone).setOnClickListener(v -> {
                            if (stall.getStallPhone() != null) {
                                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + stall.getStallPhone()));
                                startActivity(intent);
                            }
                        });
                    }
                });
    }

    private void fetchStallItems() {
        db.collection("products")
                .whereEqualTo("stallId", stallId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        foodItems.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product item = document.toObject(Product.class);
                            if (item.isAvailable()) {
                                foodItems.add(item);
                            }
                        }
                        rvItems.setAdapter(new SimpleFoodAdapter(foodItems));
                    }
                });
    }

    private class SimpleFoodAdapter extends RecyclerView.Adapter<SimpleFoodAdapter.VH> {
        private List<Product> items;
        SimpleFoodAdapter(List<Product> items) { this.items = items; }

        class VH extends RecyclerView.ViewHolder {
            TextView name, price, desc;
            ImageView img;
            VH(View v) {
                super(v);
                name = v.findViewById(R.id.tv_food_name);
                price = v.findViewById(R.id.tv_food_price);
                desc = v.findViewById(R.id.tv_description);
                img = v.findViewById(R.id.iv_food_image);
                // Hide cart buttons for detail view if preferred, or keep them
                v.findViewById(R.id.layout_quantity_control).setVisibility(View.GONE);
                v.findViewById(R.id.btn_add_toggle).setVisibility(View.GONE);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Product item = items.get(position);
            h.name.setText(item.getProductName());
            h.price.setText(getString(R.string.price_format, item.getProductPrice()));
            h.desc.setText(item.getProductDescription());
            
            if (item.getProductImageUrl() != null && !item.getProductImageUrl().isEmpty()) {
                Glide.with(h.img.getContext())
                        .load(item.getProductImageUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(h.img);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }
    }
}
