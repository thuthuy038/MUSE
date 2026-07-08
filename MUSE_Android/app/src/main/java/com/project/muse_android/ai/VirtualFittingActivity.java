package com.project.muse_android.ai;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.project.muse_android.databinding.ActivityVirtualFittingBinding;

public class VirtualFittingActivity extends AppCompatActivity {
    private ActivityVirtualFittingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVirtualFittingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Fix header overlap
        com.project.utils.ViewUtils.applySystemBarsPadding(binding.layoutHeader, true, false);

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnAiAgent.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, ChatBotActivity.class);
            startActivity(intent);
        });
        
        binding.btnFittingCart.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, FittingCartActivity.class);
            startActivity(intent);
        });

        binding.btnFittingSearch.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, FittingSearchActivity.class);
            startActivity(intent);
        });

        // Setup Bottom Navigation
        com.project.utils.ViewUtils.setupBottomNavigation(binding.bottomNavigationView, this);
    }
}
