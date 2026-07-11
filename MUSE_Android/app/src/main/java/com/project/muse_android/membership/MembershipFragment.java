package com.project.muse_android.membership;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentMembershipBinding;
import com.project.utils.SessionManager;
import com.project.network.ApiClient;
import com.project.network.ApiResponse;
import com.project.models.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MembershipFragment extends Fragment {

    private FragmentMembershipBinding binding;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMembershipBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        // Fix header overlapping status bar
        com.project.utils.ViewUtils.applySystemBarsPadding(binding.header, true, false);

        // Fix content overlapping navigation bar
        com.project.utils.ViewUtils.applySystemBarsPadding(binding.nestedScrollView, false, true);

        binding.ivBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        binding.ivNotification.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_notification);
        });

        loadMembershipData();
        setupBenefitStaticUI();
    }

    private void setupBenefitStaticUI() {
        // Free Shipping
        binding.benefitFreeShipping.ivIcon.setImageResource(R.drawable.ic_local_shipping);
        binding.benefitFreeShipping.tvTitle.setText("Free Shipping");
        binding.benefitFreeShipping.tvDesc.setText("Không giới hạn vị trí");

        // Flash Shipping
        binding.benefitFlashShipping.ivIcon.setImageResource(R.drawable.ic_timer);
        binding.benefitFlashShipping.tvTitle.setText("Giao hỏa tốc");
        binding.benefitFlashShipping.tvDesc.setText("Giao hàng nhanh trong 2h");

        // Double Points
        binding.benefitDoublePoints.ivIcon.setImageResource(R.drawable.ic_auto_awesome);
        binding.benefitDoublePoints.tvTitle.setText("Double Points");
        binding.benefitDoublePoints.tvDesc.setText("Gấp đôi điểm thưởng mỗi lần mua");

        // Vouchers
        binding.benefitVoucher.ivIcon.setImageResource(R.drawable.ic_confirmation_number);
        binding.benefitVoucher.tvTitle.setText("Voucher độc quyền");
        binding.benefitVoucher.tvDesc.setText("Voucher sinh nhật và các quyền lợi khác");
    }

    private void loadMembershipData() {
        String token = sessionManager.getToken();
        if (token == null) return;

        String authHeader = "Bearer " + token;
        
        // Fetch User Profile
        ApiClient.INSTANCE.getInstance().getProfile(authHeader).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    
                    // Fetch Orders to calculate total spending and rank
                    fetchOrdersAndCalculate(user);
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Lỗi tải dữ liệu profile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchOrdersAndCalculate(User user) {
        String userId = user.get_id();
        if (userId == null) {
            updateUI(user, 0, 0);
            return;
        }
        ApiClient.INSTANCE.getInstance().getMyOrders(userId).enqueue(new Callback<java.util.List<com.project.models.Order>>() {
            @Override
            public void onResponse(Call<java.util.List<com.project.models.Order>> call, Response<java.util.List<com.project.models.Order>> response) {
                if (isAdded()) {
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
                    updateUI(user, totalSpending, ordersCount);
                }
            }

            @Override
            public void onFailure(Call<java.util.List<com.project.models.Order>> call, Throwable t) {
                if (isAdded()) {
                    // Fallback to profile data if orders fail
                    updateUI(user, 0, 0);
                    Toast.makeText(requireContext(), "Lỗi tải dữ liệu đơn hàng", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUI(User user, double totalSpending, int ordersCount) {
        // Hạng thành viên (Dựa trên tổng chi tiêu)
        String rank;
        
        // Mức độ trải nghiệm (level) tính theo số lượng đơn hàng đã đặt
        int level;
        if (ordersCount <= 5) {
            level = 1;
        } else if (ordersCount <= 10) {
            level = 2;
        } else if (ordersCount <= 15) {
            level = 3;
        } else if (ordersCount <= 20) {
            level = 4;
        } else {
            level = 5;
        }

        if (totalSpending >= 10000000) {
            rank = "DIAMOND";
        } else if (totalSpending >= 5000000) {
            rank = "GOLDEN";
        } else if (totalSpending >= 1000000) {
            rank = "SILVER";
        } else {
            rank = "PINK";
        }

        binding.tvRankName.setText(rank);
        
        // Điểm tích lũy (Sử dụng points từ User profile nếu có, hoặc tính toán từ chi tiêu)
        // Thông thường points từ backend sẽ chính xác hơn
        int points = user.getPoints() > 0 ? user.getPoints() : (int) (totalSpending / 1000);
        binding.tvPoints.setText(formatPoints(points));
        
        // Cấp độ
        binding.tvLevel.setText(String.format(Locale.US, "Level %02d", level));
        
        // Tính toán năm tham gia
        binding.tvYears.setText(calculateMembershipYears(user.getCreatedAt()));

        // Thanh tiến trình
        double nextMilestone;
        String nextRank;
        if (totalSpending < 1000000) {
            nextMilestone = 1000000;
            nextRank = "SILVER";
        } else if (totalSpending < 5000000) {
            nextMilestone = 5000000;
            nextRank = "GOLDEN";
        } else {
            nextMilestone = 10000000; // Diamond or next milestone
            nextRank = "DIAMOND";
        }

        int progress = (int) (totalSpending * 100 / nextMilestone);
        if (progress > 100) progress = 100;
        binding.progressPoints.setProgress(progress);
        
        double remaining = nextMilestone - totalSpending;
        if (remaining > 0) {
            binding.tvPointsToNext.setText(String.format(Locale.US, "%s NỮA ĐỂ LÊN HẠNG %s", formatCurrency(remaining), nextRank));
        } else {
            binding.tvPointsToNext.setText("BẠN ĐÃ ĐẠT HẠNG CAO NHẤT");
        }
    }

    private String formatCurrency(double amount) {
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
        return formatter.format(amount) + "đ";
    }

    private String formatPoints(int points) {
        if (points >= 1000) {
            return String.format(Locale.US, "%.1fk", points / 1000.0);
        }
        return String.valueOf(points);
    }

    private String calculateMembershipYears(String createdAt) {
        if (createdAt == null || createdAt.isEmpty()) return "0 năm";
        try {
            // ISO 8601 format
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date createdDate = sdf.parse(createdAt);
            if (createdDate == null) return "0 năm";

            long diffInMillis = new Date().getTime() - createdDate.getTime();
            double years = diffInMillis / (1000.0 * 60 * 60 * 24 * 365);
            
            if (years < 0.1) return "Mới tham gia";
            return String.format(Locale.US, "%.1f năm", years);
        } catch (Exception e) {
            e.printStackTrace();
            return "0 năm";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}