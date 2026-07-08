package com.project.utils;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ViewUtils {

    /**
     * Tự động thêm Padding cho View để tránh bị đè bởi Status Bar (ở trên) hoặc Navigation Bar (ở dưới).
     */
    public static void applySystemBarsPadding(View view, boolean applyTop, boolean applyBottom) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            int paddingTop = applyTop ? insets.top : v.getPaddingTop();
            int paddingBottom = applyBottom ? insets.bottom : v.getPaddingBottom();
            
            v.setPadding(v.getPaddingLeft(), paddingTop, v.getPaddingRight(), paddingBottom);
            
            return windowInsets;
        });
    }

    /**
     * Tự động thêm Margin (khoảng cách lề) cho View. Thường dùng cho các nút nổi hoặc Bottom Nav.
     */
    public static void applySystemBarsMargin(View view, boolean applyTop, boolean applyBottom) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            if (applyTop) mlp.topMargin = insets.top;
            if (applyBottom) mlp.bottomMargin = insets.bottom;
            v.setLayoutParams(mlp);
            
            return windowInsets;
        });
    }

    /**
     * Set up bottom navigation view to route navigation requests to MainActivity.
     */
    public static void setupBottomNavigation(
            com.google.android.material.bottomnavigation.BottomNavigationView navView,
            android.content.Context context) {
        
        applySystemBarsPadding(navView, false, true);

        navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            android.content.Intent intent = new android.content.Intent(context, com.project.muse_android.main.MainActivity.class);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            
            if (itemId == com.project.muse_android.R.id.navigation_home) {
                intent.putExtra("open_home", true);
                context.startActivity(intent);
                return true;
            } else if (itemId == com.project.muse_android.R.id.navigation_explore) {
                intent.putExtra("open_explore", true);
                context.startActivity(intent);
                return true;
            } else if (itemId == com.project.muse_android.R.id.navigation_profile) {
                intent.putExtra("select_profile", true);
                context.startActivity(intent);
                return true;
            } else if (itemId == com.project.muse_android.R.id.navigation_cart) {
                intent.putExtra("open_cart", true);
                context.startActivity(intent);
                return true;
            } else if (itemId == com.project.muse_android.R.id.navigation_notification) {
                intent.putExtra("open_notification", true);
                context.startActivity(intent);
                return true;
            }
            return false;
        });
    }
}
