package com.project.muse_android.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityWelcomeScreenBinding;
import com.project.muse_android.policy.TermsOfUseActivity;
import com.project.utils.SessionManager;

public class WelcomeFragment extends Fragment {

    private ActivityWelcomeScreenBinding binding;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivityWelcomeScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        binding.btnStart.setOnClickListener(v -> {
            sessionManager.setFirstLaunch(false);
            Intent intent = new Intent(getActivity(), TermsOfUseActivity.class);
            startActivity(intent);
        });

        binding.btnSignIn.setOnClickListener(v -> {
            sessionManager.setFirstLaunch(false);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.auth_container, new LoginFragment())
                    .addToBackStack("LoginFragment")
                    .commit();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
