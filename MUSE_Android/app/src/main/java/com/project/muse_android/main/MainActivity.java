package com.project.muse_android.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import com.project.utils.SessionManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.project.muse_android.R;
import com.project.muse_android.auth.AuthActivity;
import com.project.muse_android.databinding.ActivityMainBinding;
import com.project.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isFirstLaunch()) {
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
            return;
        }

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

            // Chặn sự kiện click vào Profile để kiểm tra đăng nhập
            binding.bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_profile) {
                    if (!sessionManager.isLoggedIn()) {
                        Intent intent = new Intent(MainActivity.this, AuthActivity.class);
                        intent.putExtra("from_profile", true);
                        startActivity(intent);
                        return false;
                    }
                }
                return NavigationUI.onNavDestinationSelected(item, navController);
            });

            // Badge thông báo
            var badge = binding.bottomNavigationView
                    .getOrCreateBadge(R.id.navigation_notification);

            badge.setVisible(true);
            badge.setNumber(3);

            handleIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("select_profile", false)) {
            if (navController != null) {
                navController.navigate(R.id.navigation_profile);
            }
        }
    }
}