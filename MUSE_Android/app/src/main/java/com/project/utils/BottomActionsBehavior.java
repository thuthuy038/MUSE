package com.project.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

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
        
        float translationY = dependency.getTranslationY() - navHeight;
        child.setTranslationY(translationY);
        return true;
    }
}
