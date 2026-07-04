package com.project.muse_android.profile;

import android.content.Intent;
import android.os.Bundle;
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
import com.project.muse_android.databinding.FragmentProfileOverviewBinding;
import com.project.muse_android.dialog.LogoutDialog;
import com.project.muse_android.auth.AuthActivity;
import com.project.utils.SessionManager;
import com.project.network.ApiClient;
import com.project.models.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileOverviewFragment extends Fragment {

    private FragmentProfileOverviewBinding binding;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileOverviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        setupClickListeners();
        setupMenuItems();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void loadUserProfile() {
        String token = sessionManager.getToken();
        String cachedName = sessionManager.getUserName();

        // Show cached name immediately for better UX
        if (cachedName != null) {
            binding.tvUserName.setText(cachedName);
        }

        // Load cached avatar immediately for better UX
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
                    e.printStackTrace();
                    binding.ivAvatar.setImageResource(R.drawable.ic_account_circle);
                }
            }
        } else {
            binding.ivAvatar.setImageResource(R.drawable.ic_account_circle);
        }

        // Fetch fresh user profile from API
        if (token != null) {
            ApiClient.INSTANCE.getInstance().getProfile("Bearer " + token).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null) {
                        User user = response.body();
                        // Update UI with fresh data
                        binding.tvUserName.setText(user.getName());
                        binding.tvOrdersCount.setText(String.valueOf(user.getOrderCount()));
                        binding.tvPointsCount.setText(formatPoints(user.getPoints()));

                        // Cache updated name & email in session
                        sessionManager.saveUser(user.get_id(), user.getName(), user.getEmail());

                        if (user.getAvatar() != null && user.getAvatar().getUrl() != null && !user.getAvatar().getUrl().isEmpty()) {
                            String avatarUrl = user.getAvatar().getUrl();
                            if (avatarUrl.startsWith("http") || avatarUrl.startsWith("/")) {
                                String fullUrl = avatarUrl;
                                if (avatarUrl.startsWith("/")) {
                                    fullUrl = "https://server-testing-ymn9.onrender.com" + avatarUrl;
                                }
                                Glide.with(ProfileOverviewFragment.this).load(fullUrl).into(binding.ivAvatar);
                                sessionManager.saveAvatar(user.get_id(), fullUrl);
                            } else {
                                try {
                                    byte[] decodedString = Base64.decode(avatarUrl, Base64.DEFAULT);
                                    Glide.with(ProfileOverviewFragment.this).load(decodedString).into(binding.ivAvatar);
                                    sessionManager.saveAvatar(user.get_id(), avatarUrl);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    binding.ivAvatar.setImageResource(R.drawable.ic_account_circle);
                                }
                            }
                        } else {
                            if (sessionManager.getAvatar(user.get_id()) == null) {
                                binding.ivAvatar.setImageResource(R.drawable.ic_account_circle);
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), "Không thể tải thông tin mới nhất", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupMenuItems() {
        // Personal Info
        binding.menuPersonalInfo.ivIcon.setImageResource(R.drawable.ic_account_circle);
        binding.menuPersonalInfo.tvTitle.setText(R.string.profile_personal_info);

        // Security
        binding.menuSecurity.ivIcon.setImageResource(R.drawable.ic_lock);
        binding.menuSecurity.tvTitle.setText(R.string.profile_setting);

        // Notifications
        binding.menuNotifications.ivIcon.setImageResource(R.drawable.ic_notifications);
        binding.menuNotifications.tvTitle.setText(R.string.profile_notifications);

        // Membership
        binding.menuMembership.ivIcon.setImageResource(R.drawable.ic_star);
        binding.menuMembership.tvTitle.setText(R.string.profile_membership);

        // Orders
        binding.menuOrders.ivIcon.setImageResource(R.drawable.ic_shopping_bag);
        binding.menuOrders.tvTitle.setText(R.string.profile_my_orders);

        // Favorites
        binding.menuFavorites.ivIcon.setImageResource(R.drawable.ic_favorite);
        binding.menuFavorites.tvTitle.setText(R.string.profile_favorites);
    }

    private void setupClickListeners() {
        binding.btnLogout.setOnClickListener(v -> {
            LogoutDialog dialog = new LogoutDialog();
            dialog.setOnLogoutListener(() -> {
                // Clear preferences/session
                sessionManager.clearSession();
                Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), AuthActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
            dialog.show(getParentFragmentManager(), "logout_dialog");
        });

        binding.ivCart.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Giỏ hàng", Toast.LENGTH_SHORT).show();
        });

        binding.menuPersonalInfo.getRoot().setOnClickListener(v -> {
            int containerId = R.id.profile_container;
            if (getView() != null && getView().getParent() instanceof View) {
                containerId = ((View) getView().getParent()).getId();
            }
            getParentFragmentManager().beginTransaction()
                    .replace(containerId, new EditProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.menuSecurity.getRoot().setOnClickListener(v -> {
            int containerId = R.id.profile_container;
            if (getView() != null && getView().getParent() instanceof View) {
                containerId = ((View) getView().getParent()).getId();
            }
            getParentFragmentManager().beginTransaction()
                    .replace(containerId, new SettingsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.menuMembership.getRoot().setOnClickListener(v -> openMembership());

        binding.ivAvatar.setOnClickListener(v -> openMembership());
        binding.tvUserName.setOnClickListener(v -> openMembership());
    }

    private void openMembership() {
        int containerId = R.id.profile_container;
        if (getView() != null && getView().getParent() instanceof View) {
            containerId = ((View) getView().getParent()).getId();
        }
        getParentFragmentManager().beginTransaction()
                .replace(containerId, new com.project.muse_android.membership.MembershipFragment())
                .addToBackStack(null)
                .commit();
    }

    private String formatPoints(int points) {
        if (points >= 1000) {
            return String.format(java.util.Locale.US, "%.1fk", points / 1000.0);
        }
        return String.valueOf(points);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
