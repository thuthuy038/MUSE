package com.project.muse_android.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityAuthScreenBinding;
import com.project.muse_android.profile.ProfileActivity;
import com.project.muse_android.main.MainActivity;
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

        boolean fromProfile = getIntent().getBooleanExtra("from_profile", false);
        boolean fromOffer = getIntent().getBooleanExtra("from_offer", false);
        android.util.Log.d("MUSE_NAV", "AuthActivity onCreate: fromProfile=" + fromProfile + ", fromOffer=" + fromOffer + ", isFirstLaunch=" + sessionManager.isFirstLaunch() + ", isLoggedIn=" + sessionManager.isLoggedIn());

        // 1. If not first launch and not opened from the profile or offer button, redirect to MainActivity (homepage)
        if (!fromProfile && !fromOffer && !sessionManager.isFirstLaunch()) {
            android.util.Log.d("MUSE_NAV", "Redirecting to MainActivity from AuthActivity");
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // 2. Otherwise, load fragments
        if (savedInstanceState == null) {
            if (!fromProfile && !fromOffer && sessionManager.isFirstLaunch()) {
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
