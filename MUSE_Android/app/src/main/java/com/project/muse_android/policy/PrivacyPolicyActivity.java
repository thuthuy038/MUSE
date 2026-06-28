package com.project.muse_android.policy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.project.muse_android.databinding.PrivacyPolicyBinding;
import com.project.muse_android.notification.NotificationScreenActivity;

public class PrivacyPolicyActivity extends AppCompatActivity {

    private PrivacyPolicyBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PrivacyPolicyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnNext.setOnClickListener(v -> {
            if (binding.cbAgree.isChecked()) {
                Intent intent = new Intent(PrivacyPolicyActivity.this, NotificationScreenActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Vui lòng đồng ý với chính sách bảo mật để tiếp tục", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnPrev.setOnClickListener(v -> finish());

        binding.btnSkip.setOnClickListener(v -> {
            Intent intent = new Intent(PrivacyPolicyActivity.this, NotificationScreenActivity.class);
            startActivity(intent);
        });
    }
}
