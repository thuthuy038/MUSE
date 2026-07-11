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
import com.project.muse_android.databinding.RegisterScreenBinding;
import com.project.muse_android.dialog.SuccessDialog;
import com.project.network.ApiClient;
import com.project.models.RegisterRequest;
import com.project.models.RegisterResponse;

import android.app.Activity;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.project.models.GoogleLoginRequest;
import com.project.models.LoginResponse;
import com.project.muse_android.profile.ProfileActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterFragment extends Fragment {

    private RegisterScreenBinding binding;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = RegisterScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Fix header overlapping status bar
        com.project.utils.ViewUtils.applySystemBarsPadding(binding.getRoot(), true, false);

        // Khởi tạo Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("626423207611-nql9ucopgltr5r7l4sbrqt1fc6ig0eop.apps.googleusercontent.com")
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
                        Toast.makeText(getContext(), "Đăng nhập Google bị hủy", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        binding.btnGoogleRegister.setOnClickListener(v -> {
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        });

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

        binding.ivShowConfirmPassword.setOnClickListener(v -> {
            if (isConfirmPasswordVisible) {
                binding.etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                isConfirmPasswordVisible = false;
            } else {
                binding.etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                isConfirmPasswordVisible = true;
            }
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.getText().length());
        });

        binding.btnRegister.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else if (!password.equals(confirmPassword)) {
                Toast.makeText(getContext(), "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            } else if (!password.matches("^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$")) {
                Toast.makeText(getContext(), "Mật khẩu phải có ký tự in hoa và ký tự đặc biệt, dài tối thiểu 8 ký tự", Toast.LENGTH_SHORT).show();
            } else {
                registerUser(name, email, password);
            }
        });

        binding.tvLogin.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void registerUser(String name, String email, String password) {
        RegisterRequest request = new RegisterRequest(name, email, password, "customer");
        ApiClient.INSTANCE.getInstance().register(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    SuccessDialog dialog = SuccessDialog.newInstance("Đăng ký thành công!");
                    if (getContext() != null) {
                        com.project.utils.SessionManager sm = new com.project.utils.SessionManager(getContext());
                        sm.saveNewRegisterFlag(true);
                    }
                    dialog.setOnCloseListener(() -> {
                        getParentFragmentManager().popBackStack();
                    });
                    dialog.show(getParentFragmentManager(), "SuccessDialog");
                } else {
                    Toast.makeText(getContext(), "Đăng ký thất bại: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String idToken = account.getIdToken();
            if (idToken != null) {
                loginWithGoogle(idToken);
            } else {
                Toast.makeText(getContext(), "Không lấy được Token từ Google", Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            Toast.makeText(getContext(), "Lỗi Google: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loginWithGoogle(String idToken) {
        GoogleLoginRequest request = new GoogleLoginRequest(idToken);
        ApiClient.INSTANCE.getInstance().googleLogin(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    com.project.utils.SessionManager sessionManager = new com.project.utils.SessionManager(requireContext());
                    sessionManager.saveToken(loginResponse.getToken());
                    sessionManager.saveUser(loginResponse.get_id(), loginResponse.getName(), loginResponse.getEmail());

                    SuccessDialog dialog = SuccessDialog.newInstance("Đăng nhập bằng Google thành công!");
                    dialog.setOnCloseListener(() -> {
                        Intent intent = new Intent(getActivity(), com.project.muse_android.main.MainActivity.class);
                        intent.putExtra("select_profile", true);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    });
                    dialog.show(getParentFragmentManager(), "SuccessDialog");
                } else {
                    Toast.makeText(getContext(), "Đăng nhập Google thất bại: " + response.message(), Toast.LENGTH_SHORT).show();
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
