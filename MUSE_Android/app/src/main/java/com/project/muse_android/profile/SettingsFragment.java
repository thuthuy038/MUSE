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

        setupMenuItems();
        setupClickListeners();
        loadUserProfile();
    }

    private void loadUserProfile() {
        String token = sessionManager.getToken();
        String cachedName = sessionManager.getUserName();
        String cachedEmail = sessionManager.getUserEmail();

        if (cachedName != null) binding.tvUserName.setText(cachedName);
        if (cachedEmail != null) binding.tvUserEmail.setText(cachedEmail);

        String cachedAvatar = sessionManager.getAvatar(sessionManager.getUserId());
        if (cachedAvatar != null) {
            if (cachedAvatar.startsWith("http") || cachedAvatar.startsWith("/")) {
                String fullUrl = cachedAvatar;
                if (cachedAvatar.startsWith("/")) {
                    fullUrl = "https://server-testing-ymn9.onrender.com" + cachedAvatar;
                }
                Glide.with(this).load(fullUrl).into(binding.ivAvatar);
            } else {
                try {
                    byte[] decodedString = Base64.decode(cachedAvatar, Base64.DEFAULT);
                    Glide.with(this).load(decodedString).into(binding.ivAvatar);
                } catch (Exception e) {
                    binding.ivAvatar.setImageResource(R.drawable.ic_account_circle);
                }
            }
        }
    }

    private void setupMenuItems() {
        // Section: Bảo mật
        binding.itemTwoFactor.ivIcon.setImageResource(R.drawable.ic_verified_user);
        binding.itemTwoFactor.tvTitle.setText("Xác thực hai lớp");

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
        binding.ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

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
            // Navigate to change password or forgot password flow
            Toast.makeText(getContext(), "Đổi mật khẩu", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}