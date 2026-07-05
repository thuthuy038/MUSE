package com.project.muse_android.voucher;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.project.adapters.VoucherAdapter;
import com.project.models.ApplyVoucherRequest;
import com.project.models.ApplyVoucherResponse;
import com.project.models.Promotion;
import com.project.models.Voucher;
import com.project.network.ApiClient;
import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentVoucherBottomSheetBinding;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoucherBottomSheetFragment extends BottomSheetDialogFragment {

    private FragmentVoucherBottomSheetBinding binding;
    private VoucherAdapter discountAdapter;
    private VoucherAdapter shippingAdapter;
    private final List<Voucher> allVouchers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentVoucherBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerViews();
        fetchPromotions();

        binding.ivBack.setOnClickListener(v -> dismiss());

        binding.etVoucherCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.length() > 0;
                binding.btnApply.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), hasText ? R.color.primary_500 : R.color.neutral_300)));
                binding.btnApply.setEnabled(hasText);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnApply.setOnClickListener(v -> {
            String code = binding.etVoucherCode.getText().toString().trim();
            applyVoucherCode(code);
        });

        binding.btnConfirm.setOnClickListener(v -> dismiss());
    }

    private void setupRecyclerViews() {
        discountAdapter = new VoucherAdapter(requireContext(), (voucher, position) -> {
            voucher.setSelected(!voucher.isSelected());
            discountAdapter.notifyItemChanged(position);
            updateSummary();
        });

        shippingAdapter = new VoucherAdapter(requireContext(), (voucher, position) -> {
            voucher.setSelected(!voucher.isSelected());
            shippingAdapter.notifyItemChanged(position);
            updateSummary();
        });

        binding.rvDiscountVouchers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvDiscountVouchers.setAdapter(discountAdapter);

        binding.rvShippingVouchers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvShippingVouchers.setAdapter(shippingAdapter);
    }

    private void fetchPromotions() {
        ApiClient.INSTANCE.getInstance().getPromotions().enqueue(new Callback<List<Promotion>>() {
            @Override
            public void onResponse(@NonNull Call<List<Promotion>> call, @NonNull Response<List<Promotion>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mapPromotionsToVouchers(response.body());
                } else {
                    Toast.makeText(getContext(), "Lỗi tải khuyến mãi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Promotion>> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mapPromotionsToVouchers(List<Promotion> promotions) {
        List<Voucher> discountVouchers = new ArrayList<>();
        List<Voucher> shippingVouchers = new ArrayList<>();
        allVouchers.clear();

        for (Promotion p : promotions) {
            Voucher v = new Voucher();
            v.setId(p.getId());
            v.setCode(p.getCode());
            v.setName(p.getName());
            v.setDescription(p.getDescription());
            v.setExpiryDate(p.getEndDate());
            
            if (p.getConditions() != null && !p.getConditions().isEmpty()) {
                v.setMinOrderValue(p.getConditions().get(0).getMinOrderValue());
            }

            allVouchers.add(v);

            // Simple type check based on name
            if (p.getName().toLowerCase().contains("vận chuyển") || p.getName().toLowerCase().contains("shipping")) {
                v.setType("SHIPPING");
                shippingVouchers.add(v);
            } else {
                v.setType("DISCOUNT");
                discountVouchers.add(v);
            }
        }

        discountAdapter.setData(discountVouchers);
        shippingAdapter.setData(shippingVouchers);
        updateSummary();
    }

    private void applyVoucherCode(String code) {
        String dummyOrderId = "6677889900"; // Should be passed from Cart
        ApplyVoucherRequest request = new ApplyVoucherRequest(code, dummyOrderId);
        
        ApiClient.INSTANCE.getInstance().applyVoucher(request).enqueue(new Callback<ApplyVoucherResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApplyVoucherResponse> call, @NonNull Response<ApplyVoucherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Mã không hợp lệ hoặc không đủ điều kiện", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApplyVoucherResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummary() {
        int selectedCount = 0;
        for (Voucher v : allVouchers) {
            if (v.isSelected()) {
                selectedCount++;
            }
        }
        binding.tvAppliedVouchersCount.setText(selectedCount + " Voucher đã được áp dụng");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
