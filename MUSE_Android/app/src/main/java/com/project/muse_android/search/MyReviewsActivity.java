package com.project.muse_android.search;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayoutMediator;
import com.project.models.Order;
import com.project.models.User;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityMyReviewsBinding;
import com.project.muse_android.product.WriteReviewActivity;
import com.project.network.ApiClient;
import com.project.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyReviewsActivity extends AppCompatActivity {

    private ActivityMyReviewsBinding binding;
    private SessionManager sessionManager;

    private final ActivityResultLauncher<Intent> reviewLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Switch to "Đã đánh giá" tab (index 1)
                    binding.viewPager.setCurrentItem(1, true);
                }
            }
    );

    public void startReview(Order order) {
        Intent intent = new Intent(this, WriteReviewActivity.class);
        intent.putExtra("order", order);
        reviewLauncher.launch(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        setupUI();
        loadUserProfile();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Setup ViewPager with Tabs
        binding.viewPager.setAdapter(new MyReviewsPagerAdapter(this));
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Chưa đánh giá" : "Đã đánh giá");
        }).attach();
    }

    private void loadUserProfile() {
        String token = sessionManager.getToken();
        if (token != null) {
            ApiClient.INSTANCE.getInstance().getProfile("Bearer " + token).enqueue(new Callback<User>() {
                @Override
                public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        User user = response.body();
                        updateHeader(user);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                    Toast.makeText(MyReviewsActivity.this, "Lỗi tải thông tin", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateHeader(User user) {
        if (user.getAvatar() != null && user.getAvatar().getUrl() != null) {
            String url = user.getAvatar().getUrl();
            if (!url.startsWith("http")) url = "https://server-testing-ymn9.onrender.com" + (url.startsWith("/") ? "" : "/") + url;
            Glide.with(this).load(url).placeholder(R.drawable.ic_account_circle).circleCrop().into(binding.ivUserAvatar);
        }
    }

    private static class MyReviewsPagerAdapter extends FragmentStateAdapter {
        public MyReviewsPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return position == 0 ? new UnreviewedFragment() : new ReviewedFragment();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
