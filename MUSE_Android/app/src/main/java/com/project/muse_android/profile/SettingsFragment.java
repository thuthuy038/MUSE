package com.project.muse_android.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentSettingsBinding;
import com.project.muse_android.dialog.LogoutDialog;
import com.project.muse_android.auth.AuthActivity;
import com.project.utils.SessionManager;
import com.project.network.ApiClient;
import com.project.models.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        // Fix header overlapping status bar
        com.project.utils.ViewUtils.applySystemBarsPadding(binding.header, true, false);

        // Fix content overlapping navigation bar
        com.project.utils.ViewUtils.applySystemBarsPadding(binding.nestedScrollView, false, true);

        setupMenuItems();
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void loadUserProfile() {
        String token = sessionManager.getToken();
        String cachedName = sessionManager.getUserName();
        String cachedEmail = sessionManager.getUserEmail();

        if (cachedName != null) binding.tvUserName.setText(cachedName);
        if (cachedEmail != null) binding.tvUserEmail.setText(cachedEmail);

        // Load cached avatar immediately for better UX
        String cachedAvatar = sessionManager.getAvatar(sessionManager.getUserId());
        setAvatarImage(cachedAvatar);

        // Fetch fresh profile from API
        if (token != null) {
            ApiClient.INSTANCE.getInstance().getProfile("Bearer " + token).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null) {
                        User user = response.body();
                        binding.tvUserName.setText(user.getName());
                        binding.tvUserEmail.setText(user.getEmail());

                        // Cache name and email
                        sessionManager.saveUser(user.get_id(), user.getName(), user.getEmail());

                        if (user.getAvatar() != null && user.getAvatar().getUrl() != null && !user.getAvatar().getUrl().isEmpty()) {
                            String avatarUrl = user.getAvatar().getUrl();
                            setAvatarImage(avatarUrl);
                            
                            // Save to cache (handle relative path)
                            String fullCacheUrl = avatarUrl;
                            if (!avatarUrl.startsWith("http")) {
                                if (avatarUrl.startsWith("/")) {
                                    fullCacheUrl = "https://server-testing-ymn9.onrender.com" + avatarUrl;
                                } else {
                                    fullCacheUrl = "https://server-testing-ymn9.onrender.com/" + avatarUrl;
                                }
                            }
                            sessionManager.saveAvatar(user.get_id(), fullCacheUrl);
                        } else {
                            // Try local cache if server doesn't have it
                            String cached = sessionManager.getAvatar(user.get_id());
                            setAvatarImage(cached);
                        }
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    // Fail silently
                }
            });
        }
    }

    private void setAvatarImage(String avatar) {
        if (avatar == null || avatar.isEmpty()) {
            binding.ivAvatar.setImageResource(R.drawable.ic_profile_vector);
            return;
        }

        if (avatar.startsWith("http") || avatar.startsWith("/")) {
            String fullUrl = avatar;
            if (avatar.startsWith("/")) {
                fullUrl = "https://server-testing-ymn9.onrender.com" + avatar;
            }
            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_profile_vector)
                    .error(R.drawable.ic_profile_vector)
                    .into(binding.ivAvatar);
        } else if (avatar.length() > 200 || !avatar.contains("/")) {
            // Likely Base64 or a weird relative path without slash
            try {
                byte[] decodedString = Base64.decode(avatar, Base64.DEFAULT);
                Glide.with(this)
                        .load(decodedString)
                        .placeholder(R.drawable.ic_profile_vector)
                        .error(R.drawable.ic_profile_vector)
                        .into(binding.ivAvatar);
            } catch (Exception e) {
                // If Base64 fails, try treating it as relative path without leading slash
                String fullUrl = "https://server-testing-ymn9.onrender.com/" + avatar;
                Glide.with(this)
                        .load(fullUrl)
                        .placeholder(R.drawable.ic_profile_vector)
                        .error(R.drawable.ic_profile_vector)
                        .into(binding.ivAvatar);
            }
        } else {
            // Likely relative path without leading slash
            String fullUrl = "https://server-testing-ymn9.onrender.com/" + avatar;
            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_profile_vector)
                    .error(R.drawable.ic_profile_vector)
                    .into(binding.ivAvatar);
        }
    }

    private void setupMenuItems() {
        // Section: Bảo mật
        binding.itemChangePassword.ivIcon.setImageResource(R.drawable.ic_lock);
        binding.itemChangePassword.tvTitle.setText("Đổi mật khẩu");

        // Section: Hỗ trợ
        binding.itemHelpCenter.ivIcon.setImageResource(R.drawable.ic_help_outline);
        binding.itemHelpCenter.tvTitle.setText("Trung tâm hỗ trợ");

        binding.itemContact.ivIcon.setImageResource(R.drawable.ic_help_outline);
        binding.itemContact.tvTitle.setText("Liên hệ");

        binding.itemFeedback.ivIcon.setImageResource(R.drawable.ic_help_outline);
        binding.itemFeedback.tvTitle.setText("Feedback");

        // Section: Thông tin ứng dụng
        binding.itemVersion.ivIcon.setImageResource(R.drawable.ic_info);
        binding.itemVersion.tvTitle.setText("Version");
        binding.itemVersion.tvValue.setVisibility(View.VISIBLE);
        binding.itemVersion.tvValue.setText("1.1.1");
        binding.itemVersion.ivChevron.setVisibility(View.GONE);

        binding.itemPrivacyPolicy.ivIcon.setImageResource(R.drawable.ic_gavel);
        binding.itemPrivacyPolicy.tvTitle.setText("Chính sách bảo mật & pháp lý");
    }

    private void setupClickListeners() {
        binding.ivBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        binding.btnLogout.setOnClickListener(v -> {
            LogoutDialog dialog = new LogoutDialog();
            dialog.setOnLogoutListener(() -> {
                sessionManager.clearSession();
                Intent intent = new Intent(getActivity(), AuthActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) getActivity().finish();
            });
            dialog.show(getParentFragmentManager(), "logout_dialog");
        });

        binding.itemChangePassword.getRoot().setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_change_password);
        });

        binding.itemContact.getRoot().setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_contact);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
