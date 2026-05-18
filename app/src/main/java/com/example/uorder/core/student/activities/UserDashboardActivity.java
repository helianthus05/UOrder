package com.example.uorder.core.student.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.uorder.R;
import com.example.uorder.core.auth.SelectionActivity;
import com.example.uorder.core.auth.SessionManager;
import com.example.uorder.core.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.example.uorder.core.student.fragments.HistoryFragment;
import com.example.uorder.core.student.fragments.MenuFragment;
import com.example.uorder.core.student.fragments.OrdersFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class UserDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private ListenerRegistration orderListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        sessionManager = new SessionManager(this);
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);

        setupNotificationListener();

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        
        // Handle navigation from other activities (like Checkout)
        int targetFragment = getIntent().getIntExtra("target_fragment", -1);
        if (targetFragment != -1) {
            nav.setSelectedItemId(targetFragment);
            if (targetFragment == R.id.nav_menu) loadFragment(new MenuFragment());
            else if (targetFragment == R.id.nav_orders) loadFragment(new OrdersFragment());
            else if (targetFragment == R.id.nav_history) loadFragment(new HistoryFragment());
        } else {
            // Initial fragment: Menu
            loadFragment(new MenuFragment());
        }

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_menu) {
                loadFragment(new MenuFragment());
                return true;
            } else if (id == R.id.nav_orders) {
                loadFragment(new OrdersFragment());
                return true;
            } else if (id == R.id.nav_history) {
                loadFragment(new HistoryFragment());
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, com.example.uorder.core.activities.AccountSettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    private void showLogoutConfirmation() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_confirm_msg)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    sessionManager.logoutUser();
                    Intent intent = new Intent(this, SelectionActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void setupNotificationListener() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        orderListener = FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("customerId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    for (DocumentChange dc : value.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            String status = dc.getDocument().getString("orderStatus");
                            String stallName = dc.getDocument().getString("stallName");
                            if (status != null && ("Accepted".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status))) {
                                NotificationHelper.showNotification(this,
                                        "Order " + status,
                                        "Your order from " + (stallName != null ? stallName : "the stall") + " has been " + status.toLowerCase() + ".");
                            }
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderListener != null) {
            orderListener.remove();
        }
    }
}
