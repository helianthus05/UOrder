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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CustomerLoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogle;
    private TextView tvRegister;
    private FirebaseAuth mAuth;
    private SessionManager sessionManager;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);

        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogle = findViewById(R.id.btn_google_signin);
        tvRegister = findViewById(R.id.tv_register);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.error_fill_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            checkUserRoleAndRedirect(mAuth.getCurrentUser().getUid(), email);
                        } else {
                            Toast.makeText(CustomerLoginActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerLoginActivity.this, CustomerRegisterActivity.class);
            startActivity(intent);
        });
        
        btnGoogle.setOnClickListener(v -> signInWithGoogle());
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
                        checkUserRoleAndRedirect(userId, email);
                    } else {
                        Toast.makeText(this, "Google authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRoleAndRedirect(String userId, String email) {
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String role = document.getString("userRole");
                            if ("customer".equals(role)) {
                                String name = document.getString("userName");
                                sessionManager.createLoginSession(name != null ? name : email, "customer");
                                Intent intent = new Intent(this, UserDashboardActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(this, "This account is registered as a business. Please use Business Login.", Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                            }
                        } else {
                            // If user doesn't exist in Firestore (first time Google login)
                            saveNewUser(userId, email);
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Firestore Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveNewUser(String userId, String email) {
        String name = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getDisplayName() : "User";
        com.example.uorder.core.models.User newUser = new com.example.uorder.core.models.User(name, email, "customer");

        FirebaseFirestore.getInstance().collection("users").document(userId).set(newUser)
                .addOnSuccessListener(aVoid -> {
                    sessionManager.createLoginSession(name, "customer");
                    Intent intent = new Intent(this, UserDashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}