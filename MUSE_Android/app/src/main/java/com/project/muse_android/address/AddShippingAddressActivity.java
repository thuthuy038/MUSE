package com.project.muse_android.address;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.project.muse_android.databinding.ActivityAddShippingAddressBinding;

public class AddShippingAddressActivity extends AppCompatActivity {

    private ActivityAddShippingAddressBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        binding = ActivityAddShippingAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        setupUI();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.btnComplete.setOnClickListener(v -> {
            // Logic for saving address would go here
            finish();
        });
    }
}
