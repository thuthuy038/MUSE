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
}
