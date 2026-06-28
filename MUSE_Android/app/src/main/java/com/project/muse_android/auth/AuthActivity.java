package com.project.muse_android.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityAuthScreenBinding;
import com.project.muse_android.profile.ProfileActivity;
import com.project.utils.SessionManager;

public class AuthActivity extends AppCompatActivity {

    private ActivityAuthScreenBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        // 1. If already logged in, redirect directly to ProfileActivity
        if (sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // 2. If first launch, show WelcomeFragment; otherwise skip to LoginFragment
        if (savedInstanceState == null) {
            if (sessionManager.isFirstLaunch()) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.auth_container, new WelcomeFragment())
                        .commit();
            } else {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.auth_container, new LoginFragment())
                        .commit();
            }
        }
    }
}
