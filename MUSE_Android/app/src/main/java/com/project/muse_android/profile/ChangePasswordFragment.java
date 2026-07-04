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

        loadUserProfile();
        setupClickListeners();
    }

    private void loadUserProfile() {
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

    private void setupClickListeners() {
        binding.ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

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
                binding.btnConfirm.setEnabled(true);
                binding.btnConfirm.setText("Xác nhận");
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuccessDialog() {
        SuccessDialog dialog = SuccessDialog.newInstance("Đổi mật khẩu thành công!");
        dialog.setOnCloseListener(() -> getParentFragmentManager().popBackStack());
        dialog.show(getParentFragmentManager(), "success_dialog");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
