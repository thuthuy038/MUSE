package com.project.muse_android.checkout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.project.adapters.HorizontalProductAdapter;
import com.project.models.Product;
import com.project.models.ProductVariant;
import com.project.models.enums.HorizontalProductMode;
import com.project.muse_android.R;
import com.project.muse_android.cart.PromotionDetailsBottomSheetFragment;
import com.project.muse_android.databinding.ActivityCheckoutBinding;
import com.project.muse_android.voucher.VoucherBottomSheetFragment;
import com.project.utils.ViewUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private ActivityCheckoutBinding binding;
    private List<Product> checkoutProducts = new ArrayList<>();
    private HorizontalProductAdapter adapter;
    
    private double originalTotal = 0;
    private double productDiscount = 0;
    private double voucherDiscount = 0; 
    private double shippingFee = 50000; 
    private double shippingDiscount = 50000; 

    private int selectedShippingMethod = 1; // 1: Standard, 2: Fast, 3: Express
    private int selectedPaymentMethod = 1; // 1: COD, 2: Bank, 3: Momo, 4: VNPay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewUtils.applySystemBarsPadding(binding.header, true, false);
        ViewUtils.applySystemBarsPadding(binding.bottomContainer, false, true);

        loadData();
        setupUI();
        updateShippingUI();
        updatePaymentUI();
        calculatePrices();
    }

    private void loadData() {
        // Retrieve products passed from CartFragment
        List<Product> list = getIntent().getParcelableArrayListExtra("products");
        if (list != null) {
            checkoutProducts.addAll(list);
        }
        
        // Retrieve voucher discount passed from CartFragment
        voucherDiscount = getIntent().getDoubleExtra("voucher_discount", 0);
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView with Adapter in READ_ONLY mode
        adapter = new HorizontalProductAdapter(this, HorizontalProductMode.READ_ONLY, new HorizontalProductAdapter.OnProductActionListener() {
            @Override
            public void onDelete(Product product, int position) {}
            @Override
            public void onSimilar(Product product, int position) {}
            @Override
            public void onCheckedChanged(Product product, int position, boolean checked) {}
            @Override
            public void onQuantityChanged(Product product, int position, int quantity) {}
            @Override
            public void onVariantClick(Product product, int position) {}
            @Override
            public void onProductClick(Product product) {}
        });

        binding.rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrderItems.setAdapter(adapter);
        adapter.setData(checkoutProducts);

        binding.btnSelectVoucher.setOnClickListener(v -> {
            VoucherBottomSheetFragment voucherSheet = new VoucherBottomSheetFragment();
            voucherSheet.setOnVoucherSelectedListener((discount, shipping) -> {
                this.voucherDiscount = discount;
                calculatePrices();
            });
            voucherSheet.show(getSupportFragmentManager(), "VoucherBottomSheet");
        });

        binding.layoutPriceSummary.setOnClickListener(v -> {
            PromotionDetailsBottomSheetFragment sheet = new PromotionDetailsBottomSheetFragment(
                    originalTotal,
                    voucherDiscount,
                    productDiscount,
                    shippingFee,
                    shippingDiscount
            );
            sheet.show(getSupportFragmentManager(), "PromotionDetails");
        });

        binding.cardShippingStandard.setOnClickListener(v -> {
            selectedShippingMethod = 1;
            updateShippingUI();
            calculatePrices();
        });

        binding.cardShippingFast.setOnClickListener(v -> {
            selectedShippingMethod = 2;
            updateShippingUI();
            calculatePrices();
        });

        binding.cardShippingExpress.setOnClickListener(v -> {
            selectedShippingMethod = 3;
            updateShippingUI();
            calculatePrices();
        });

        binding.cardPaymentCOD.setOnClickListener(v -> {
            selectedPaymentMethod = 1;
            updatePaymentUI();
        });

        binding.cardPaymentBank.setOnClickListener(v -> {
            selectedPaymentMethod = 2;
            updatePaymentUI();
        });

        binding.cardPaymentMomo.setOnClickListener(v -> {
            selectedPaymentMethod = 3;
            updatePaymentUI();
        });

        binding.cardPaymentVNPay.setOnClickListener(v -> {
            selectedPaymentMethod = 4;
            updatePaymentUI();
        });

        binding.btnOrder.setOnClickListener(v -> {
            // In a real app, you would send the order to the server here.
            // For now, we just navigate to success screen.
            Intent intent = new Intent(this, com.project.muse_android.order.OrderSuccessActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void updateShippingUI() {
        // Reset all to unselected
        binding.cardShippingStandard.setCardBackgroundColor(getResources().getColor(R.color.white));
        binding.cardShippingStandard.setStrokeColor(getResources().getColor(R.color.neutral_300));
        binding.ivCheckStandard.setImageResource(R.drawable.ic_unchecked_circle);
        binding.ivCheckStandard.setColorFilter(getResources().getColor(R.color.neutral_300));

        binding.cardShippingFast.setCardBackgroundColor(getResources().getColor(R.color.white));
        binding.cardShippingFast.setStrokeColor(getResources().getColor(R.color.neutral_300));
        binding.ivCheckFast.setImageResource(R.drawable.ic_unchecked_circle);
        binding.ivCheckFast.setColorFilter(getResources().getColor(R.color.neutral_300));

        binding.cardShippingExpress.setCardBackgroundColor(getResources().getColor(R.color.white));
        binding.cardShippingExpress.setStrokeColor(getResources().getColor(R.color.neutral_300));
        binding.ivCheckExpress.setImageResource(R.drawable.ic_unchecked_circle);
        binding.ivCheckExpress.setColorFilter(getResources().getColor(R.color.neutral_300));

        // Set selected
        switch (selectedShippingMethod) {
            case 1:
                binding.cardShippingStandard.setCardBackgroundColor(android.graphics.Color.parseColor("#F0FEFF"));
                binding.cardShippingStandard.setStrokeColor(android.graphics.Color.parseColor("#008B86"));
                binding.ivCheckStandard.setImageResource(R.drawable.ic_check_circle);
                binding.ivCheckStandard.setColorFilter(android.graphics.Color.parseColor("#008B86"));
                break;
            case 2:
                binding.cardShippingFast.setCardBackgroundColor(android.graphics.Color.parseColor("#F0FEFF"));
                binding.cardShippingFast.setStrokeColor(android.graphics.Color.parseColor("#008B86"));
                binding.ivCheckFast.setImageResource(R.drawable.ic_check_circle);
                binding.ivCheckFast.setColorFilter(android.graphics.Color.parseColor("#008B86"));
                break;
            case 3:
                binding.cardShippingExpress.setCardBackgroundColor(android.graphics.Color.parseColor("#F0FEFF"));
                binding.cardShippingExpress.setStrokeColor(android.graphics.Color.parseColor("#008B86"));
                binding.ivCheckExpress.setImageResource(R.drawable.ic_check_circle);
                binding.ivCheckExpress.setColorFilter(android.graphics.Color.parseColor("#008B86"));
                break;
        }
    }

    private void updatePaymentUI() {
        // Reset all to unselected
        binding.cardPaymentCOD.setCardBackgroundColor(getResources().getColor(R.color.white));
        binding.cardPaymentCOD.setStrokeColor(getResources().getColor(R.color.neutral_300));
        binding.ivCheckCOD.setImageResource(R.drawable.ic_unchecked_circle);
        binding.ivCheckCOD.setColorFilter(getResources().getColor(R.color.neutral_300));

        binding.cardPaymentBank.setCardBackgroundColor(getResources().getColor(R.color.white));
        binding.cardPaymentBank.setStrokeColor(getResources().getColor(R.color.neutral_300));
        binding.ivCheckBank.setImageResource(R.drawable.ic_unchecked_circle);
        binding.ivCheckBank.setColorFilter(getResources().getColor(R.color.neutral_300));

        binding.cardPaymentMomo.setCardBackgroundColor(getResources().getColor(R.color.white));
        binding.cardPaymentMomo.setStrokeColor(getResources().getColor(R.color.neutral_300));
        binding.ivCheckMomo.setImageResource(R.drawable.ic_unchecked_circle);
        binding.ivCheckMomo.setColorFilter(getResources().getColor(R.color.neutral_300));

        binding.cardPaymentVNPay.setCardBackgroundColor(getResources().getColor(R.color.white));
        binding.cardPaymentVNPay.setStrokeColor(getResources().getColor(R.color.neutral_300));
        binding.ivCheckVNPay.setImageResource(R.drawable.ic_unchecked_circle);
        binding.ivCheckVNPay.setColorFilter(getResources().getColor(R.color.neutral_300));

        // Set selected
        switch (selectedPaymentMethod) {
            case 1:
                binding.cardPaymentCOD.setCardBackgroundColor(android.graphics.Color.parseColor("#FFE5EC"));
                binding.cardPaymentCOD.setStrokeColor(android.graphics.Color.parseColor("#FB6F92"));
                binding.ivCheckCOD.setImageResource(R.drawable.ic_check_circle);
                binding.ivCheckCOD.setColorFilter(android.graphics.Color.parseColor("#FB6F92"));
                break;
            case 2:
                binding.cardPaymentBank.setCardBackgroundColor(android.graphics.Color.parseColor("#FFE5EC"));
                binding.cardPaymentBank.setStrokeColor(android.graphics.Color.parseColor("#FB6F92"));
                binding.ivCheckBank.setImageResource(R.drawable.ic_check_circle);
                binding.ivCheckBank.setColorFilter(android.graphics.Color.parseColor("#FB6F92"));
                break;
            case 3:
                binding.cardPaymentMomo.setCardBackgroundColor(android.graphics.Color.parseColor("#FFE5EC"));
                binding.cardPaymentMomo.setStrokeColor(android.graphics.Color.parseColor("#FB6F92"));
                binding.ivCheckMomo.setImageResource(R.drawable.ic_check_circle);
                binding.ivCheckMomo.setColorFilter(android.graphics.Color.parseColor("#FB6F92"));
                break;
            case 4:
                binding.cardPaymentVNPay.setCardBackgroundColor(android.graphics.Color.parseColor("#FFE5EC"));
                binding.cardPaymentVNPay.setStrokeColor(android.graphics.Color.parseColor("#FB6F92"));
                binding.ivCheckVNPay.setImageResource(R.drawable.ic_check_circle);
                binding.ivCheckVNPay.setColorFilter(android.graphics.Color.parseColor("#FB6F92"));
                break;
        }
    }

    private void calculatePrices() {
        originalTotal = 0;
        productDiscount = 0;
        double itemTotalDiscounted = 0;
        int totalQuantity = 0;

        for (Product p : checkoutProducts) {
            int q = p.getQuantity() > 0 ? p.getQuantity() : 1;
            totalQuantity += q;
            
            originalTotal += p.getPrice() * q;
            double discPrice = (p.getDiscountPrice() != null && p.getDiscountPrice() > 0) 
                    ? p.getDiscountPrice() : p.getPrice();
            
            itemTotalDiscounted += discPrice * q;
            productDiscount += (p.getPrice() - discPrice) * q;
        }

        double finalTotal = itemTotalDiscounted + shippingFee - shippingDiscount - voucherDiscount;

        binding.txtTotalCountLabel.setText("Tổng số tiền (" + totalQuantity + " sản phẩm)");
        binding.txtTotalItemsPrice.setText(formatPrice(itemTotalDiscounted));

        binding.txtSubtotal.setText(formatPrice(itemTotalDiscounted));
        binding.txtShippingFee.setText(formatPrice(shippingFee));
        binding.txtShippingDiscount.setText("-" + formatPrice(shippingDiscount));
        binding.txtVoucherDiscount.setText("-" + formatPrice(voucherDiscount));
        binding.txtTotalAmount.setText(formatPrice(finalTotal));
        
        binding.txtBottomTotal.setText(formatPrice(finalTotal));
        binding.txtBottomSavings.setText("Tiết kiệm: " + formatPrice(productDiscount + voucherDiscount));
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + "đ";
    }
}
