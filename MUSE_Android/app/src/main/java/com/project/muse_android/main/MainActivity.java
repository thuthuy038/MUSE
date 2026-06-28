package com.project.muse_android.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityMainBinding;
import com.project.muse_android.home.HomeFragment;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }
}
