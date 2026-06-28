package com.project.muse_android.profile;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityProfileScreenBinding;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileScreenBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.profile_container, new ProfileOverviewFragment())
                    .commit();
        }
    }
}