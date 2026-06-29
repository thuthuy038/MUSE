package com.project.muse_android.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
                navController.navigate(R.id.navigation_profile);
            });

            // Set badge for notifications
            var badge = binding.bottomNavigationView.getOrCreateBadge(R.id.navigation_notification);
            badge.setVisible(true);
            badge.setNumber(3);

            // Handle selection state for FAB and ensure others are selected correctly
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                if (id == R.id.navigation_profile) {
                    binding.bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
                    for (int i = 0; i < binding.bottomNavigationView.getMenu().size(); i++) {
                        binding.bottomNavigationView.getMenu().getItem(i).setChecked(false);
                    }
                    binding.bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
                } else {
                    // NavigationUI handles this, but we can force it if needed
                    binding.bottomNavigationView.getMenu().findItem(id).setChecked(true);
                }
            });
        }
    }
}