package com.project.muse_android.notification;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.project.muse_android.databinding.NotificationScreenBinding;
import com.project.muse_android.main.MainActivity;

public class NotificationScreenActivity extends AppCompatActivity {

    private NotificationScreenBinding binding;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Đã bật thông báo", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Thông báo bị từ chối", Toast.LENGTH_SHORT).show();
                }
                navigateToMain();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = NotificationScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnEnable.setOnClickListener(v -> checkAndRequestNotificationPermission());

        binding.btnLater.setOnClickListener(v -> navigateToMain());

        binding.btnSkip.setOnClickListener(v -> navigateToMain());

        binding.btnGetStarted.setOnClickListener(v -> navigateToMain());

        binding.btnPrev.setOnClickListener(v -> finish());
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                navigateToMain();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            navigateToMain();
        }
    }

    private void navigateToMain() {
        com.project.utils.SessionManager sessionManager = new com.project.utils.SessionManager(this);
        sessionManager.setShouldShowOffer(true);

        Intent intent = new Intent(NotificationScreenActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}

