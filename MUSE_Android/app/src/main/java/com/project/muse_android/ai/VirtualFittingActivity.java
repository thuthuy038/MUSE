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
        
        binding.btnFittingCart.setOnClickListener(v -> {
            // Handle Fitting from Cart
        });

        binding.btnFittingSearch.setOnClickListener(v -> {
            // Handle Fitting from Search
        });
    }
}
