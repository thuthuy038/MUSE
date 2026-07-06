package com.project.muse_android.ai;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.project.models.Banner;
import com.project.models.User;
import com.project.muse_android.R;
import com.project.muse_android.auth.AISetupActivity;
import com.project.muse_android.auth.AuthActivity;
import com.project.muse_android.databinding.FragmentAiBinding;
import com.project.network.ApiClient;
import com.project.network.HomeApiClient;
import com.project.network.HomeApiService;
import com.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AIFragment extends Fragment {

    private FragmentAiBinding binding;
    private SessionManager sessionManager;
    private HomeApiService homeApiService;

    private final List<Banner> bannerList = new ArrayList<>();
    private int currentBannerIndex = 0;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private Runnable bannerRunnable;
    private com.project.adapters.BannerAdapter bannerAdapter;

    // Available options for selection in guest or user edit mode
    private final String[] STYLES = {"Elegant", "Minimalist", "Streetwear", "Vintage", "Korean", "Japanese", "Casual", "Office", "Luxury", "Sport", "Classic"};
    private final String[] COLORS = {"White", "Black", "Pink", "Beige", "Brown", "Blue", "Green", "Gray"};
    private final String[] PURPOSES = {"Daily", "Work", "Party", "Travel", "Dating"};
    private final String[] SHAPES = {"Dáng cân đối", "Dáng đồng hồ cát", "Dáng quả lê", "Dáng quả táo", "Dáng hình chữ nhật"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        homeApiService = HomeApiClient.getHomeApiService();

        binding.ivBack.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });

        binding.btnEditProfile.setOnClickListener(v -> {
            if (sessionManager.isLoggedIn()) {
                Intent intent = new Intent(getActivity(), AISetupActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để sử dụng tính năng thiết lập AI Profile!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), AuthActivity.class);
                startActivity(intent);
            }
        });

        // Initialize ViewPager2 for AI seasonal banners
        bannerAdapter = new com.project.adapters.BannerAdapter(bannerList);
        binding.vpBannersAI.setAdapter(bannerAdapter);
        binding.vpBannersAI.setPageTransformer((page, position) -> {
            page.setAlpha(1 - Math.abs(position));
            page.setScaleX(0.9f + (1 - Math.abs(position)) * 0.1f);
        });
        binding.vpBannersAI.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position < bannerList.size()) {
                    Banner banner = bannerList.get(position);
                    binding.tvBannerContent.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                        if (binding == null) return;
                        if (banner.getTitle() != null && !banner.getTitle().isEmpty()) {
                            binding.tvBannerContent.setText(banner.getTitle());
                        }
                        binding.tvBannerContent.animate().alpha(1f).setDuration(200).start();
                    }).start();
                }
            }
        });

        loadUserProfile();
        loadBannersAndStartRotation();
    }

    private void loadUserProfile() {
        String token = sessionManager.getToken();
        if (token == null) {
            // Guest mode: load guest UI without redirecting to login
            setupGuestUI();
            return;
        }

        // Fetch user profile from REST API database
        ApiClient.INSTANCE.getInstance().getProfile("Bearer " + token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!isAdded() || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    populateUserUI(user);
                } else {
                    Toast.makeText(getContext(), "Không thể tải thông tin profile từ database", Toast.LENGTH_SHORT).show();
                    loadCachedFallback();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                if (!isAdded() || binding == null) return;
                Toast.makeText(getContext(), "Lỗi kết nối database: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                loadCachedFallback();
            }
        });
    }

    private void setupGuestUI() {
        binding.tvWelcomeTitle.setText("Xin chào, Người đẹp ✨");
        binding.tvUserName.setText("Người đẹp");
        binding.tvUserLabel.setText("AI Profile Khách");
        binding.ivUserAvatar.setImageResource(R.drawable.ic_account_circle);

        // Populate with full options for guests
        populateSpinnerWithOptions(binding.spFavoriteStyle, STYLES, null);
        populateSpinnerWithOptions(binding.spColorPalette, COLORS, null);
        populateSpinnerWithOptions(binding.spStyleVibe, PURPOSES, null);
        populateBodyShapeSpinner(binding.spBodyShape, null);

        // Show setup popup for guest (named "Người đẹp")
        showSetupPopup();
    }

    private void populateUserUI(User user) {
        binding.tvWelcomeTitle.setText("Xin chào, " + user.getName() + " ✨");
        binding.tvUserName.setText(user.getName());
        binding.tvUserLabel.setText(user.isProfileCompleted() ? "AI Profile Đã Hoàn Thành" : "AI Profile Chưa Hoàn Thành");

        // Load Avatar
        if (user.getAvatar() != null && user.getAvatar().getUrl() != null && !user.getAvatar().getUrl().isEmpty()) {
            String avatarUrl = user.getAvatar().getUrl();
            if (avatarUrl.startsWith("http") || avatarUrl.startsWith("/")) {
                String fullUrl = avatarUrl.startsWith("/") ? "https://server-testing-ymn9.onrender.com" + avatarUrl : avatarUrl;
                Glide.with(this).load(fullUrl).into(binding.ivUserAvatar);
            } else {
                try {
                    byte[] decodedString = Base64.decode(avatarUrl, Base64.DEFAULT);
                    Glide.with(this).load(decodedString).into(binding.ivUserAvatar);
                } catch (Exception e) {
                    binding.ivUserAvatar.setImageResource(R.drawable.ic_account_circle);
                }
            }
        } else {
            binding.ivUserAvatar.setImageResource(R.drawable.ic_account_circle);
        }

        // Populating Spinners from User database data (pre-select saved selection)
        populateSpinnerWithOptions(binding.spFavoriteStyle, STYLES, user.getFavoriteStyles());
        populateSpinnerWithOptions(binding.spColorPalette, COLORS, user.getFavoriteColors());
        populateSpinnerWithOptions(binding.spStyleVibe, PURPOSES, user.getFashionPurpose());
        populateBodyShapeSpinner(binding.spBodyShape, user);

        // Show setup popup if name is "Người đẹp" or profile is not completed
        if ("Người đẹp".equals(user.getName()) || !user.isProfileCompleted()) {
            showSetupPopup();
        }
    }

    private void showSetupPopup() {
        if (!isAdded() || getContext() == null) return;

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_setup_required);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.findViewById(R.id.btnMaybeLater).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnCompleteSetup).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(getActivity(), AISetupActivity.class);
            startActivity(intent);
        });

        dialog.show();
    }

    private void populateSpinnerWithOptions(Spinner spinner, String[] options, List<String> userPreferences) {
        List<String> list = new ArrayList<>();
        int selectedIndex = 0;

        String target = null;
        if (userPreferences != null && !userPreferences.isEmpty()) {
            target = userPreferences.get(0);
        }

        for (int i = 0; i < options.length; i++) {
            list.add(options[i]);
            if (target != null && target.equalsIgnoreCase(options[i])) {
                selectedIndex = i;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                list
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selectedIndex);
    }

    private void populateBodyShapeSpinner(Spinner spinner, User user) {
        List<String> list = new ArrayList<>();
        int selectedIndex = 0;

        if (user != null && user.getHeight() > 0 && user.getWeight() > 0) {
            String bmiSpec = "Chiều cao: " + user.getHeight() + " cm | Cân nặng: " + (int) user.getWeight() + " kg";
            list.add(bmiSpec);

            float heightM = user.getHeight() / 100f;
            float bmi = user.getWeight() / (heightM * heightM);
            String bmiShape = "Dáng người: Cân đối";
            if (bmi < 18.5f) bmiShape = "Dáng người: Mảnh mai";
            else if (bmi >= 25f && bmi < 30f) bmiShape = "Dáng người: Đầy đặn";
            else if (bmi >= 30f) bmiShape = "Dáng người: Tròn trịa";
            list.add(bmiShape);

            selectedIndex = 0; // Show measurement specs as first selected option
        } else {
            list.add("(Chưa thiết lập số đo)");
        }

        for (String shape : SHAPES) {
            list.add(shape);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                list
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selectedIndex);
    }

    private void loadCachedFallback() {
        binding.tvWelcomeTitle.setText("Xin chào, " + sessionManager.getUserName() + " ✨");
        binding.tvUserName.setText(sessionManager.getUserName());
        binding.tvUserLabel.setText(sessionManager.isProfileCompleted() ? "AI Profile Đã Hoàn Thành" : "AI Profile Chưa Hoàn Thành");
        binding.ivUserAvatar.setImageResource(R.drawable.ic_account_circle);

        populateSpinnerWithOptions(binding.spFavoriteStyle, STYLES, null);
        populateSpinnerWithOptions(binding.spColorPalette, COLORS, null);
        populateSpinnerWithOptions(binding.spStyleVibe, PURPOSES, null);
        populateBodyShapeSpinner(binding.spBodyShape, null);
    }

    private void loadBannersAndStartRotation() {
        homeApiService.getBanners().enqueue(new Callback<List<Banner>>() {
            @Override
            public void onResponse(Call<List<Banner>> call, Response<List<Banner>> response) {
                if (!isAdded() || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    bannerList.clear();
                    for (Banner banner : response.body()) {
                        if (banner.getStatus() == null || "active".equalsIgnoreCase(banner.getStatus())) {
                            bannerList.add(banner);
                        }
                    }

                    if (!bannerList.isEmpty()) {
                        bannerAdapter.notifyDataSetChanged();
                        binding.vpBannersAI.setCurrentItem(0, false);
                        if (bannerList.get(0).getTitle() != null) {
                            binding.tvBannerContent.setText(bannerList.get(0).getTitle());
                        }
                        startRotationTimer();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Banner>> call, Throwable t) {
                // Fallback is default static layout banner in fragment_ai.xml
            }
        });
    }

    private void startRotationTimer() {
        if (bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }

        bannerRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding == null || bannerList.isEmpty()) return;

                int nextItem = (binding.vpBannersAI.getCurrentItem() + 1) % bannerList.size();
                binding.vpBannersAI.setCurrentItem(nextItem, true);

                bannerHandler.postDelayed(this, 5000); // Change banner every 5 seconds
            }
        };

        bannerHandler.postDelayed(bannerRunnable, 5000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }
        binding = null;
    }
}
