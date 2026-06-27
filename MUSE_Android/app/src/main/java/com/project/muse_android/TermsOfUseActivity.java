package com.project.muse_android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.project.muse_android.databinding.TermsOfUseBinding;

public class TermsOfUseActivity extends AppCompatActivity {

    private TermsOfUseBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = TermsOfUseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnNext.setOnClickListener(v -> {
            if (binding.cbAgree.isChecked()) {
                Intent intent = new Intent(TermsOfUseActivity.this, PrivacyPolicyActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Vui lòng đồng ý với điều khoản để tiếp tục", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnSkip.setOnClickListener(v -> {
            Intent intent = new Intent(TermsOfUseActivity.this, NotificationScreenActivity.class);
            startActivity(intent);
        });
    }
}