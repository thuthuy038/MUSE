package com.project.muse_android.cart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.project.models.Product;
import com.project.models.ProductVariant;
import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentProductVariantBottomSheetBinding;

import java.text.DecimalFormat;
import java.util.List;

public class ProductVariantBottomSheetFragment extends BottomSheetDialogFragment {

    private FragmentProductVariantBottomSheetBinding binding;
    private final Product product;
    private int currentQuantity = 1;
    private String selectedColor = "";
    private String selectedSize = "";

    public ProductVariantBottomSheetFragment(Product product) {
        this.product = product;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProductVariantBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initUI();
        setupVariantOptions();
        setupQuantityEditor();

        binding.ivClose.setOnClickListener(v -> dismiss());
        binding.btnConfirm.setOnClickListener(v -> {
            // Callback to update cart item if needed
            dismiss();
        });
    }

    private void initUI() {
        if (product == null) return;

        // Image
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            Glide.with(this).load(product.getImages().get(0).getUrl())
                    .placeholder(R.drawable.demo_product)
                    .into(binding.imgProductThumb);
        }

        // Price
        double price = product.getDiscountPrice() > 0 ? product.getDiscountPrice() : product.getPrice();
        binding.txtPrice.setText(formatPrice(price));

        // Stock
        binding.txtStock.setText("Tồn kho: " + product.getStock());
    }

    private void setupVariantOptions() {
        if (product == null) return;

        // Colors
        List<String> colors = product.getColors();
        if (colors != null) {
            for (String color : colors) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_variant, binding.chipGroupColor, false);
                chip.setText(color);
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) selectedColor = color;
                });
                binding.chipGroupColor.addView(chip);
            }
        }

        // Sizes
        List<String> sizes = product.getSizes();
        if (sizes != null) {
            for (String size : sizes) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_variant, binding.chipGroupSize, false);
                chip.setText(size);
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) selectedSize = size;
                });
                binding.chipGroupSize.addView(chip);
            }
        }
    }

    private void setupQuantityEditor() {
        binding.txtQuantity.setText(String.valueOf(currentQuantity));

        binding.btnAdd.setOnClickListener(v -> {
            currentQuantity++;
            binding.txtQuantity.setText(String.valueOf(currentQuantity));
        });

        binding.btnMinus.setOnClickListener(v -> {
            if (currentQuantity > 1) {
                currentQuantity--;
                binding.txtQuantity.setText(String.valueOf(currentQuantity));
            }
        });
    }

    private String formatPrice(double price) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(price).replace(",", ".") + "đ";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
