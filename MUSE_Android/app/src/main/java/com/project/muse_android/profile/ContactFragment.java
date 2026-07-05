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
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentContactBinding;
import com.project.utils.SessionManager;
import com.project.network.ApiClient;
import com.project.models.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
