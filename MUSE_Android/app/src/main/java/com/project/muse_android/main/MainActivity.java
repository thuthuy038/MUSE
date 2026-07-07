package com.project.muse_android.main;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import androidx.core.content.ContextCompat;
import com.google.android.material.badge.BadgeDrawable;
import com.project.models.Notification;
import com.project.muse_android.R;
import com.project.muse_android.auth.AuthActivity;
import com.project.muse_android.databinding.ActivityMainBinding;
import com.project.muse_android.notification.NotificationScreenActivity;
import com.project.network.ApiClient;
import com.project.utils.SessionManager;
import com.project.utils.ViewUtils;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

            // Chặn sự kiện click vào Profile/Notification
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

                if (itemId == R.id.navigation_notification) {
                    return NavigationUI.onNavDestinationSelected(item, navController);
                }
                
                // Sử dụng NavigationUI để xử lý chuyển trang và quản lý backstack
                boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
                if (!handled && itemId == R.id.navigation_home) {
                    return true;
                }
                return handled;
            });

            setupDraggableAI();
            startAIFloatingAnimation();

            // Ẩn/hiện bong bóng AI tùy theo fragment và đảm bảo hiện lại Bottom Nav
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.navigation_ai) {
                    binding.btnAIDraggable.setVisibility(View.GONE);
                } else {
                    binding.btnAIDraggable.setVisibility(View.VISIBLE);
                }
                binding.bottomNavigationView.animate().translationY(0).setDuration(300).start();
            });

            handleIntent(getIntent());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationBadge();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    public void updateNotificationBadge() {
        if (!sessionManager.isLoggedIn()) {
            binding.bottomNavigationView.removeBadge(R.id.navigation_notification);
            return;
        }

        String userId = sessionManager.getUserId();
        String token = "Bearer " + sessionManager.getToken();
        android.util.Log.d("NotificationBadge", "updateNotificationBadge: userId=" + userId + ", token=" + token);
        ApiClient.INSTANCE.getInstance().getNotifications(token, userId).enqueue(new Callback<com.project.models.NotificationResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.project.models.NotificationResponse> call, @NonNull Response<com.project.models.NotificationResponse> response) {
                android.util.Log.d("NotificationBadge", "onResponse: code=" + response.code() + ", isSuccessful=" + response.isSuccessful());
                int unreadCount = 0;
                
                // Server notifications
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    android.util.Log.d("NotificationBadge", "Server notifications fetched: " + response.body().getData().size());
                    for (Notification n : response.body().getData()) {
                        android.util.Log.d("NotificationBadge", "Server Notification: id=" + n.getId() + ", title=" + n.getTitle() + ", status=" + n.getStatus());
                        if ("unread".equals(n.getStatus())) {
                            unreadCount++;
                        }
                    }
                } else {
                    try {
                        String err = response.errorBody() != null ? response.errorBody().string() : "empty";
                        android.util.Log.e("NotificationBadge", "Server error body: " + err);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                // Local notifications
                List<Notification> locals = sessionManager.getLocalNotifications();
                android.util.Log.d("NotificationBadge", "Local notifications found: " + locals.size());
                for (Notification n : locals) {
                    android.util.Log.d("NotificationBadge", "Local Notification: title=" + n.getTitle() + ", status=" + n.getStatus());
                    if ("unread".equals(n.getStatus())) {
                        unreadCount++;
                    }
                }

                android.util.Log.d("NotificationBadge", "Final unreadCount = " + unreadCount);

                final int finalUnreadCount = unreadCount;
                binding.bottomNavigationView.post(() -> {
                    if (finalUnreadCount > 0) {
                        BadgeDrawable badge = binding.bottomNavigationView.getOrCreateBadge(R.id.navigation_notification);
                        badge.setNumber(finalUnreadCount);
                        badge.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.error_500));
                        badge.setBadgeTextColor(ContextCompat.getColor(MainActivity.this, R.color.white));
                        badge.setVisible(true);
                    } else {
                        binding.bottomNavigationView.removeBadge(R.id.navigation_notification);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<com.project.models.NotificationResponse> call, @NonNull Throwable t) {
                android.util.Log.e("NotificationBadge", "API request failed: " + t.getMessage(), t);
            }
        });
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            if (intent.getBooleanExtra("select_profile", false)) {
                if (navController != null) {
                    navController.navigate(R.id.navigation_profile);
                    intent.removeExtra("select_profile");
                }
            } else if (intent.getBooleanExtra("open_cart", false)) {
                if (navController != null) {
                    navController.navigate(R.id.navigation_cart);
                    intent.removeExtra("open_cart");
                }
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
