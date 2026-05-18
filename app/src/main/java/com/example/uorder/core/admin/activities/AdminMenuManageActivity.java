package com.example.uorder.core.admin.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uorder.R;
import com.example.uorder.core.admin.adapters.MenuItemAdapter;
import com.example.uorder.core.auth.SessionManager;
import com.example.uorder.core.auth.SelectionActivity;
import com.example.uorder.core.models.Product;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.InputStream;
import java.util.Map;
import java.util.ArrayList;

public class AdminMenuManageActivity extends AppCompatActivity {

    ListView lvMenuItems;
    Button btnCancel, btnDone;
    android.widget.ProgressBar progressBar;
    SessionManager sessionManager;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    String stallId;

    // Local working copy — changes only commit when DONE is tapped
    ArrayList<Product> menuItemList;
    MenuItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_menu_manage);

        sessionManager = new SessionManager(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        stallId = mAuth.getUid();

        lvMenuItems = findViewById(R.id.lv_menu_items);
        btnCancel   = findViewById(R.id.btn_cancel);
        btnDone     = findViewById(R.id.btn_done);
        progressBar = findViewById(R.id.progress_bar);

        menuItemList = new ArrayList<>();
        adapter = new MenuItemAdapter(this, menuItemList);
        lvMenuItems.setAdapter(adapter);

        loadMenuFromFirebase();

        // FAB: Add new item
        findViewById(R.id.fab_add_item).setOnClickListener(v -> {
            Intent intent = new Intent(this, ItemManageFoodActivity.class);
            intent.putExtra("position", -1); // -1 indicates a new item
            startActivityForResult(intent, 1);
        });

        // REMOVE: show confirmation dialog before deleting (per spec)
        adapter.setOnRemoveClickListener(position -> {
            Product item = menuItemList.get(position);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_remove_title)
                    .setMessage(getString(R.string.dialog_remove_msg, item.getProductName()))
                    .setPositiveButton(R.string.remove, (dialog, which) -> {
                        Product removedItem = menuItemList.remove(position);
                        adapter.notifyDataSetChanged();
                        db.collection("products").document(String.valueOf(removedItem.getId())).delete();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        // Tap a row to open the edit form for that item
        lvMenuItems.setOnItemClickListener((parent, view, position, id) ->
                openEditForm(menuItemList.get(position), position));

        // DONE: save local changes back to Firestore
        btnDone.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.msg_menu_saved), Toast.LENGTH_SHORT).show();
            finish();
        });

        // CANCEL: reload from Firebase
        btnCancel.setOnClickListener(v -> loadMenuFromFirebase());

        // Bottom nav
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.admin_nav_menu);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.admin_nav_orders) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.admin_nav_menu) {
                return true; // already here
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

    private void loadMenuFromFirebase() {
        if (stallId == null) return;
        if (progressBar != null) progressBar.setVisibility(android.view.View.VISIBLE);
        db.collection("products")
                .whereEqualTo("stallId", stallId)
                .get()
                .addOnCompleteListener(task -> {
                    if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
                    if (task.isSuccessful()) {
                        menuItemList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product item = document.toObject(Product.class);
                            item.setId(document.getId());
                            menuItemList.add(item);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Error loading menu", Toast.LENGTH_SHORT).show();
                    }
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

    // Open ItemManageFoodActivity to edit or add an item
    private void openEditForm(Product item, int position) {
        Intent intent = new Intent(this, ItemManageFoodActivity.class);
        intent.putExtra("item_name",  item.getProductName());
        intent.putExtra("item_price", item.getProductPrice());
        intent.putExtra("item_desc",  item.getProductDescription());
        intent.putExtra("item_type",  item.getProductType());
        intent.putExtra("item_available", item.isAvailable());
        intent.putExtra("productImageUrl", item.getProductImageUrl());
        intent.putExtra("position",   position);
        startActivityForResult(intent, 1);
    }

    // Receive the saved item back from ItemManageFoodActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            int position = data.getIntExtra("position", -1);
            String action = data.getStringExtra("action");

            // Handle remove action from the edit screen
            if ("remove".equals(action)) {
                if (position != -1 && position < menuItemList.size()) {
                    Product removed = menuItemList.remove(position);
                    adapter.notifyDataSetChanged();
                    if (removed.getId() != null) {
                        db.collection("products").document(removed.getId()).delete();
                    }
                }
                return;
            }

            String name     = data.getStringExtra("item_name");
            double price    = data.getDoubleExtra("item_price", 0.0);
            String desc     = data.getStringExtra("item_desc");
            String type     = data.getStringExtra("item_type");
            boolean available = data.getBooleanExtra("item_available", true);
            String imgUri   = data.getStringExtra("productImageUrl");

            Product updated = new Product(name, price, desc, type, stallId, available, imgUri);

            if (position == -1) {
                // For new items, if they have a local file URI (from internal storage), we should upload it
                if (imgUri != null && (imgUri.startsWith("file:") || imgUri.startsWith("/"))) {
                    uploadItemImageAndSave(updated, android.net.Uri.parse(imgUri), null);
                } else {
                    saveItemToFirestore(updated);
                }
            } else {
                Product original = menuItemList.get(position);
                updated.setId(original.getId());
                // If image changed to a new local file, upload it
                if (imgUri != null && (imgUri.startsWith("file:") || imgUri.startsWith("/")) && !imgUri.equals(original.getProductImageUrl())) {
                    uploadItemImageAndSave(updated, android.net.Uri.parse(imgUri), original.getProductImageUrl());
                } else {
                    saveItemToFirestore(updated);
                }
            }
        }
    }

    private void uploadItemImageAndSave(Product item, android.net.Uri localUri, String fallbackUrl) {
        if (stallId == null || stallId.isEmpty()) {
            stallId = FirebaseAuth.getInstance().getUid();
        }

        if (stallId == null) {
            Toast.makeText(this, "Upload failed: Session expired", Toast.LENGTH_SHORT).show();
            if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
            return;
        }

        if (progressBar != null) progressBar.setVisibility(android.view.View.VISIBLE);

        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dk5xemzlx",
                "secure", true
        ));

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(localUri);
                Map uploadResult = cloudinary.uploader().unsignedUpload(inputStream, "UORDER", ObjectUtils.emptyMap());

                String imageUrl = (String) uploadResult.get("secure_url");

                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
                    item.setProductImageUrl(imageUrl);
                    saveItemToFirestore(item);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Fallback to original/no image to prevent saving local 'file://' URIs to Firestore
                    item.setProductImageUrl(fallbackUrl);
                    saveItemToFirestore(item);
                });
            }
        }).start();
    }

    private void saveItemToFirestore(Product item) {
        if (item.getId() == null || item.getId().isEmpty()) {
            String newId = db.collection("products").document().getId();
            item.setId(newId);
        }

        if (progressBar != null) progressBar.setVisibility(android.view.View.VISIBLE);
        db.collection("products").document(item.getId()).set(item)
                .addOnSuccessListener(aVoid -> {
                    // Update local list if needed (it might already be updated or need refresh)
                    loadMenuFromFirebase();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Failed to save item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
