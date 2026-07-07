package com.project.muse_android.product;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.project.models.Category;
import com.project.models.Product;
import com.project.models.ProductVariant;
import com.project.muse_android.R;
import com.project.muse_android.main.MainActivity;
import com.project.muse_android.cart.ProductVariantBottomSheetFragment;
import com.project.muse_android.checkout.CheckoutActivity;
import com.project.muse_android.databinding.ActivityProductDetailBinding;
import com.project.network.HomeApiClient;
import com.project.network.ApiService;
import com.project.network.HomeApiService;
import com.project.utils.ViewUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.project.utils.CartManager;

public class ProductDetailActivity extends AppCompatActivity {

    private ActivityProductDetailBinding binding;
    private String productId;
    private Product currentProduct;
    private String selectedColor = "";
    private String selectedSize = "";
    private static final String BASE_URL = "https://server-testing-ymn9.onrender.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge support
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Đẩy Header (nút Back) xuống dưới Status Bar
        ViewUtils.applySystemBarsPadding(binding.header, true, false);

        productId = getIntent().getStringExtra("product_id");

        binding.btnBack.setOnClickListener(v -> finish());
        binding.txtOriginalPrice.setPaintFlags(binding.txtOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        binding.btnCart.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
            intent.putExtra("open_cart", true);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        if (productId != null) {
            loadProductDetail();
        } else {
            Toast.makeText(this, "Không tìm thấy mã sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupActionButtons();
        setupCollapsibleSections();
    }

    private void loadProductDetail() {
        ApiService service = HomeApiClient.getApiService();
        service.getProductDetail(productId).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayProduct(response.body());
                } else {
                    Toast.makeText(ProductDetailActivity.this, "Lỗi tải chi tiết sản phẩm", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                Toast.makeText(ProductDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayProduct(Product product) {
        this.currentProduct = product;
        // Breadcrumb and Basic Info
        binding.txtProductNameBreadcrumb.setText(product.getName());
        binding.txtProductName.setText(product.getName());

        // Price Logic
        Double dPrice = product.getDiscountPrice();
        if (dPrice != null && dPrice > 0) {
            binding.txtPrice.setText(formatPrice(dPrice));
            binding.txtOriginalPrice.setText(formatPrice(product.getPrice()));
            binding.txtOriginalPrice.setVisibility(View.VISIBLE);
            binding.txtOriginalPrice.setPaintFlags(binding.txtOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            binding.txtPrice.setText(formatPrice(product.getPrice()));
            binding.txtOriginalPrice.setVisibility(View.GONE);
        }

        // Stats
        binding.txtSoldCount.setText(String.format(Locale.getDefault(), "Đã bán %d", product.getSoldCount()));
        binding.txtRatingScore.setText(String.valueOf(product.getRating()));

        binding.txtReviewTitle.setText(String.format(Locale.getDefault(), "Đánh giá sản phẩm (%d)", product.getReviewCount()));

        // Stock and Category
        binding.txtStockInfo.setText(product.getStock() > 0 ? "Kho còn hàng" : "Hết hàng");
        if (product.getStock() <= 0) {
            binding.txtSoldOut.setVisibility(View.VISIBLE);
            binding.btnBuyNow.setEnabled(false);
            binding.btnBuyNow.setAlpha(0.5f);
        }

        // Load Category Name for Breadcrumb
        loadCategoryName(product.getCategory());

        // Description
        binding.txtDescription.setText(product.getDescription() != null && !product.getDescription().isEmpty() ?
                product.getDescription() : "Mô tả đang được cập nhật...");

        // Image
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            String imageUrl = product.getImages().get(0).getUrl();
            if (!imageUrl.startsWith("http")) {
                imageUrl = BASE_URL + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
            }
            Glide.with(this).load(imageUrl).placeholder(R.drawable.image).into(binding.imgProduct);
            binding.txtImageIndicator.setText(String.format(Locale.getDefault(), "1/%d", product.getImages().size()));
        }

        // Colors & Sizes
        setupColors(product.getColors());
        
        // Cung cấp cả product để lấy được đầy đủ danh sách size (từ cả variants và sizes)
        setupSizes(product);

        updateFavoriteUI(product.isFavorite());
        binding.btnFavoriteDetail.setOnClickListener(v -> {
            product.setFavorite(!product.isFavorite());
            updateFavoriteUI(product.isFavorite());
            Toast.makeText(this, product.isFavorite() ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateFavoriteUI(boolean isFavorite) {
        if (isFavorite) {
            binding.btnFavoriteDetail.setImageResource(R.drawable.ic_favorite_filled);
            binding.btnFavoriteDetail.setImageTintList(null);
        } else {
            binding.btnFavoriteDetail.setImageResource(R.drawable.ic_favorite);
            binding.btnFavoriteDetail.setImageTintList(ColorStateList.valueOf(Color.parseColor("#333333")));
        }
    }

    private void loadCategoryName(String categoryId) {
        if (categoryId == null) return;
        HomeApiService homeService = HomeApiClient.getHomeApiService();
        homeService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Category cat : response.body()) {
                        if (cat.getId().equals(categoryId)) {
                            binding.txtCategoryBreadcrumb.setText(cat.getName().toUpperCase());
                            binding.txtCategoryInfo.setText(String.format("Danh mục %s", cat.getName()));
                            break;
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {}
        });
    }

    private void setupColors(List<String> colors) {
        binding.layoutColorsDetail.removeAllViews();
        if (colors == null || colors.isEmpty()) {
            binding.txtSelectedColorLabel.setVisibility(View.GONE);
            return;
        }

        binding.txtSelectedColorLabel.setVisibility(View.VISIBLE);
        float density = getResources().getDisplayMetrics().density;
        int dotSize = (int) (32 * density);
        int margin = (int) (12 * density);

        final View[] selectedDot = {null};

        for (int i = 0; i < colors.size(); i++) {
            String colorStr = colors.get(i);
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSize, dotSize);
            params.setMargins(0, 0, margin, 0);
            dot.setLayoutParams(params);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            try {
                shape.setColor(Color.parseColor(colorStr.startsWith("#") ? colorStr : mapColorNameToHex(colorStr)));
            } catch (Exception e) {
                shape.setColor(Color.LTGRAY);
            }

            // Default stroke
            shape.setStroke((int) (2 * density), Color.parseColor("#DDDDDD"));
            dot.setBackground(shape);

            dot.setOnClickListener(v -> {
                // Reset previous selection
                if (selectedDot[0] != null) {
                    ((GradientDrawable) selectedDot[0].getBackground()).setStroke((int) (2 * density), Color.parseColor("#DDDDDD"));
                }

                // Set new selection
                shape.setStroke((int) (3 * density), Color.BLACK);
                selectedDot[0] = dot;
                selectedColor = colorStr;
                binding.txtSelectedColorLabel.setText(String.format("Màu sắc: %s", colorStr));
            });

            binding.layoutColorsDetail.addView(dot);

            // Select first color by default
            if (i == 0) {
                dot.performClick();
            }
        }
    }

    private String mapColorNameToHex(String name) {
        if (name == null) return "#E0E0E0";
        String colorName = name.toLowerCase().trim();

        // Basic Colors
        if (colorName.contains("trắng") || colorName.contains("white")) return "#FFFFFF";
        if (colorName.contains("đen") || colorName.contains("black")) return "#000000";
        if (colorName.contains("xám") || colorName.contains("gray") || colorName.contains("grey") || colorName.contains("ghi")) return "#808080";
        
        // Metallics
        if (colorName.contains("bạc") || colorName.contains("silver")) return "#C0C0C0";
        if (colorName.contains("vàng đồng") || colorName.contains("gold")) return "#D4AF37";
        
        // Blues
        if (colorName.contains("navy") || colorName.contains("xanh than") || colorName.contains("than")) return "#000080";
        if (colorName.contains("sky") || colorName.contains("da trời")) return "#87CEEB";
        if (colorName.contains("xanh dương") || colorName.contains("blue") || colorName.equals("xanh")) return "#0000FF";
        if (colorName.contains("cobalt") || colorName.contains("xanh coban")) return "#0047AB";
        
        // Reds & Pinks
        if (colorName.contains("đỏ đô") || colorName.contains("burgundy") || colorName.contains("đỏ rượu")) return "#800000";
        if (colorName.contains("đỏ") || colorName.contains("red")) return "#FF0000";
        if (colorName.contains("hồng phấn") || colorName.contains("rose")) return "#FF66CC";
        if (colorName.contains("hồng") || colorName.contains("pink")) return "#FFC0CB";
        if (colorName.contains("fuchsia") || colorName.contains("hồng sen")) return "#FF00FF";
        
        // Purples
        if (colorName.contains("tím") || colorName.contains("purple") || colorName.contains("violet")) return "#800080";
        if (colorName.contains("mận") || colorName.contains("plum")) return "#8E4585";
        if (colorName.contains("lavender") || colorName.contains("oải hương")) return "#E6E6FA";
        
        // Greens
        if (colorName.contains("rêu") || colorName.contains("olive")) return "#808000";
        if (colorName.contains("xanh lá") || colorName.contains("green")) return "#008000";
        if (colorName.contains("cốm") || colorName.contains("mint")) return "#98FF98";
        if (colorName.contains("xanh ngọc") || colorName.contains("teal") || colorName.contains("turquoise")) return "#008080";
        
        // Browns & Earth Tones
        if (colorName.contains("nâu") || colorName.contains("brown") || colorName.contains("bò")) return "#A52A2A";
        if (colorName.contains("be") || colorName.contains("beige") || colorName.contains("kem") || colorName.contains("cream")) return "#F5F5DC";
        if (colorName.contains("khaki") || colorName.contains("cát")) return "#C3B091";
        if (colorName.contains("nâu đất") || colorName.contains("terracotta")) return "#E2725B";
        
        // Oranges & Yellows
        if (colorName.contains("cam") || colorName.contains("orange")) return "#FFA500";
        if (colorName.contains("gạch") || colorName.contains("brick")) return "#B22222";
        if (colorName.contains("vàng") || colorName.contains("yellow")) return "#FFFF00";
        if (colorName.contains("mù tạt") || colorName.contains("mustard")) return "#FFDB58";
        
        // Patterns & Multi
        if (colorName.contains("đa sắc") || colorName.contains("nhiều màu") || colorName.contains("multi")) return "#CCCCCC";
        if (colorName.contains("họa tiết") || colorName.contains("hoa") || colorName.contains("caro")) return "#F0F0F0";

        return "#E0E0E0";
    }

    private void setupSizes(Product product) {
        binding.chipGroupSizes.removeAllViews();
        
        java.util.List<String> sizeNames = new java.util.ArrayList<>();
        
        // 1. Lấy từ variants (nguồn chính xác về tồn kho)
        if (product.getVariants() != null) {
            for (com.project.models.ProductVariant v : product.getVariants()) {
                if (v.getSize() != null && !sizeNames.contains(v.getSize())) {
                    sizeNames.add(v.getSize());
                }
            }
        }
        
        // 2. Lấy bổ sung từ sizes (để hiện các size đã hết hàng hoàn toàn, không có trong variants)
        if (product.getSizes() != null) {
            for (Product.ProductSize ps : product.getSizes()) {
                if (ps.getSize() != null && !sizeNames.contains(ps.getSize())) {
                    sizeNames.add(ps.getSize());
                }
            }
        }

        if (sizeNames.isEmpty()) {
            binding.txtSelectedSizeLabel.setVisibility(View.GONE);
            return;
        }

        // Sắp xếp kích cỡ để hiển thị đúng thứ tự (ví dụ: 36, 37, 38 hoặc S, M, L)
        try {
            sizeNames.sort((s1, s2) -> {
                try {
                    Double d1 = Double.parseDouble(s1.replaceAll("[^0-9.]", ""));
                    Double d2 = Double.parseDouble(s2.replaceAll("[^0-9.]", ""));
                    return d1.compareTo(d2);
                } catch (Exception e) {
                    // Logic sắp xếp cho S, M, L, XL
                    String order = "XXS XS S M L XL XXL 2XL 3XL";
                    int i1 = order.indexOf(s1.toUpperCase());
                    int i2 = order.indexOf(s2.toUpperCase());
                    if (i1 != -1 && i2 != -1) return Integer.compare(i1, i2);
                    return s1.compareTo(s2);
                }
            });
        } catch (Exception ignored) {}

        binding.txtSelectedSizeLabel.setVisibility(View.VISIBLE);
        binding.txtSelectedSizeLabel.setText("Kích cỡ");

        int firstAvailableId = -1;

        for (String sizeName : sizeNames) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_variant, binding.chipGroupSizes, false);
            chip.setText(sizeName);
            
            int chipId = View.generateViewId();
            chip.setId(chipId);

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedSize = sizeName;
                    binding.txtSelectedSizeLabel.setText(String.format("Kích cỡ: %s", sizeName));
                }
            });

            // Kiểm tra xem size này có còn hàng không (trong bất kỳ variant nào)
            boolean hasStock = false;
            if (product.getVariants() != null) {
                for (com.project.models.ProductVariant v : product.getVariants()) {
                    if (sizeName.equals(v.getSize()) && v.getQuantity() > 0) {
                        hasStock = true;
                        break;
                    }
                }
            }

            if (!hasStock) {
                chip.setEnabled(false);
                chip.setAlpha(0.3f);
                chip.setCheckable(false); // Không cho phép chọn nếu hết hàng
            } else {
                if (firstAvailableId == -1) {
                    firstAvailableId = chipId;
                }
            }

            binding.chipGroupSizes.addView(chip);
        }

        // Auto-select first available size
        if (firstAvailableId != -1) {
            final int idToCheck = firstAvailableId;
            binding.chipGroupSizes.post(() -> {
                binding.chipGroupSizes.check(idToCheck);
            });
        }
    }

    private void setupCollapsibleSections() {
        binding.btnToggleSizeGuide.setOnClickListener(v -> {
            boolean isVisible = binding.imgSizeGuideContent.getVisibility() == View.VISIBLE;
            binding.imgSizeGuideContent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            binding.imgSizeGuideArrow.setRotation(isVisible ? 0 : 90);
        });
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + " VNĐ";
    }

    private void setupActionButtons() {
        binding.btnAddToCart.setOnClickListener(v -> {
            if (currentProduct == null) return;

            if (!selectedColor.isEmpty() && !selectedSize.isEmpty()) {
                // Add directly if already selected
                CartManager.getInstance(this).addToCart(currentProduct, selectedColor, selectedSize, 1, new CartManager.CartCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(ProductDetailActivity.this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(ProductDetailActivity.this, "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Otherwise show bottom sheet
                ProductVariantBottomSheetFragment variantSheet = new ProductVariantBottomSheetFragment(currentProduct, selectedColor, selectedSize, 1);
                variantSheet.setButtonText("Thêm vào giỏ hàng");
                variantSheet.setOnVariantSelectedListener((color, size, quantity) -> {
                    CartManager.getInstance(this).addToCart(currentProduct, color, size, quantity, new CartManager.CartCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(ProductDetailActivity.this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(ProductDetailActivity.this, "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
                variantSheet.show(getSupportFragmentManager(), "ProductVariantBottomSheet");
            }
        });

        binding.btnBuyNow.setOnClickListener(v -> {
            if (currentProduct == null) return;

            if (!selectedColor.isEmpty() && !selectedSize.isEmpty()) {
                navigateToCheckout(currentProduct, selectedColor, selectedSize, 1);
            } else {
                ProductVariantBottomSheetFragment variantSheet = new ProductVariantBottomSheetFragment(currentProduct, selectedColor, selectedSize, 1);
                variantSheet.setButtonText("Mua ngay");
                variantSheet.setOnVariantSelectedListener((color, size, quantity) -> {
                    navigateToCheckout(currentProduct, color, size, quantity);
                });
                variantSheet.show(getSupportFragmentManager(), "ProductVariantBottomSheet");
            }
        });

        binding.btnFavoriteDetail.setOnClickListener(v -> {
            if (currentProduct == null) return;
            currentProduct.setFavorite(!currentProduct.isFavorite());
            updateFavoriteUI(currentProduct.isFavorite());
            Toast.makeText(this, currentProduct.isFavorite() ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
        });
    }

    private void navigateToCheckout(Product product, String color, String size, int quantity) {
        // Create a copy of the product for checkout
        Product checkoutProduct = new Product();
        checkoutProduct.setId(product.getId());
        checkoutProduct.setName(product.getName());
        checkoutProduct.setPrice(product.getPrice());
        checkoutProduct.setDiscountPrice(product.getDiscountPrice());
        checkoutProduct.setImages(product.getImages());
        checkoutProduct.setQuantity(quantity);

        // Set only the selected variant for correct display in Checkout adapter
        ProductVariant selectedVariant = new ProductVariant();
        selectedVariant.setColor(color);
        selectedVariant.setSize(size);
        List<ProductVariant> variants = new ArrayList<>();
        variants.add(selectedVariant);
        checkoutProduct.setVariants(variants);

        ArrayList<Product> productList = new ArrayList<>();
        productList.add(checkoutProduct);

        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putParcelableArrayListExtra("products", productList);
        startActivity(intent);
    }
}