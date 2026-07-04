package com.project.muse_android.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import com.project.utils.SessionManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            NavigationUI.setupWithNavController(
                    binding.bottomNavigationView,
                    navController
            );

            binding.fabProfile.setOnClickListener(v -> {
                android.util.Log.d("MUSE_NAV", "Profile click: isLoggedIn=" + sessionManager.isLoggedIn());
                if (sessionManager.isLoggedIn()) {
                    Intent intent = new Intent(MainActivity.this, com.project.muse_android.profile.ProfileActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(MainActivity.this, com.project.muse_android.auth.AuthActivity.class);
                    intent.putExtra("from_profile", true);
                    startActivity(intent);
                }
            });

            // Badge thông báo
            var badge = binding.bottomNavigationView
                    .getOrCreateBadge(R.id.navigation_notification);

            badge.setVisible(true);
            badge.setNumber(3);
        }
    }
}