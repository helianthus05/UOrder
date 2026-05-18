package com.example.uorder.core.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uorder.R;
import com.example.uorder.core.student.activities.UserDashboardActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CustomerRegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister, btnGoogle;
    private TextView tvLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        btnGoogle = findViewById(R.id.btn_google_signin);
        tvLogin = findViewById(R.id.tv_login);

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, R.string.error_fill_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, R.string.error_password_mismatch, Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, R.string.error_password_too_short, Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            String userId = mAuth.getCurrentUser().getUid();
                            saveUserToFirestore(userId, name, email);
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            new androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Registration Failed")
                                    .setMessage(error)
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    });
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
            // Sign-in cancelled by user
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
                        String name = mAuth.getCurrentUser().getDisplayName();
                        
                        // Check if user already exists before saving
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        // User exists, just log them in
                                        String role = documentSnapshot.getString("userRole");
                                        String existingName = documentSnapshot.getString("userName");
                                        
                                        if ("customer".equals(role)) {
                                            sessionManager.createLoginSession(existingName != null ? existingName : email, "customer");
                                            Intent intent = new Intent(CustomerRegisterActivity.this, UserDashboardActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(this, "This account is registered as a business. Please use Business Login.", Toast.LENGTH_LONG).show();
                                            mAuth.signOut();
                                        }
                                    } else {
                                        // New user, proceed with registration
                                        saveUserToFirestore(userId, name, email);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error checking user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Google authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email) {
        com.example.uorder.core.models.User user = new com.example.uorder.core.models.User(name, email, "customer");

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    sessionManager.createLoginSession(name != null ? name : email, "customer");
                    Intent intent = new Intent(CustomerRegisterActivity.this, UserDashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Database Error")
                            .setMessage("Successfully authenticated, but failed to save profile: " + e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                });
    }
}
