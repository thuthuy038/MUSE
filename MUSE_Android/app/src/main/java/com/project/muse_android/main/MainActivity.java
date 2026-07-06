package com.project.muse_android.main;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import android.content.Intent;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityMainBinding;
import com.project.utils.SessionManager;
import com.project.utils.ViewUtils;
import com.project.muse_android.auth.AuthActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private float dX, dY;
    private ObjectAnimator aiFloatAnim;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (sessionManager.isFirstLaunch()) {
            android.util.Log.d("MUSE_NAV", "First launch! Redirecting to AuthActivity from MainActivity");
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
            return;
        }

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
                        if (aiFloatAnim != null) aiFloatAnim.pause();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        v.setX(event.getRawX() + dX);
                        v.setY(event.getRawY() + dY);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (aiFloatAnim != null) aiFloatAnim.resume();
                        float dist = (float) Math.hypot(event.getRawX() - initialTouchX, event.getRawY() - initialTouchY);
                        if (dist < 10) {
                            v.performClick();
                        } else {
                            snapToEdges(v);
                        }
                        return true;
                }
                return false;
            }
        });

        binding.btnAIDraggable.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigate(R.id.navigation_ai);
            }
        });
    }

    private void startAIFloatingAnimation() {
        if (aiFloatAnim != null) aiFloatAnim.cancel();
        aiFloatAnim = ObjectAnimator.ofFloat(binding.btnAIDraggable, "translationY", -15f, 15f);
        aiFloatAnim.setDuration(2000);
        aiFloatAnim.setRepeatMode(ValueAnimator.REVERSE);
        aiFloatAnim.setRepeatCount(ValueAnimator.INFINITE);
        aiFloatAnim.start();
    }

    private void snapToEdges(View v) {
        float screenWidth = getResources().getDisplayMetrics().widthPixels;
        float targetX = (v.getX() + v.getWidth() / 2f < screenWidth / 2f) ? 40f : screenWidth - v.getWidth() - 40f;
        v.animate()
                .x(targetX)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}