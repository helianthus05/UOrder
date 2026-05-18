package com.example.uorder.core.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.uorder.R;
import com.example.uorder.core.auth.SelectionActivity;
import com.example.uorder.core.auth.SessionManager;
import com.example.uorder.core.models.Stall;
import com.example.uorder.core.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AccountSettingsActivity extends AppCompatActivity {

    private EditText etName, etStallDescription, etStallPhone, etStallEmail, etStallFacebook;
    private ImageView ivStallLogo, ivStallBg;
    private View layoutBusinessFields, progressBar;
    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userRole;
    private String userId;
    private Uri selectedImageUri, selectedBgUri;
    private static final int RC_IMAGE_PICK = 9003;
    private static final int RC_BG_PICK = 9004;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        sessionManager = new SessionManager(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getUid();
        userRole = sessionManager.getRole();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etName = findViewById(R.id.et_name);
        etStallDescription = findViewById(R.id.et_stall_description);
        etStallPhone = findViewById(R.id.et_stall_phone);
        etStallEmail = findViewById(R.id.et_stall_email);
        etStallFacebook = findViewById(R.id.et_stall_facebook);
        ivStallLogo = findViewById(R.id.iv_stall_logo);
        ivStallBg = findViewById(R.id.iv_stall_bg);
        layoutBusinessFields = findViewById(R.id.layout_business_fields);
        progressBar = findViewById(R.id.progress_bar);

        if ("business".equals(userRole)) {
            layoutBusinessFields.setVisibility(View.VISIBLE);
            findViewById(R.id.btn_change_logo).setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, RC_IMAGE_PICK);
            });
            findViewById(R.id.btn_change_bg).setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, RC_BG_PICK);
            });
            loadStallData();
        } else {
            loadUserData();
        }

        findViewById(R.id.btn_save).setOnClickListener(v -> saveChanges());
        findViewById(R.id.btn_change_password).setOnClickListener(v -> {
            startActivity(new Intent(this, ChangePasswordActivity.class));
        });
        findViewById(R.id.btn_logout).setOnClickListener(v -> showLogoutConfirmation());
    }

    private void loadUserData() {
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            User user = documentSnapshot.toObject(User.class);
            if (user != null) {
                etName.setText(user.getUserName());
            }
        });
    }

    private void loadStallData() {
        db.collection("stalls").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            Stall stall = documentSnapshot.toObject(Stall.class);
            if (stall != null) {
                etName.setText(stall.getStallName()); // Stall Name
                etStallDescription.setText(stall.getStallDescription());
                etStallPhone.setText(stall.getStallPhone());
                etStallEmail.setText(stall.getStallEmail());
                etStallFacebook.setText(stall.getFacebookLink());
                if (stall.getStallLogoUrl() != null && !stall.getStallLogoUrl().isEmpty()) {
                    Glide.with(this)
                            .load(stall.getStallLogoUrl())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(ivStallLogo);
                }
                if (stall.getStallBg() != null && !stall.getStallBg().isEmpty()) {
                    Glide.with(this)
                            .load(stall.getStallBg())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(ivStallBg);
                }
            }
        });
    }

    private void saveChanges() {
        String newName = etName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("business".equals(userRole)) {
            checkStallNameUniquenessAndSave(newName);
        } else {
            updateUserName(newName);
        }
    }

    private void updateUserName(String newName) {
        db.collection("users").document(userId)
                .update("userName", newName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void checkStallNameUniquenessAndSave(String newName) {
        db.collection("stalls")
                .whereEqualTo("stallName", newName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isUnique = true;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (!doc.getId().equals(userId)) {
                            isUnique = false;
                            break;
                        }
                    }

                    if (isUnique) {
                        startUploadSequence(newName);
                    } else {
                        Toast.makeText(this, R.string.error_stall_name_taken, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startUploadSequence(String stallName) {
        if (selectedImageUri != null) {
            uploadLogo(stallName);
        } else if (selectedBgUri != null) {
            uploadBackground(stallName, null);
        } else {
            saveStallData(stallName, null, null);
        }
    }

    private void uploadLogo(String stallName) {
        progressBar.setVisibility(View.VISIBLE);
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dk5xemzlx",
                "secure", true
        ));

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                Map uploadResult = cloudinary.uploader().unsignedUpload(inputStream, "UORDER", ObjectUtils.emptyMap());
                String imageUrl = (String) uploadResult.get("secure_url");

                runOnUiThread(() -> {
                    if (selectedBgUri != null) {
                        uploadBackground(stallName, imageUrl);
                    } else {
                        saveStallData(stallName, imageUrl, null);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Logo upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void uploadBackground(String stallName, @Nullable String logoUrl) {
        progressBar.setVisibility(View.VISIBLE);
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dk5xemzlx",
                "secure", true
        ));

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedBgUri);
                Map uploadResult = cloudinary.uploader().unsignedUpload(inputStream, "UORDER", ObjectUtils.emptyMap());
                String bgUrl = (String) uploadResult.get("secure_url");

                runOnUiThread(() -> {
                    saveStallData(stallName, logoUrl, bgUrl);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Background upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveStallData(String stallName, @Nullable String logoUrl, @Nullable String bgUrl) {
        progressBar.setVisibility(View.VISIBLE);
        Map<String, Object> updates = new HashMap<>();
        updates.put("stallName", stallName);
        updates.put("stallDescription", etStallDescription.getText().toString().trim());
        updates.put("stallPhone", etStallPhone.getText().toString().trim());
        updates.put("stallEmail", etStallEmail.getText().toString().trim());
        updates.put("facebookLink", etStallFacebook.getText().toString().trim());
        if (logoUrl != null) {
            updates.put("stallLogoUrl", logoUrl);
        }
        if (bgUrl != null) {
            updates.put("stallBg", bgUrl);
        }

        db.collection("stalls").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Also update users collection name
                    db.collection("users").document(userId).update("userName", stallName);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.stall_updated, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == RC_IMAGE_PICK) {
                selectedImageUri = data.getData();
                ivStallLogo.setImageURI(selectedImageUri);
            } else if (requestCode == RC_BG_PICK) {
                selectedBgUri = data.getData();
                ivStallBg.setImageURI(selectedBgUri);
            }
        }
    }
}
