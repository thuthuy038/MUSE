package com.project.muse_android.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityForgotPasswordBinding;
import com.project.network.ApiClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordFragment extends Fragment {

    private ActivityForgotPasswordBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivityForgotPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnConfirm.setOnClickListener(v -> {
            String emailPhone = binding.etEmailPhone.getText().toString().trim();
            if (emailPhone.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập email hoặc số điện thoại", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.btnConfirm.setEnabled(false);
            Toast.makeText(getContext(), "Đang gửi mã OTP...", Toast.LENGTH_SHORT).show();

            Map<String, String> body = new HashMap<>();
            body.put("email", emailPhone);
            body.put("emailOrPhone", emailPhone);

            ApiClient.INSTANCE.getInstance().sendOtp(body).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (!isAdded()) return;
                    binding.btnConfirm.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, String> data = response.body();
                        String otp = data.get("otp");
                        Toast.makeText(getContext(), "Mã OTP đã được gửi thành công!", Toast.LENGTH_SHORT).show();

                        VerifyOtpFragment verifyFragment = VerifyOtpFragment.newInstance(emailPhone, otp != null ? otp : "");
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.auth_container, verifyFragment)
                                .addToBackStack(null)
                                .commit();
                    } else {
                        Toast.makeText(getContext(), "Gửi OTP thất bại hoặc Email không tồn tại", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    if (!isAdded()) return;
                    binding.btnConfirm.setEnabled(true);
                    Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
