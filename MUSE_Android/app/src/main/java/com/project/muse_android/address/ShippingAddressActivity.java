package com.project.muse_android.address;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.muse_android.databinding.ActivityShippingAddressBinding;

public class ShippingAddressActivity extends AppCompatActivity {

    private ActivityShippingAddressBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        binding = ActivityShippingAddressBinding.inflate(getLayoutInflater());
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
        
        binding.rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        // You would typically set an adapter here
        
        binding.btnAddAddress.setOnClickListener(v -> {
            startActivity(new Intent(this, AddShippingAddressActivity.class));
        });
    }
}
