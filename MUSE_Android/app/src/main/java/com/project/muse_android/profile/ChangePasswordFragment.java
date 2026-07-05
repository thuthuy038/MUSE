package com.project.muse_android.profile;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
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
import com.project.muse_android.databinding.FragmentChangePasswordBinding;
import com.project.muse_android.dialog.SuccessDialog;
import com.project.utils.SessionManager;
import com.project.network.ApiClient;
import com.project.models.User;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordFragment extends Fragment {

    private FragmentChangePasswordBinding binding;
    private SessionManager sessionManager;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

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
            binding.ivAvatar.setImageResource(R.drawable.ic_account_circle);
            return;
        }

        if (avatar.startsWith("http") || avatar.startsWith("/")) {
            String fullUrl = avatar;
            if (avatar.startsWith("/")) {
                fullUrl = "https://server-testing-ymn9.onrender.com" + avatar;
            }
            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_account_circle)
                    .error(R.drawable.ic_account_circle)
                    .into(binding.ivAvatar);
        } else if (avatar.length() > 200 || !avatar.contains("/")) {
            // Likely Base64 or a weird relative path without slash
            try {
                byte[] decodedString = Base64.decode(avatar, Base64.DEFAULT);
                Glide.with(this)
                        .load(decodedString)
                        .placeholder(R.drawable.ic_account_circle)
                        .error(R.drawable.ic_account_circle)
                        .into(binding.ivAvatar);
            } catch (Exception e) {
                // If Base64 fails, try treating it as relative path without leading slash
                String fullUrl = "https://server-testing-ymn9.onrender.com/" + avatar;
                Glide.with(this)
                        .load(fullUrl)
                        .placeholder(R.drawable.ic_account_circle)
                        .error(R.drawable.ic_account_circle)
                        .into(binding.ivAvatar);
            }
        } else {
            // Likely relative path without leading slash
            String fullUrl = "https://server-testing-ymn9.onrender.com/" + avatar;
            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_account_circle)
                    .error(R.drawable.ic_account_circle)
                    .into(binding.ivAvatar);
        }
    }

    private void setupClickListeners() {
        binding.ivBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        binding.ivShowNewPassword.setOnClickListener(v -> {
            isNewPasswordVisible = !isNewPasswordVisible;
            if (isNewPasswordVisible) {
                binding.etNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                binding.etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            binding.etNewPassword.setSelection(binding.etNewPassword.getText().length());
        });

        binding.ivShowConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            if (isConfirmPasswordVisible) {
                binding.etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                binding.etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.getText().length());
        });

        binding.btnConfirm.setOnClickListener(v -> validateAndChangePassword());
    }

    private void validateAndChangePassword() {
        String newPassword = binding.etNewPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (newPassword.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập mật khẩu mới", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 8) {
            Toast.makeText(getContext(), "Mật khẩu phải có ít nhất 8 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Mật khẩu nhập lại không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        changePassword(newPassword);
    }

    private void changePassword(String newPassword) {
        String userId = sessionManager.getUserId();
        String token = "Bearer " + sessionManager.getToken();

        if (userId == null) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("password", newPassword);

        binding.btnConfirm.setEnabled(false);
        binding.btnConfirm.setText("Đang xử lý...");

        ApiClient.INSTANCE.getInstance().updateUser(userId, token, updateData).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!isAdded()) return;
                binding.btnConfirm.setEnabled(true);
                binding.btnConfirm.setText("Xác nhận");

                if (response.isSuccessful()) {
                    showSuccessDialog();
                } else {
                    Toast.makeText(getContext(), "Đổi mật khẩu thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                if (!isAdded()) return;
                binding.btnConfirm.setEnabled(true);
                binding.btnConfirm.setText("Xác nhận");
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuccessDialog() {
        SuccessDialog dialog = SuccessDialog.newInstance("Đổi mật khẩu thành công!");
        dialog.setOnCloseListener(() -> {
            if (getView() != null) {
                Navigation.findNavController(getView()).popBackStack();
            }
        });
        dialog.show(getParentFragmentManager(), "success_dialog");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
