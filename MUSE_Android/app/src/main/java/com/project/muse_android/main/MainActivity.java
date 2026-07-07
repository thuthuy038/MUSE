package com.project.muse_android.main;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityMainBinding;
import com.project.utils.ViewUtils;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private float dX, dY;
    private ObjectAnimator aiFloatAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge support (Dùng chuẩn Android mới)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Sử dụng Helper để tự động đẩy Bottom Nav lên trên Navigation Bar của hệ thống
        ViewUtils.applySystemBarsPadding(binding.bottomNavigationView, false, true);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            NavigationUI.setupWithNavController(
                    binding.bottomNavigationView,
                    navController
            );

            // Badge thông báo
            var badge = binding.bottomNavigationView
                    .getOrCreateBadge(R.id.navigation_notification);

            badge.setVisible(true);
            badge.setNumber(3);

            setupDraggableAI();
            startAIFloatingAnimation();

            // Ẩn/hiện bong bóng AI tùy theo fragment (tùy chọn)
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.navigation_ai) {
                    binding.btnAIDraggable.setVisibility(View.GONE);
                } else {
                    binding.btnAIDraggable.setVisibility(View.VISIBLE);
                }
            });

            handleIntent();
        }
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent();
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent == null || navController == null) return;

        if (intent.getBooleanExtra("open_cart", false)) {
            navController.navigate(R.id.navigation_cart);
            intent.removeExtra("open_cart");
        } else if (intent.hasExtra("category_id")) {
            String categoryId = intent.getStringExtra("category_id");
            if (categoryId != null) {
                Bundle args = new Bundle();
                args.putString("category_id", categoryId);
                try {
                    navController.navigate(R.id.navigation_category_products, args);
                } catch (Exception e) {
                    Log.e("MainActivity", "Navigation failed", e);
                }
                intent.removeExtra("category_id");
            }
        }
    }

    private void setupDraggableAI() {
        binding.btnAIDraggable.setOnTouchListener(new View.OnTouchListener() {
            private float initialTouchX, initialTouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        stopAIFloatingAnimation();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        v.setX(event.getRawX() + dX);
                        v.setY(event.getRawY() + dY);
                        return true;

                    case MotionEvent.ACTION_UP:
                        float finalX = event.getRawX();
                        float finalY = event.getRawY();
                        double distance = Math.sqrt(Math.pow(finalX - initialTouchX, 2) + Math.pow(finalY - initialTouchY, 2));

                        if (distance < 10) {
                            v.performClick();
                            navController.navigate(R.id.navigation_ai);
                        }
                        startAIFloatingAnimation();
                        return true;
                }
                return false;
            }
        });
    }

    private void startAIFloatingAnimation() {
        if (aiFloatAnim != null && aiFloatAnim.isRunning()) return;

        aiFloatAnim = ObjectAnimator.ofFloat(binding.btnAIDraggable, "translationY", -20f, 20f);
        aiFloatAnim.setDuration(1500);
        aiFloatAnim.setRepeatMode(ValueAnimator.REVERSE);
        aiFloatAnim.setRepeatCount(ValueAnimator.INFINITE);
        aiFloatAnim.setInterpolator(new DecelerateInterpolator());
        aiFloatAnim.start();
    }

    private void stopAIFloatingAnimation() {
        if (aiFloatAnim != null) {
            aiFloatAnim.cancel();
        }
    }
}
