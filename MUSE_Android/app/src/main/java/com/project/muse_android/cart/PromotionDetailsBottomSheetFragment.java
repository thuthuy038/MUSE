package com.project.muse_android.cart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.project.muse_android.databinding.FragmentPromotionDetailsBottomSheetBinding;

public class PromotionDetailsBottomSheetFragment extends BottomSheetDialogFragment {

    private FragmentPromotionDetailsBottomSheetBinding binding;

    private double originalTotal;
    private double voucherDiscount;
    private double productDiscount;
    private double shippingFee;
    private double shippingDiscount;

    public PromotionDetailsBottomSheetFragment(double originalTotal, double voucherDiscount, double productDiscount, double shippingFee, double shippingDiscount) {
        this.originalTotal = originalTotal;
        this.voucherDiscount = voucherDiscount;
        this.productDiscount = productDiscount;
        this.shippingFee = shippingFee;
        this.shippingDiscount = shippingDiscount;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPromotionDetailsBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        double totalSavings = voucherDiscount + productDiscount;
        double finalTotal = originalTotal - totalSavings;

        binding.tvOriginalTotal.setText(formatPrice(originalTotal));
        binding.tvVoucherDiscount.setText("-" + formatPrice(voucherDiscount));
        binding.tvProductDiscount.setText("-" + formatPrice(productDiscount));
        binding.tvTotalSavings.setText("-" + formatPrice(totalSavings));
        binding.tvFinalTotal.setText(formatPrice(finalTotal));

        binding.tvShippingFee.setText(formatPrice(shippingFee));
        binding.tvShippingDiscount.setText("-" + formatPrice(shippingDiscount));

        binding.ivClose.setOnClickListener(v -> dismiss());
    }

    private String formatPrice(double price) {
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(new java.util.Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        java.text.DecimalFormat decimalFormat = new java.text.DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + " VNĐ";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}