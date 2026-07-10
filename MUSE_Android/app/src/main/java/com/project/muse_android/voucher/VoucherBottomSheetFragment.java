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

    public interface OnVoucherSelectedListener {
        void onVouchersSelected(double discountAmount, double shippingDiscountAmount, String voucherCode);
    }

    private OnVoucherSelectedListener listener;
    private double orderTotal = 0;

    public static VoucherBottomSheetFragment newInstance(double orderTotal) {
        VoucherBottomSheetFragment fragment = new VoucherBottomSheetFragment();
        Bundle args = new Bundle();
        args.putDouble("order_total", orderTotal);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnVoucherSelectedListener(OnVoucherSelectedListener listener) {
        this.listener = listener;
    }

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

        if (getArguments() != null) {
            orderTotal = getArguments().getDouble("order_total", 0);
        }

        setupRecyclerViews();
        fetchPromotions();

        binding.ivBack.setOnClickListener(v -> dismiss());

        binding.ivHelp.setOnClickListener(v -> {
            VoucherHelpBottomSheetFragment.newInstance().show(getParentFragmentManager(), "VoucherHelp");
        });

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

        binding.btnConfirm.setOnClickListener(v -> {
            if (listener != null) {
                double discount = 0;
                double shipping = 0;
                String selectedCode = "";
                
                if (discountAdapter != null) {
                    for (Voucher voucher : allVouchers) {
                        if (voucher.isSelected()) {
                            double discountVal = voucher.calculateDiscount(orderTotal);
                            if ("SHIPPING".equals(voucher.getType())) {
                                shipping += discountVal;
                            } else {
                                discount += discountVal;
                                selectedCode = voucher.getCode();
                            }
                        }
                    }
                }
                listener.onVouchersSelected(discount, shipping, selectedCode);
            }
            dismiss();
        });
    }

    private void setupRecyclerViews() {
        discountAdapter = new VoucherAdapter(requireContext(), (voucher, position) -> {
            boolean wasSelected = voucher.isSelected();
            for (Voucher v : allVouchers) {
                if (!"SHIPPING".equals(v.getType())) {
                    v.setSelected(false);
                }
            }
            voucher.setSelected(!wasSelected);
            discountAdapter.notifyDataSetChanged();
            updateSummary();
        });

        shippingAdapter = new VoucherAdapter(requireContext(), (voucher, position) -> {
            boolean wasSelected = voucher.isSelected();
            for (Voucher v : allVouchers) {
                if ("SHIPPING".equals(v.getType())) {
                    v.setSelected(false);
                }
            }
            voucher.setSelected(!wasSelected);
            shippingAdapter.notifyDataSetChanged();
            updateSummary();
        });

        binding.rvDiscountVouchers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvDiscountVouchers.setAdapter(discountAdapter);

    }

    private void fetchPromotions() {
        android.util.Log.d("VoucherSheet", "Bắt đầu gọi API lấy danh sách khuyến mãi...");
        ApiClient.INSTANCE.getInstance().getPromotions().enqueue(new Callback<List<Promotion>>() {
            @Override
            public void onResponse(@NonNull Call<List<Promotion>> call, @NonNull Response<List<Promotion>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("VoucherSheet", "Lấy khuyến mãi thành công: thu được " + response.body().size() + " khuyến mãi.");
                    loadVouchersForPromotions(response.body());
                } else {
                    String errorMsg = "Lỗi tải khuyến mãi. Code: " + response.code() + ", Message: " + response.message();
                    android.util.Log.e("VoucherSheet", errorMsg);
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Promotion>> call, @NonNull Throwable t) {
                String errorMsg = "Lỗi kết nối API Promotions: " + t.getMessage();
                android.util.Log.e("VoucherSheet", errorMsg, t);
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadVouchersForPromotions(List<Promotion> promotions) {
        List<Voucher> discountVouchers = new ArrayList<>();
        List<Voucher> shippingVouchers = new ArrayList<>();
        allVouchers.clear();

        List<Promotion> activePromotions = new ArrayList<>();
        for (Promotion p : promotions) {
            android.util.Log.d("VoucherSheet", "Khuyến mãi tìm thấy: ID=" + p.getId() + ", Name=" + p.getName() + ", Status=" + p.getStatus());
            if ("active".equalsIgnoreCase(p.getStatus())) {
                if (p.getVoucher() != null && p.getVoucher().getQuantity() <= 0) {
                    android.util.Log.d("VoucherSheet", "Khuyến mãi " + p.getName() + " bị bỏ qua do quantity <= 0");
                    continue;
                }
                activePromotions.add(p);
            }
        }

        if (activePromotions.isEmpty()) {
            android.util.Log.d("VoucherSheet", "Không tìm thấy chương trình khuyến mãi nào có status 'active'.");
            Toast.makeText(getContext(), "Không có chương trình khuyến mãi nào hoạt động", Toast.LENGTH_LONG).show();
            discountAdapter.setData(discountVouchers);
            shippingAdapter.setData(shippingVouchers);
            updateSummary();
            return;
        }

        android.util.Log.d("VoucherSheet", "Bắt đầu tải vouchers cho " + activePromotions.size() + " khuyến mãi active...");
        final int[] pendingRequests = { activePromotions.size() };

        for (Promotion p : activePromotions) {
            ApiClient.INSTANCE.getInstance().getVouchersByPromotion(p.getId()).enqueue(new Callback<List<Voucher>>() {
                @Override
                public void onResponse(@NonNull Call<List<Voucher>> call, @NonNull Response<List<Voucher>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Voucher> vouchersList = response.body();
                        android.util.Log.d("VoucherSheet", "Tải voucher thành công cho Promotion ID " + p.getId() + ": thu được " + vouchersList.size() + " vouchers.");
                        
                        int unusedCount = 0;
                        for (Voucher v : vouchersList) {
                            if ("unused".equalsIgnoreCase(v.getStatus())) {
                                v.setPromotion(p);
                                allVouchers.add(v);
                                unusedCount++;

                                if ("SHIPPING".equals(v.getType())) {
                                    shippingVouchers.add(v);
                                } else {
                                    discountVouchers.add(v);
                                }
                            }
                        }
                        android.util.Log.d("VoucherSheet", "Có " + unusedCount + " / " + vouchersList.size() + " vouchers ở trạng thái 'unused'.");
                    } else {
                        String errorMsg = "Lỗi tải voucher của promotion " + p.getName() + ". Code: " + response.code();
                        android.util.Log.e("VoucherSheet", errorMsg);
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                    }

                    pendingRequests[0]--;
                    if (pendingRequests[0] == 0) {
                        android.util.Log.d("VoucherSheet", "Hoàn tất tải tất cả vouchers. Tổng số hiển thị: Discount=" + discountVouchers.size() + ", Shipping=" + shippingVouchers.size());
                        if (allVouchers.isEmpty()) {
                            Toast.makeText(getContext(), "Không tìm thấy voucher khả dụng nào", Toast.LENGTH_LONG).show();
                        }
                        discountAdapter.setData(discountVouchers);
                        shippingAdapter.setData(shippingVouchers);
                        updateSummary();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<Voucher>> call, @NonNull Throwable t) {
                    String errorMsg = "Lỗi kết nối API Vouchers cho " + p.getName() + ": " + t.getMessage();
                    android.util.Log.e("VoucherSheet", errorMsg, t);
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();

                    pendingRequests[0]--;
                    if (pendingRequests[0] == 0) {
                        discountAdapter.setData(discountVouchers);
                        shippingAdapter.setData(shippingVouchers);
                        updateSummary();
                    }
                }
            });
        }
    }

    private void applyVoucherCode(String code) {
        ApplyVoucherRequest request = new ApplyVoucherRequest(code, ""); // orderId is blank during selection
        ApiClient.INSTANCE.getInstance().applyVoucher(request).enqueue(new Callback<ApplyVoucherResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApplyVoucherResponse> call, @NonNull Response<ApplyVoucherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApplyVoucherResponse applyResponse = response.body();
                    Toast.makeText(getContext(), applyResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    Voucher appliedVoucher = applyResponse.getVoucher();
                    if (appliedVoucher != null) {
                        fetchPromotionDetailsAndCallback(appliedVoucher);
                    } else {
                        dismiss();
                    }
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

    private void fetchPromotionDetailsAndCallback(Voucher voucher) {
        String promotionId = voucher.getPromotionId();
        if (promotionId == null || promotionId.isEmpty()) {
            if (listener != null) {
                double discount = "SHIPPING".equals(voucher.getType()) ? 0 : 10000;
                double shipping = "SHIPPING".equals(voucher.getType()) ? 50000 : 0;
                listener.onVouchersSelected(discount, shipping, voucher.getCode());
            }
            dismiss();
            return;
        }

        ApiClient.INSTANCE.getInstance().getPromotionById(promotionId).enqueue(new Callback<Promotion>() {
            @Override
            public void onResponse(@NonNull Call<Promotion> call, @NonNull Response<Promotion> response) {
                if (response.isSuccessful() && response.body() != null) {
                    voucher.setPromotion(response.body());
                    if (listener != null) {
                        double discountVal = voucher.calculateDiscount(orderTotal);
                        double discount = "SHIPPING".equals(voucher.getType()) ? 0 : discountVal;
                        double shipping = "SHIPPING".equals(voucher.getType()) ? discountVal : 0;
                        listener.onVouchersSelected(discount, shipping, voucher.getCode());
                    }
                } else {
                    if (listener != null) {
                        double discount = "SHIPPING".equals(voucher.getType()) ? 0 : 10000;
                        double shipping = "SHIPPING".equals(voucher.getType()) ? 50000 : 0;
                        listener.onVouchersSelected(discount, shipping, voucher.getCode());
                    }
                }
                dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<Promotion> call, @NonNull Throwable t) {
                if (listener != null) {
                    double discount = "SHIPPING".equals(voucher.getType()) ? 0 : 10000;
                    double shipping = "SHIPPING".equals(voucher.getType()) ? 50000 : 0;
                    listener.onVouchersSelected(discount, shipping, voucher.getCode());
                }
                dismiss();
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
    public void onStart() {
        super.onStart();
        android.app.Dialog dialog = getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior = 
                        com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}