package com.project.muse_android.notification;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.google.android.material.tabs.TabLayoutMediator;
import com.project.muse_android.databinding.ActivityNotificationTabsBinding;
import com.project.muse_android.main.MainActivity;
import com.project.utils.ViewUtils;

public class NotificationScreenActivity extends AppCompatActivity {

    private ActivityNotificationTabsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationTabsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Header Padding
        ViewUtils.applySystemBarsPadding(binding.header, true, false);

        setupTabs();
        
        binding.viewPager.setUserInputEnabled(true);
    }

    private void setupTabs() {
        binding.viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0: return NotificationListFragment.newInstance("promotion");
                    case 1: return NotificationListFragment.newInstance("system");
                    case 2: return NotificationListFragment.newInstance("order");
                    default: return new Fragment();
                }
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        });

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Khuyến mãi"); break;
                case 1: tab.setText("Hệ thống"); break;
                case 2: tab.setText("Đơn hàng"); break;
            }
        }).attach();
    }
}
