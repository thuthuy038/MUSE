package com.project.muse_android.auth;

import android.content.Intent;
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
import com.project.muse_android.databinding.LoginScreenBinding;
import com.project.muse_android.profile.ProfileActivity;
import com.project.muse_android.dialog.SuccessDialog;
import com.project.network.ApiClient;
import com.project.models.LoginRequest;
import com.project.models.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private LoginScreenBinding binding;
    private boolean isPasswordVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = LoginScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.ivShowPassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                binding.etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                isPasswordVisible = false;
            } else {
                binding.etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                isPasswordVisible = true;
            }
            binding.etPassword.setSelection(binding.etPassword.getText().length());
        });

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString();
            String password = binding.etPassword.getText().toString();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.auth_container, new ForgotPasswordFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.tvRegister.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.auth_container, new RegisterFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void loginUser(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        ApiClient.INSTANCE.getInstance().login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    com.project.utils.SessionManager sessionManager = new com.project.utils.SessionManager(requireContext());
                    sessionManager.saveToken(loginResponse.getToken());
                    sessionManager.saveUser(loginResponse.get_id(), loginResponse.getName(), loginResponse.getEmail());

                    SuccessDialog dialog = SuccessDialog.newInstance("Đăng nhập thành công!");
                    dialog.setOnCloseListener(() -> {
                        Intent intent = new Intent(getActivity(), ProfileActivity.class);
                        startActivity(intent);
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    });
                    dialog.show(getParentFragmentManager(), "SuccessDialog");
                } else {
                    Toast.makeText(getContext(), "Đăng nhập thất bại: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
