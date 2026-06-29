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
import com.project.muse_android.databinding.ActivityVerifyOtpBinding;

public class VerifyOtpFragment extends Fragment {

    private ActivityVerifyOtpBinding binding;
    private String emailPhone;
    private String serverOtp;

    public static VerifyOtpFragment newInstance(String emailPhone, String serverOtp) {
        VerifyOtpFragment fragment = new VerifyOtpFragment();
        Bundle args = new Bundle();
        args.putString("emailPhone", emailPhone);
        args.putString("serverOtp", serverOtp);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            emailPhone = getArguments().getString("emailPhone");
            serverOtp = getArguments().getString("serverOtp");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivityVerifyOtpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Pre-fill OTP and alert user to simplify testing
        if (serverOtp != null && !serverOtp.isEmpty()) {
            binding.etOtp.setText(serverOtp);
            Toast.makeText(getContext(), "Mã OTP (Test): " + serverOtp, Toast.LENGTH_LONG).show();
        }

        binding.btnVerify.setOnClickListener(v -> {
            String otp = binding.etOtp.getText().toString().trim();
            if (otp.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập mã xác nhận", Toast.LENGTH_SHORT).show();
            } else {
                ResetPasswordFragment resetFragment = ResetPasswordFragment.newInstance(emailPhone, otp);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.auth_container, resetFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
