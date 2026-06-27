package com.project.muse_android;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.project.muse_android.databinding.NotificationScreenBinding;

public class NotificationScreenActivity extends AppCompatActivity {

    private NotificationScreenBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = NotificationScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnEnable.setOnClickListener(v -> {
            Toast.makeText(this, "Đã bật thông báo!", Toast.LENGTH_SHORT).show();
        });

        binding.btnLater.setOnClickListener(v -> {
            Toast.makeText(this, "Bạn có thể cài đặt sau", Toast.LENGTH_SHORT).show();
        });

        binding.btnPrev.setOnClickListener(v -> finish());

        binding.btnGetStarted.setOnClickListener(v -> {
            Toast.makeText(this, "Chào mừng bạn đến với MUSE!", Toast.LENGTH_LONG).show();
        });

        binding.btnSkip.setOnClickListener(v -> {
            // Xử lý skip
        });
    }
}