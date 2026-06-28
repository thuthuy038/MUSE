package com.project.muse_android.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.project.muse_android.databinding.ActivityWelcomeScreenBinding;
import com.project.muse_android.policy.TermsOfUseActivity;

public class WelcomeScreen extends AppCompatActivity {

    private ActivityWelcomeScreenBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeScreen.this, TermsOfUseActivity.class);
            startActivity(intent);
        });

        binding.btnSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeScreen.this, LoginActivity.class);
            startActivity(intent);
        });
    }
}
