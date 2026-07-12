package com.project.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BottomActionsBehavior extends CoordinatorLayout.Behavior<View> {

    public BottomActionsBehavior() {
    }

    public BottomActionsBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull View dependency) {
        return dependency instanceof BottomNavigationView;
    }

    @Override
    public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull View dependency) {
        float navHeight = dependency.getHeight();
        if (navHeight == 0) {
            navHeight = dependency.getMeasuredHeight();
        }
        
        int navigationBarHeight = 0;
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(parent);
        if (insets != null) {
            navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
        }
        
        float dy = dependency.getTranslationY(); // ranges from 0 (visible) to navHeight (hidden)
        float progress = navHeight > 0 ? (dy / navHeight) : 0f;
        
        // Translate child from -navHeight (when BottomNav is visible) to -navigationBarHeight (when BottomNav is hidden)
        float targetTranslationY = -navHeight + progress * (navHeight - navigationBarHeight);
        child.setTranslationY(targetTranslationY);
        return true;
    }
}
