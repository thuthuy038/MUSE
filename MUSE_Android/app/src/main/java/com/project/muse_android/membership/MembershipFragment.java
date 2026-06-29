package com.project.muse_android.membership;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentMembershipBinding;
import com.project.utils.SessionManager;
import com.project.network.ApiClient;
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

        binding.ivBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
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

        ApiClient.INSTANCE.getInstance().getProfile("Bearer " + token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    updateUI(user);
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUI(User user) {
        // Hạng thành viên (Dựa trên level hoặc data từ database)
        String rank = "MEMBER";
        int points = user.getPoints();
        int level = user.getLevel();

        if (level >= 10) rank = "DIAMOND";
        else if (level >= 7) rank = "PLATINUM";
        else if (level >= 4) rank = "GOLDEN";
        else if (level >= 2) rank = "SILVER";

        binding.tvRankName.setText(rank);
        
        // Điểm tích lũy
        binding.tvPoints.setText(formatPoints(points));
        
        // Cấp độ
        binding.tvLevel.setText(String.format(Locale.US, "Level %02d", level));
        
        // Tính toán năm tham gia
        binding.tvYears.setText(calculateMembershipYears(user.getCreatedAt()));

        // Thanh tiến trình (Giả sử 1000 điểm để lên cấp tiếp theo)
        int nextLevelPoints = ((level / 3) + 1) * 1000; 
        int progress = (points % nextLevelPoints) * 100 / nextLevelPoints;
        binding.progressPoints.setProgress(progress);
        
        int remaining = nextLevelPoints - (points % nextLevelPoints);
        binding.tvPointsToNext.setText(String.format(Locale.US, "%d ĐIỂM NỮA ĐỂ TĂNG HẠNG", remaining));
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