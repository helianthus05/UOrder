package com.example.uorder.core.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uorder.R;
import com.example.uorder.core.admin.activities.AdminDashboardActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class BusinessRegisterActivity extends AppCompatActivity {

    private TextInputEditText etOwnerName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister, btnGoogle;
    private TextView tvLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private boolean isGoogleLinked = false;

    private TextInputLayout tilPassword, tilConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        etOwnerName = findViewById(R.id.et_stall_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        btnGoogle = findViewById(R.id.btn_google_signin);
        tvLogin = findViewById(R.id.tv_login);

        // Check if we came from a Google login redirect
        String googleEmail = getIntent().getStringExtra("google_email");
        if (googleEmail != null) {
            isGoogleLinked = true;
            etEmail.setText(googleEmail);
            etEmail.setEnabled(false);
            tilPassword.setVisibility(View.GONE);
            tilConfirmPassword.setVisibility(View.GONE);
            btnGoogle.setVisibility(View.GONE);
            
            // Change UI to reflect account completion
            btnRegister.setText("Complete Account");
        }

        btnRegister.setOnClickListener(v -> {
            String ownerName = etOwnerName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (ownerName.isEmpty() || email.isEmpty() || (!isGoogleLinked && (password.isEmpty() || confirmPassword.isEmpty()))) {
                Toast.makeText(this, R.string.error_fill_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isGoogleLinked) {
                if (!password.equals(confirmPassword)) {
                    Toast.makeText(this, R.string.error_password_mismatch, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(this, R.string.error_password_too_short, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (isGoogleLinked) {
                // Already authenticated via Google, just save data
                saveBusinessToFirestore(mAuth.getCurrentUser().getUid(), ownerName, email);
            } else {
                // Email/Password registration
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                saveBusinessToFirestore(mAuth.getCurrentUser().getUid(), ownerName, email);
                            } else {
                                Toast.makeText(BusinessRegisterActivity.this, "Registration failed: " + task.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        btnGoogle.setOnClickListener(v -> signInWithGoogle());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void signInWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                handleSignInError(e);
            }
        }
    }

    private void handleSignInError(ApiException e) {
        String message;
        int statusCode = e.getStatusCode();

        if (statusCode == CommonStatusCodes.DEVELOPER_ERROR || statusCode == 10) {
            message = "Developer Error (" + statusCode + "). This usually means:\n" +
                    "1. Support Email is not set in Firebase Console Settings.\n" +
                    "2. SHA-1 fingerprint is missing or incorrect in Firebase Console.\n" +
                    "3. Google Sign-In is not enabled in Firebase Auth.";
        } else if (statusCode == 7) {
            message = "Network Error. Please check your internet connection.";
        } else if (statusCode == 12501) {
            return;
        } else {
            message = "Google Sign-In failed (Status Code: " + statusCode + "). " + e.getMessage();
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sign-In Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        String email = mAuth.getCurrentUser().getEmail();
                        
                        // Check if user already exists
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        String role = documentSnapshot.getString("userRole");
                                        if ("business".equals(role)) {
                                            // Already a business user, log them in
                                            sessionManager.createLoginSession(email, "business");
                                            Intent intent = new Intent(BusinessRegisterActivity.this, AdminDashboardActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(this, "This account is registered as a customer. Please use Customer Login.", Toast.LENGTH_LONG).show();
                                            mAuth.signOut();
                                        }
                                    } else {
                                        // New business user, link Google and wait for owner name
                                        isGoogleLinked = true;
                                        etEmail.setText(email);
                                        etEmail.setEnabled(false);
                                        tilPassword.setVisibility(View.GONE);
                                        tilConfirmPassword.setVisibility(View.GONE);
                                        btnGoogle.setVisibility(View.GONE);
                                        Toast.makeText(this, "Google account linked. Please enter your name.", Toast.LENGTH_LONG).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error checking account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Google authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveBusinessToFirestore(String userId, String ownerName, String email) {
        btnRegister.setEnabled(false);
        btnRegister.setText("Saving...");

        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

        // 1. Save to users collection
        Map<String, Object> user = new HashMap<>();
        user.put("userName", ownerName);
        user.put("userEmail", email);
        user.put("userRole", "business");
        user.put("createdAt", now);

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    // 2. Create a skeleton stall entry in the stalls collection
                    // The business owner will fill this in via AccountSettingsActivity later
                    Map<String, Object> stall = new HashMap<>();
                    stall.put("stallName", "New Stall"); // Default name
                    stall.put("stallEmail", email);
                    stall.put("ownerId", userId);
                    
                    db.collection("stalls").document(userId)
                            .set(stall)
                            .addOnSuccessListener(stallVoid -> {
                                sessionManager.createLoginSession(email, "business");
                                Intent intent = new Intent(BusinessRegisterActivity.this, AdminDashboardActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnRegister.setEnabled(true);
                                btnRegister.setText("Register");
                                showError("Database Error", "Failed to initialize stall profile: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText(isGoogleLinked ? "Complete Account" : "Register");
                    showError("Database Error", "Failed to save profile: " + e.getMessage());
                });
    }

    private void showError(String title, String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
