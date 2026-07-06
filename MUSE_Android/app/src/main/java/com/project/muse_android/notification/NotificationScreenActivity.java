package com.project.muse_android.notification;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.project.muse_android.databinding.NotificationScreenBinding;
import com.project.muse_android.dialog.SuccessDialog;
import com.project.muse_android.main.MainActivity;

public class NotificationScreenActivity extends AppCompatActivity {

    private NotificationScreenBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = NotificationScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnEnable.setOnClickListener(v -> {
            SuccessDialog dialog = SuccessDialog.newInstance("Đã bật nhận thông báo thành công!");
            dialog.setOnCloseListener(this::navigateToHome);
            dialog.show(getSupportFragmentManager(), "success_dialog");
        });

        binding.btnLater.setOnClickListener(v -> navigateToHome());
        binding.btnSkip.setOnClickListener(v -> navigateToHome());
        binding.btnGetStarted.setOnClickListener(v -> navigateToHome());
        binding.btnPrev.setOnClickListener(v -> finish());
    }

    private void navigateToHome() {
        // Đảm bảo đánh dấu đã qua lần đầu chạy để không bị MainActivity redirect ngược lại AuthActivity
        com.project.utils.SessionManager sessionManager = new com.project.utils.SessionManager(this);
        sessionManager.setFirstLaunch(false);
        
        // Đánh dấu cần hiện thông báo chào mừng thành viên mới ở trang chủ
        sessionManager.setShouldShowOffer(true);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
