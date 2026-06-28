package com.project.muse_android.auth;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityResetPasswordBinding;
import com.project.muse_android.dialog.SuccessDialog;
import com.project.network.ApiClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordFragment extends Fragment {

    private ActivityResetPasswordBinding binding;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private String emailPhone;
    private String otp;

    public static ResetPasswordFragment newInstance(String emailPhone, String otp) {
        ResetPasswordFragment fragment = new ResetPasswordFragment();
        Bundle args = new Bundle();
        args.putString("emailPhone", emailPhone);
        args.putString("otp", otp);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            emailPhone = getArguments().getString("emailPhone");
            otp = getArguments().getString("otp");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivityResetPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.ivShowNewPassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                binding.etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                isPasswordVisible = false;
            } else {
                binding.etNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                isPasswordVisible = true;
            }
            binding.etNewPassword.setSelection(binding.etNewPassword.getText().length());
        });

        binding.ivShowConfirmNewPassword.setOnClickListener(v -> {
            if (isConfirmPasswordVisible) {
                binding.etConfirmNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                isConfirmPasswordVisible = false;
            } else {
                binding.etConfirmNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                isConfirmPasswordVisible = true;
            }
            binding.etConfirmNewPassword.setSelection(binding.etConfirmNewPassword.getText().length());
        });

        binding.btnReset.setOnClickListener(v -> {
            String newPassword = binding.etNewPassword.getText().toString();
            String confirmPassword = binding.etConfirmNewPassword.getText().toString();

            if (newPassword.length() < 8) {
                Toast.makeText(getContext(), "Mật khẩu tối thiểu 8 kí tự", Toast.LENGTH_SHORT).show();
            } else if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(getContext(), "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            } else {
                binding.btnReset.setEnabled(false);
                Toast.makeText(getContext(), "Đang đặt lại mật khẩu...", Toast.LENGTH_SHORT).show();

                Map<String, String> body = new HashMap<>();
                body.put("email", emailPhone);
                body.put("emailOrPhone", emailPhone);
                body.put("otp", otp);
                body.put("password", newPassword);
                body.put("confirmPassword", newPassword);
                body.put("newPassword", newPassword);
                body.put("confirmNewPassword", newPassword);

                ApiClient.INSTANCE.getInstance().resetPassword(body).enqueue(new Callback<Map<String, String>>() {
                    @Override
                    public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                        if (!isAdded()) return;
                        binding.btnReset.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null) {
                            SuccessDialog dialog = SuccessDialog.newInstance("Đổi mật khẩu thành công!");
                            dialog.setOnCloseListener(() -> {
                                getParentFragmentManager().popBackStack("LoginFragment", 0);
                            });
                            dialog.show(getParentFragmentManager(), "success_dialog");
                        } else {
                            String errMsg = "Đổi mật khẩu thất bại";
                            try {
                                if (response.errorBody() != null) {
                                    String errJson = response.errorBody().string();
                                    if (errJson.contains("message")) {
                                        errMsg = new org.json.JSONObject(errJson).getString("message");
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(getContext(), errMsg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, String>> call, Throwable t) {
                        if (!isAdded()) return;
                        binding.btnReset.setEnabled(true);
                        Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
