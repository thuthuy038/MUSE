package com.project.muse_android.notification;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.project.models.Promotion;
import com.project.muse_android.databinding.DialogPromotionDetailBinding;

import java.util.Locale;

public class PromotionDetailDialog extends DialogFragment {

    private DialogPromotionDetailBinding binding;
    private Promotion promotion;

    public static PromotionDetailDialog newInstance(Promotion promotion) {
        PromotionDetailDialog fragment = new PromotionDetailDialog();
        Bundle args = new Bundle();
        args.putSerializable("promotion", (java.io.Serializable) promotion);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            promotion = (Promotion) getArguments().getSerializable("promotion");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogPromotionDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            android.view.Window window = getDialog().getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Remove the dark background dimming
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (promotion != null) {
            binding.tvPromoName.setText(promotion.getName());
            
            // Determine if it's percentage or fixed VND early for consistency
            boolean isPercent = false;
            Promotion.Condition cond = null;
            if (promotion.getConditions() != null && !promotion.getConditions().isEmpty()) {
                cond = promotion.getConditions().get(0);
                String rawType = cond.getDiscountType();
                isPercent = rawType != null && (rawType.trim().equalsIgnoreCase("percent") || rawType.toLowerCase().startsWith("per") || rawType.contains("%"));
                
                // Fallback: If name has % and value is small (<= 100), treat as percentage for consistency
                if (!isPercent && cond.getDiscountValue() != null && cond.getDiscountValue() <= 100) {
                    if (promotion.getName() != null && promotion.getName().contains("%")) {
                        isPercent = true;
                    }
                }
            }

            // 1. Format Badge (Discount)
            if (cond != null && cond.getDiscountValue() != null) {
                if (isPercent) {
                    binding.tvDiscountValue.setText(String.format(Locale.getDefault(), "%.0f%% OFF", cond.getDiscountValue()));
                } else {
                    // Display as -Xk for large VND values
                    if (cond.getDiscountValue() >= 1000) {
                        binding.tvDiscountValue.setText(String.format(Locale.getDefault(), "-%.0fkđ", cond.getDiscountValue() / 1000));
                    } else {
                        binding.tvDiscountValue.setText(String.format(Locale.getDefault(), "-%.0fđ", cond.getDiscountValue()));
                    }
                }
            } else if (cond != null && cond.getGiftProductId() != null) {
                binding.tvDiscountValue.setText("GIFT");
            }

            // 2. Date Range
            String start = formatDate(promotion.getStartDate());
            String end = formatDate(promotion.getEndDate());
            binding.tvDateRange.setText("KHẢ DỤNG: " + start + " - " + end);

            // 3. Dynamic Description based on conditions
            StringBuilder detailBuilder = new StringBuilder();
            if (cond != null) {
                String method = promotion.getPromotionMethod() != null ? promotion.getPromotionMethod().trim() : "";

                if ("discountOrder".equals(method) || "quantityDiscount".equals(method)) {
                    detailBuilder.append("Ưu đãi giảm ");
                    if (isPercent) {
                        detailBuilder.append(cond.getDiscountValue().intValue()).append("%");
                    } else {
                        detailBuilder.append(String.format(Locale.getDefault(), "%,.0fđ", cond.getDiscountValue()));
                    }
                    
                    if (cond.getMinOrderValue() != null && cond.getMinOrderValue() > 0) {
                        detailBuilder.append(" cho đơn hàng từ ").append(String.format(Locale.getDefault(), "%,.0fđ", cond.getMinOrderValue()));
                    } else {
                        detailBuilder.append(" cho mọi đơn hàng.");
                    }
                } else if ("buyXGetY".equals(method)) {
                    detailBuilder.append("Chương trình Mua ").append(cond.getBuyQuantity())
                                 .append(" Tặng ").append(cond.getGiftQuantity())
                                 .append(" sản phẩm.");
                } else if ("giftOrder".equals(method)) {
                    detailBuilder.append("Tặng quà miễn phí cho đơn hàng từ ")
                                 .append(String.format(Locale.getDefault(), "%,.0fđ", cond.getMinOrderValue()));
                } else if (promotion.getDescription() != null) {
                    detailBuilder.append(promotion.getDescription());
                }
            } else if (promotion.getDescription() != null) {
                detailBuilder.append(promotion.getDescription());
            }

            if (detailBuilder.length() > 0) {
                binding.tvDescription.setText(detailBuilder.toString());
            }
        }

        binding.btnClose.setOnClickListener(v -> dismiss());
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || !isoDate.contains("-")) return "";
        try {
            // Simple split for yyyy-MM-ddThh:mm:ss
            String datePart = isoDate.split("T")[0];
            String[] parts = datePart.split("-");
            if (parts.length == 3) {
                return parts[2] + "/" + parts[1] + "/" + parts[0]; // dd/MM/yyyy
            }
            return datePart;
        } catch (Exception e) {
            return isoDate;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
