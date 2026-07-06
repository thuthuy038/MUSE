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
import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentProductVariantBottomSheetBinding;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class ProductVariantBottomSheetFragment extends BottomSheetDialogFragment {

    public interface OnVariantSelectedListener {
        void onConfirm(String color, String size, int quantity);
    }

    private FragmentProductVariantBottomSheetBinding binding;
    private final Product product;
    private int currentQuantity = 1;
    private String selectedColor = "";
    private String selectedSize = "";
    private String buttonText = "Xác nhận";
    private OnVariantSelectedListener listener;

    public ProductVariantBottomSheetFragment(Product product) {
        this.product = product;
    }

    public ProductVariantBottomSheetFragment(Product product, String initialColor, String initialSize, int initialQuantity) {
        this.product = product;
        this.selectedColor = initialColor != null ? initialColor : "";
        this.selectedSize = initialSize != null ? initialSize : "";
        this.currentQuantity = initialQuantity > 0 ? initialQuantity : 1;
    }

    public void setOnVariantSelectedListener(OnVariantSelectedListener listener) {
        this.listener = listener;
    }

    public void setButtonText(String text) {
        this.buttonText = text;
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
        
        binding.btnConfirm.setText(buttonText);
        binding.btnConfirm.setOnClickListener(v -> {
            if (selectedColor.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng chọn màu sắc", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedSize.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng chọn kích cỡ", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (listener != null) {
                listener.onConfirm(selectedColor, selectedSize, currentQuantity);
            }
            dismiss();
        });
    }

    private void initUI() {
        if (product == null) return;

        // Image
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            String imageUrl = product.getImages().get(0).getUrl();
            if (imageUrl != null && !imageUrl.startsWith("http")) {
                imageUrl = "https://server-testing-ymn9.onrender.com" + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
            }
            Glide.with(this).load(imageUrl)
                    .placeholder(R.drawable.demo_product)
                    .error(R.drawable.demo_product)
                    .into(binding.imgProductThumb);
        }

        // Price
        double price = product.getDiscountPrice() != null && product.getDiscountPrice() > 0 
                ? product.getDiscountPrice() : product.getPrice();
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
                
                if (color.equalsIgnoreCase(selectedColor)) {
                    chip.setChecked(true);
                }

                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) selectedColor = color;
                });
                binding.chipGroupColor.addView(chip);
            }
        }

        // Sizes
        List<Product.ProductSize> sizes = product.getSizes();
        if (sizes != null) {
            for (Product.ProductSize sizeObj : sizes) {
                String size = sizeObj.getSize();
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_variant, binding.chipGroupSize, false);
                chip.setText(size);
                
                if (size.equalsIgnoreCase(selectedSize)) {
                    chip.setChecked(true);
                }
                
                chip.setOnClickListener(v -> {
                    selectedSize = size;
                });

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
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + " VNĐ";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
