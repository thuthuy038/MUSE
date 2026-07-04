package com.project.muse_android.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentContactBinding;
import com.project.utils.SessionManager;

public class ContactFragment extends Fragment {

    private FragmentContactBinding binding;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentContactBinding.inflate(inflater, container, false);
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

        binding.btnFacebook.setOnClickListener(v -> {
            String facebookUrl = "https://www.facebook.com/muse.inc";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl));
            startActivity(intent);
        });

        binding.btnMessenger.setOnClickListener(v -> {
            String messengerUrl = "https://m.me/muse.inc";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(messengerUrl));
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
