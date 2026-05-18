package com.example.uorder;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.uorder.core.auth.SelectionActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Direct to SelectionActivity for role-based entry and auto-login check
        Intent intent = new Intent(this, SelectionActivity.class);
        startActivity(intent);
        finish();
    }
}