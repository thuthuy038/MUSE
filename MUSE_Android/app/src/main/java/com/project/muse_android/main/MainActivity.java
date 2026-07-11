package com.project.muse_android.main;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
                
                if (destination.getId() == R.id.navigation_wishlist) {
                    binding.bottomNavigationView.setVisibility(View.GONE);
                } else {
                    binding.bottomNavigationView.setVisibility(View.VISIBLE);
                    binding.bottomNavigationView.animate().translationY(0).setDuration(300).start();
                }
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
                        String nType = n.getType() != null ? n.getType().toLowerCase().trim() : "";
                        boolean isTabType = nType.equals("promotion") || nType.equals("stock") 
                                || nType.equals("system") || nType.equals("review") 
                                || nType.equals("order");
                                
                        if (isTabType && "unread".equalsIgnoreCase(n.getStatus())) {
                            unreadCount++;
                        }
                    }
                    android.util.Log.d("NotificationBadge", "Server unreadCount (3 tabs): " + unreadCount);
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
                    String nType = n.getType() != null ? n.getType().toLowerCase().trim() : "";
                    boolean isTabType = nType.equals("promotion") || nType.equals("stock") 
                            || nType.equals("system") || nType.equals("review") 
                            || nType.equals("order");
                            
                    android.util.Log.d("NotificationBadge", "Local Notification: title=" + n.getTitle() + ", status=" + n.getStatus());
                    if (isTabType && "unread".equalsIgnoreCase(n.getStatus())) {
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
            if (intent == null || navController == null) return;
    
            if (intent.getBooleanExtra("select_profile", false)) {
                navController.navigate(R.id.navigation_profile);
                intent.removeExtra("select_profile");
            } else if (intent.getBooleanExtra("open_home", false)) {
                navController.navigate(R.id.navigation_home);
                intent.removeExtra("open_home");
            } else if (intent.getBooleanExtra("open_cart", false)) {
                navController.navigate(R.id.navigation_cart);
                intent.removeExtra("open_cart");
            } else if (intent.getBooleanExtra("open_explore", false)) {
                navController.navigate(R.id.navigation_explore);
                intent.removeExtra("open_explore");
            } else if (intent.getBooleanExtra("open_ai_hub", false)) {
                navController.navigate(R.id.navigation_ai);
                intent.removeExtra("open_ai_hub");
            } else if (intent.hasExtra("category_id")) {
                String categoryId = intent.getStringExtra("category_id");
                if (categoryId != null) {
                    Bundle args = new Bundle();
                    args.putString("category_id", categoryId);
                    try {
                        navController.navigate(R.id.navigation_category_products, args);
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Navigation failed", e);
                    }
                    intent.removeExtra("category_id");
                }
            }
        }
   

    private void setupDraggableAI() {
        binding.btnAIDraggable.setOnTouchListener(new View.OnTouchListener() {
            private float initialTouchX, initialTouchY;
            private float lastX, lastY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        stopAIFloatingAnimation();
                        v.animate().cancel();
                        v.setTranslationY(0f);
                        
                        android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
                        if (params instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                            androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams lp = (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) params;
                            if (lp.gravity != (android.view.Gravity.TOP | android.view.Gravity.START)) {
                                params.leftMargin = (int) v.getX();
                                params.topMargin = (int) v.getY();
                                params.rightMargin = 0;
                                params.bottomMargin = 0;
                                lp.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
                                v.setLayoutParams(params);
                            }
                        }
                        
                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - lastX;
                        float deltaY = event.getRawY() - lastY;
                        
                        android.view.ViewGroup.MarginLayoutParams moveParams = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
                        if (moveParams instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                            androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams lp = (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) moveParams;
                            lp.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
                        }
                        
                        moveParams.leftMargin += deltaX;
                        moveParams.topMargin += deltaY;
                        
                        View parent = (View) v.getParent();
                        if (parent != null) {
                            int parentWidth = parent.getWidth();
                            int parentHeight = parent.getHeight();
                            float density = getResources().getDisplayMetrics().density;
                            
                            int maxLeft = parentWidth - v.getWidth();
                            int topBound = (int) (50 * density);
                            int bottomBound = parentHeight - v.getHeight() - (int) (160 * density);
                            
                            if (moveParams.leftMargin < 0) moveParams.leftMargin = 0;
                            if (moveParams.leftMargin > maxLeft) moveParams.leftMargin = maxLeft;
                            if (moveParams.topMargin < topBound) moveParams.topMargin = topBound;
                            if (moveParams.topMargin > bottomBound) moveParams.topMargin = bottomBound;
                        }
                        
                        v.setLayoutParams(moveParams);
                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_UP:
                        float finalX = event.getRawX();
                        float finalY = event.getRawY();
                        double distance = Math.sqrt(Math.pow(finalX - initialTouchX, 2) + Math.pow(finalY - initialTouchY, 2));

                        if (distance < 10) {
                            v.performClick();
                            navController.navigate(R.id.navigation_ai);
                            startAIFloatingAnimation();
                        } else {
                            View p = (View) v.getParent();
                            int pWidth = p != null ? p.getWidth() : 0;
                            float dens = getResources().getDisplayMetrics().density;
                            float margin = 16 * dens;
                            
                            float destX;
                            if (v.getX() + v.getWidth() / 2f < pWidth / 2f) {
                                destX = margin;
                            } else {
                                destX = pWidth - v.getWidth() - margin;
                            }
                            
                            v.animate()
                             .x(destX)
                             .setDuration(250)
                             .setInterpolator(new android.view.animation.DecelerateInterpolator())
                             .withEndAction(() -> {
                                 android.view.ViewGroup.MarginLayoutParams endParams = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
                                 if (endParams instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                                     androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams lp = (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) endParams;
                                     lp.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
                                 }
                                 endParams.leftMargin = (int) destX;
                                 endParams.topMargin = (int) v.getY();
                                 endParams.rightMargin = 0;
                                 endParams.bottomMargin = 0;
                                 v.setLayoutParams(endParams);
                                 v.setTranslationX(0f);
                                 v.setTranslationY(0f);
                                 
                                 startAIFloatingAnimation();
                             })
                             .start();
                        }
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
