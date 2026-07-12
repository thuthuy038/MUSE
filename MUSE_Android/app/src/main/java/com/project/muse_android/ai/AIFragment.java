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

    private final String[] STYLES = {"Chưa có", "Elegant", "Minimalist", "Streetwear", "Vintage", "Korean", "Japanese", "Casual", "Office", "Luxury", "Sport", "Classic"};
    private final String[] COLORS = {"Chưa có", "White", "Black", "Pink", "Beige", "Brown", "Blue", "Green", "Gray"};
    private final String[] PURPOSES = {"Chưa có", "Daily", "Work", "Party", "Travel", "Dating"};
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

        com.project.utils.ViewUtils.applySystemBarsPadding(binding.header, true, false);

        binding.ivBack.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });

        binding.btnEditProfile.setOnClickListener(v -> {
            if (isAIProfileEmpty()) {
                showSetupPopup();
            } else {
                Intent intent = new Intent(getActivity(), AISetupActivity.class);
                startActivity(intent);
            }
        });

        binding.cardAiAgent.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ChatBotActivity.class);
            startActivity(intent);
        });

        binding.cardArchival.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ArchivalActivity.class);
            startActivity(intent);
        });

        binding.cardVirtualFitting.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), VirtualFittingActivity.class);
            startActivity(intent);
        });

        binding.cardOutfitSpace.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), VirtualFittingActivity.class);
            intent.putExtra("outfit_scan", true);
            startActivity(intent);
        });

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

            setupGuestUI();
            return;
        }

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
        binding.ivUserAvatar.setImageResource(R.drawable.ic_profile_vector);

        setupSpinnersFromPrefs();
        updateProfileStatusUI();

        if (isAIProfileEmpty()) {
            showSetupPopup();
        }
    }

    private void populateUserUI(User user) {
        binding.tvWelcomeTitle.setText("Xin chào, " + user.getName() + " ✨");
        binding.tvUserName.setText(user.getName());
        binding.tvUserLabel.setText(user.isProfileCompleted() ? "AI Profile Đã Hoàn Thành" : "AI Profile Chưa Hoàn Thành");

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
                    binding.ivUserAvatar.setImageResource(R.drawable.ic_profile_vector);
                }
            }
        } else {
            binding.ivUserAvatar.setImageResource(R.drawable.ic_profile_vector);
        }

        if (getContext() != null) {
            android.content.SharedPreferences.Editor editor = requireContext().getSharedPreferences("AI_PREFS", android.content.Context.MODE_PRIVATE).edit();
            String userIdKey = getCurrentUserId();
            if (user.getGender() != null && !user.getGender().isEmpty()) {
                editor.putString(userIdKey + "_gender", user.getGender());
            }
            if (user.getHeight() > 0) {
                editor.putInt(userIdKey + "_height", user.getHeight());
            }
            if (user.getWeight() > 0) {
                editor.putInt(userIdKey + "_weight", (int) user.getWeight());
            }
            
            if (user.getFavoriteStyles() != null && !user.getFavoriteStyles().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String style : user.getFavoriteStyles()) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(style);
                }
                editor.putString(userIdKey + "_styles", sb.toString());
            }
            editor.apply();
        }

        setupSpinnersFromPrefs();
        updateProfileStatusUI();

        if (isAIProfileEmpty()) {
            showSetupPopup();
        }
    }

    private String getCurrentUserId() {
        if (sessionManager == null) return "guest";
        String userId = sessionManager.getUserId();
        return (userId != null) ? userId : "guest";
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
            if (sessionManager.isLoggedIn()) {
                Intent intent = new Intent(getActivity(), AISetupActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để sử dụng tính năng thiết lập AI Profile!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), AuthActivity.class);
                startActivity(intent);
            }
        });

        dialog.show();
    }

    private boolean isAIProfileEmpty() {
        if (getContext() == null) return true;

        if (sessionManager.isLoggedIn() && sessionManager.isNewRegister()) {
            return true;
        }

        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("AI_PREFS", android.content.Context.MODE_PRIVATE);
        String userIdKey = getCurrentUserId();
        String gender = prefs.getString(userIdKey + "_gender", "");
        int height = prefs.getInt(userIdKey + "_height", 0);
        int weight = prefs.getInt(userIdKey + "_weight", 0);
        String vong1 = prefs.getString(userIdKey + "_vong1", "");
        String vong2 = prefs.getString(userIdKey + "_vong2", "");
        String vong3 = prefs.getString(userIdKey + "_vong3", "");
        String styles = prefs.getString(userIdKey + "_styles", "");
        int skin = prefs.getInt(userIdKey + "_skin", 0);

        boolean hasAtLeastOneField = (!gender.isEmpty() && !gender.equalsIgnoreCase("other"))
                || height > 0 
                || weight > 0 
                || !vong1.isEmpty() 
                || !vong2.isEmpty() 
                || !vong3.isEmpty() 
                || !styles.isEmpty() 
                || skin > 0;

        if (sessionManager.isLoggedIn()) {
            if (hasAtLeastOneField) {
                sessionManager.saveProfileCompleted(true);
                return false;
            } else {
                sessionManager.saveProfileCompleted(false);
                return true;
            }
        }

        return !hasAtLeastOneField;
    }

    private void setupSpinnersFromPrefs() {
        if (getContext() == null) return;
        
        boolean isLogged = sessionManager.isLoggedIn();
        boolean completed = sessionManager.isProfileCompleted();
        
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("AI_PREFS", android.content.Context.MODE_PRIVATE);
        String userIdKey = getCurrentUserId();
        
        String savedStyle = prefs.getString(userIdKey + "_spinner_favorite_style", null);
        String savedColor = prefs.getString(userIdKey + "_spinner_color_palette", null);
        String savedVibe = prefs.getString(userIdKey + "_spinner_style_vibe", null);
        String savedShape = prefs.getString(userIdKey + "_spinner_body_shape", null);

        populateSpinnerWithSavedOption(binding.spFavoriteStyle, STYLES, savedStyle, "spinner_favorite_style");
        populateSpinnerWithSavedOption(binding.spColorPalette, COLORS, savedColor, "spinner_color_palette");
        populateSpinnerWithSavedOption(binding.spStyleVibe, PURPOSES, savedVibe, "spinner_style_vibe");
        populateBodyShapeSpinnerFromPrefs(binding.spBodyShape, savedShape);
    }

    private void populateSpinnerWithSavedOption(Spinner spinner, String[] options, String savedValue, String prefKey) {
        List<String> list = new ArrayList<>();
        int selectedIndex = 0;

        for (int i = 0; i < options.length; i++) {
            list.add(options[i]);
            if (savedValue != null && savedValue.equalsIgnoreCase(options[i])) {
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

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (getContext() != null) {
                    android.content.SharedPreferences prefs = requireContext().getSharedPreferences("AI_PREFS", android.content.Context.MODE_PRIVATE);
                    prefs.edit().putString(getCurrentUserId() + "_" + prefKey, options[position]).apply();
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void populateBodyShapeSpinnerFromPrefs(Spinner spinner, String savedShape) {
        List<String> list = new ArrayList<>();
        int selectedIndex = 0;

        list.add("Chưa có");

        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("AI_PREFS", android.content.Context.MODE_PRIVATE);
        String userIdKey = getCurrentUserId();
        int height = prefs.getInt(userIdKey + "_height", 0);
        int weight = prefs.getInt(userIdKey + "_weight", 0);

        if (height > 0 && weight > 0) {
            String bmiSpec = "Chiều cao: " + height + " cm | Cân nặng: " + weight + " kg";
            list.add(bmiSpec);

            float heightM = height / 100f;
            float bmi = weight / (heightM * heightM);
            String bmiShape = "Dáng người: Cân đối";
            if (bmi < 18.5f) bmiShape = "Dáng người: Mảnh mai";
            else if (bmi >= 25f && bmi < 30f) bmiShape = "Dáng người: Đầy đặn";
            else if (bmi >= 30f) bmiShape = "Dáng người: Tròn trịa";
            list.add(bmiShape);
        }

        for (String shape : SHAPES) {
            list.add(shape);
        }

        if (savedShape != null) {
            for (int i = 0; i < list.size(); i++) {
                if (savedShape.equalsIgnoreCase(list.get(i))) {
                    selectedIndex = i;
                    break;
                }
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

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (getContext() != null) {
                    android.content.SharedPreferences prefs = requireContext().getSharedPreferences("AI_PREFS", android.content.Context.MODE_PRIVATE);
                    prefs.edit().putString(getCurrentUserId() + "_spinner_body_shape", list.get(position)).apply();
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void updateProfileStatusUI() {
        if (binding == null) return;
        boolean isEmpty = isAIProfileEmpty();
        if (isEmpty) {
            binding.tvUserLabel.setText("Chưa có Profile");
            binding.btnEditProfile.setText("THIẾT LẬP");
        } else {
            binding.tvUserLabel.setText("Đã có Profile");
            binding.btnEditProfile.setText("CHỈNH SỬA");
        }
    }

    private void loadCachedFallback() {
        binding.tvWelcomeTitle.setText("Xin chào, " + sessionManager.getUserName() + " ✨");
        binding.tvUserName.setText(sessionManager.getUserName());
        binding.tvUserLabel.setText(sessionManager.isProfileCompleted() ? "AI Profile Đã Hoàn Thành" : "AI Profile Chưa Hoàn Thành");
        binding.ivUserAvatar.setImageResource(R.drawable.ic_profile_vector);

        setupSpinnersFromPrefs();
        updateProfileStatusUI();
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
