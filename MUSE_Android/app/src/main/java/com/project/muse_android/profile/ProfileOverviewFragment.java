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
import androidx.navigation.Navigation;

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
        setAvatarImage(cachedAvatar);

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

                        // Fetch orders and calculate spending, points, order count, rank dynamically
                        fetchOrdersAndCalculate(user);

                        // Cache updated name & email in session
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
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void fetchOrdersAndCalculate(User user) {
        String userId = user.get_id();
        if (userId == null) return;

        ApiClient.INSTANCE.getInstance().getMyOrders(userId).enqueue(new Callback<java.util.List<com.project.models.Order>>() {
            @Override
            public void onResponse(Call<java.util.List<com.project.models.Order>> call, Response<java.util.List<com.project.models.Order>> response) {
                if (!isAdded()) return;

                double totalSpending = 0;
                int ordersCount = 0;

                if (response.isSuccessful() && response.body() != null) {
                    ordersCount = response.body().size();
                    for (com.project.models.Order order : response.body()) {
                        // Chỉ tính các đơn hàng đã hoàn thành/đã giao (không tính các đơn đang chờ xác nhận, đang giao, đã hủy)
                        String status = order.getStatus();
                        if ("COMPLETED".equalsIgnoreCase(status) || 
                            "DELIVERED".equalsIgnoreCase(status) || 
                            "Đã giao".equalsIgnoreCase(status) ||
                            "Đã hoàn thành".equalsIgnoreCase(status) ||
                            "Hoàn thành".equalsIgnoreCase(status)) {
                            totalSpending += order.getFinalPrice();
                        }
                    }
                }

                // Calculate points
                int points = user.getPoints() > 0 ? user.getPoints() : (int) (totalSpending / 1000);

                // Calculate membership rank
                String rank;
                if (totalSpending >= 10000000) {
                    rank = "DIAMOND";
                } else if (totalSpending >= 5000000) {
                    rank = "GOLDEN";
                } else if (totalSpending >= 1000000) {
                    rank = "SILVER";
                } else {
                    rank = "PINK";
                }

                // Update UI
                binding.tvOrdersCount.setText(String.valueOf(ordersCount));
                binding.tvPointsCount.setText(formatPoints(points));
                binding.tvMembershipRank.setText("THÀNH VIÊN " + rank);
            }

            @Override
            public void onFailure(Call<java.util.List<com.project.models.Order>> call, Throwable t) {
                if (!isAdded()) return;
                // Fallback to profile defaults
                binding.tvOrdersCount.setText(String.valueOf(user.getOrderCount()));
                binding.tvPointsCount.setText(formatPoints(user.getPoints()));
                binding.tvMembershipRank.setText("THÀNH VIÊN PINK");
            }
        });
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
        binding.menuFavorites.ivIcon.setImageResource(R.drawable.ic_favorite_filled);
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
            Navigation.findNavController(v).navigate(R.id.navigation_edit_profile);
        });

        binding.menuOrders.getRoot().setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.project.muse_android.order.OrderActivity.class);
            startActivity(intent);
        });

        binding.menuSecurity.getRoot().setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_settings);
        });

        binding.menuMembership.getRoot().setOnClickListener(v -> openMembership(v));

        binding.menuFavorites.getRoot().setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_wishlist);
        });

        binding.llMembershipBadge.setOnClickListener(v -> openMembership(v));

        binding.ivAvatar.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_edit_profile);
        });

        binding.btnEditAvatar.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_edit_profile);
        });

        binding.tvUserName.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_edit_profile);
        });

        binding.cardAiStylist.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_ai);
        });

        binding.cardArRoom.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), com.project.muse_android.ai.VirtualFittingActivity.class);
            intent.putExtra("outfit_scan", true);
            startActivity(intent);
        });
    }

    private void openMembership(View v) {
        Navigation.findNavController(v).navigate(R.id.navigation_membership);
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
