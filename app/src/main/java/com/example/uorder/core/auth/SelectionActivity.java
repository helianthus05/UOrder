package com.example.uorder.core.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.uorder.R;
import com.example.uorder.core.admin.activities.AdminDashboardActivity;
import com.example.uorder.core.student.activities.UserDashboardActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;
import android.widget.Toast;

public class SelectionActivity extends AppCompatActivity {
    private static final String TAG = "SelectionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        SessionManager sessionManager = new SessionManager(this);

        if (mAuth.getCurrentUser() != null) {
            if (sessionManager.isLoggedIn() && sessionManager.getRole() != null) {
                Log.d(TAG, "Auto-login: Session found for role " + sessionManager.getRole());
                redirectToDashboard(sessionManager.getRole());
                return;
            } else {
                Log.d(TAG, "Auto-login: Firebase user found but session incomplete. Attempting recovery.");
                recoverSessionAndRedirect(mAuth.getCurrentUser().getUid(), mAuth.getCurrentUser().getEmail(), sessionManager);
                return;
            }
        }

        setupSelectionUI();
    }

    private void setupSelectionUI() {
        setContentView(R.layout.activity_selection);

        MaterialButton btnCustomer = findViewById(R.id.btn_customer_selection);
        MaterialButton btnBusiness = findViewById(R.id.btn_business_selection);

        btnCustomer.setOnClickListener(v -> {
            Intent intent = new Intent(SelectionActivity.this, CustomerLoginActivity.class);
            startActivity(intent);
        });

        btnBusiness.setOnClickListener(v -> {
            Intent intent = new Intent(SelectionActivity.this, BusinessLoginActivity.class);
            startActivity(intent);
        });
    }

    private void recoverSessionAndRedirect(String userId, String email, SessionManager sessionManager) {
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("userRole");
                        String name = documentSnapshot.getString("userName");
                        
                        Log.d(TAG, "Recovery: User doc found. Role: " + role);
                        sessionManager.createLoginSession(name != null ? name : email, role);
                        redirectToDashboard(role);
                    } else {
                        Log.d(TAG, "Recovery: User doc not found in Firestore.");
                        setupSelectionUI();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Recovery: Firestore fetch failed", e);
                    Toast.makeText(this, "Session recovery failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setupSelectionUI();
                });
    }

    private void redirectToDashboard(String role) {
        Intent intent;
        if ("customer".equals(role) || "student".equals(role)) {
            intent = new Intent(this, UserDashboardActivity.class);
        } else if ("business".equals(role) || "admin".equals(role)) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else {
            Log.w(TAG, "Unknown role: " + role);
            Toast.makeText(this, "Error: Unknown user role '" + role + "'", Toast.LENGTH_SHORT).show();
            return;
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
