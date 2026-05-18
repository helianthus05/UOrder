package com.example.uorder.core.admin.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.uorder.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.Locale;

public class ItemManageFoodActivity extends AppCompatActivity {

    EditText etEditName, etEditPrice, etEditDescription;
    AutoCompleteTextView spinnerEditType;
    MaterialSwitch switchAvailable;
    ImageView ivEditImage;
    Button btnUploadImg, btnRemoveItem, btnCancel, btnDone;
    MaterialToolbar toolbar;

    int position;
    Uri selectedImageUri = null;
    private final String[] PRODUCT_TYPES = {"Meal", "Snack", "Beverage", "Dessert", "Side Dish"};

    // Gallery image picker
    ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this)
                            .load(uri)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(ivEditImage);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_manage_food);

        etEditName        = findViewById(R.id.et_edit_name);
        etEditPrice       = findViewById(R.id.et_edit_price);
        etEditDescription = findViewById(R.id.et_edit_description);
        spinnerEditType   = findViewById(R.id.spinner_edit_type);
        switchAvailable   = findViewById(R.id.switch_available);
        ivEditImage       = findViewById(R.id.iv_edit_image);
        btnUploadImg      = findViewById(R.id.btn_upload_img);
        btnRemoveItem     = findViewById(R.id.btn_remove_item);
        btnCancel         = findViewById(R.id.btn_cancel);
        btnDone           = findViewById(R.id.btn_done);
        toolbar           = findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, PRODUCT_TYPES);
        spinnerEditType.setAdapter(typeAdapter);

        position = getIntent().getIntExtra("position", -1);

        if (position == -1) {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.add_new_item);
            btnDone.setText(R.string.add_item);
            btnRemoveItem.setVisibility(View.GONE);
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.edit_item);
            btnDone.setText(R.string.update_item);
            btnRemoveItem.setVisibility(View.VISIBLE);
        }

        // Ensure fields are fully unlocked and interactive
        etEditName.setText(getIntent().getStringExtra("item_name"));
        etEditDescription.setText(getIntent().getStringExtra("item_desc"));
        
        String currentType = getIntent().getStringExtra("item_type");
        if (currentType != null) {
            spinnerEditType.setText(currentType, false);
        } else {
            spinnerEditType.setText(PRODUCT_TYPES[0], false);
        }

        boolean isAvailable = getIntent().getBooleanExtra("item_available", true);
        switchAvailable.setChecked(isAvailable);

        double price = getIntent().getDoubleExtra("item_price", 0.0);
        if (price > 0) {
            etEditPrice.setText(String.format(Locale.getDefault(), "%.2f", price));
        }

        String imgUriStr = getIntent().getStringExtra("productImageUrl");
        if (imgUriStr != null) {
            selectedImageUri = Uri.parse(imgUriStr);
            Glide.with(this)
                    .load(selectedImageUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(ivEditImage);
        } else {
            int resId = getIntent().getIntExtra("item_image_res", 0);
            if (resId != 0) {
                Glide.with(this)
                        .load(resId)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(ivEditImage);
            }
        }

        if (btnUploadImg != null) {
            btnUploadImg.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        }

        if (btnRemoveItem != null) {
            btnRemoveItem.setOnClickListener(v -> {
                Intent result = new Intent();
                result.putExtra("action", "remove");
                result.putExtra("position", position);
                setResult(RESULT_OK, result);
                finish();
            });
        }

        if (btnDone != null) {
            btnDone.setOnClickListener(v -> saveAndReturn());
        }
        
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item_manage, menu);
        // Change menu item title based on action
        MenuItem saveItem = menu.findItem(R.id.action_save);
        if (saveItem != null) {
            saveItem.setTitle(position == -1 ? R.string.add : R.string.save);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveAndReturn();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveAndReturn() {
        String name     = etEditName.getText().toString().trim();
        String priceStr = etEditPrice.getText().toString().trim();
        String desc     = etEditDescription.getText().toString().trim();
        String type     = spinnerEditType.getText().toString().trim();
        boolean available = switchAvailable.isChecked();

        if (name.isEmpty()) {
            etEditName.setError("Name required");
            return;
        }
        if (priceStr.isEmpty()) {
            etEditPrice.setError("Price required");
            return;
        }
        if (type.isEmpty()) {
            spinnerEditType.setError("Type required");
            return;
        }

        double parsedPrice;
        try {
            parsedPrice = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            etEditPrice.setError("Invalid price");
            return;
        }

        String finalImageUri = null;
        
        if (selectedImageUri != null) {
            if (selectedImageUri.toString().startsWith("content://")) {
                finalImageUri = saveImageToInternalStorage(selectedImageUri);
            } else {
                finalImageUri = selectedImageUri.toString();
            }
        } else {
            // If no new URI, check if we had an original one
            finalImageUri = getIntent().getStringExtra("productImageUrl");
        }

        Intent result = new Intent();
        result.putExtra("item_name",  name);
        result.putExtra("item_price", parsedPrice);
        result.putExtra("item_desc",  desc);
        result.putExtra("item_type",  type);
        result.putExtra("item_available", available);
        result.putExtra("productImageUrl", finalImageUri);
        result.putExtra("position",   position);
        setResult(RESULT_OK, result);
        finish();
    }

    private String saveImageToInternalStorage(Uri uri) {
        try {
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            java.io.File file = new java.io.File(getFilesDir(), "item_" + System.currentTimeMillis() + ".jpg");
            java.io.OutputStream os = new java.io.FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.close();
            is.close();
            return Uri.fromFile(file).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
