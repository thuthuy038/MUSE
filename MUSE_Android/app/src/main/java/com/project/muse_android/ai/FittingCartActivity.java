package com.project.muse_android.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.EdgeToEdge;

import com.bumptech.glide.Glide;
import com.project.models.Product;
import com.project.models.ProductVariant;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityFittingCartBinding;
import com.project.utils.CartManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FittingCartActivity extends AppCompatActivity {
    
    private ActivityFittingCartBinding binding;
    private final List<Product> cartList = new ArrayList<>();
    private OtherCartItemsAdapter adapter;
    private Product currentProduct;

    private String originalColor = "";
    private String originalSize = "";
    private int originalQuantity = 1;
    private String selectedColor = "";
    private String selectedSize = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityFittingCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        com.project.utils.ViewUtils.applySystemBarsPadding(binding.layoutHeader, true, false);
        binding.btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        binding.rvOtherCartItems.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new OtherCartItemsAdapter(cartList);
        binding.rvOtherCartItems.setAdapter(adapter);

        // Load database cart items
        loadCartItems();

        // Setup photo room action
        binding.btnGoToPhotoRoom.setOnClickListener(v -> {
            if (currentProduct != null) {
                Intent intent = new Intent(this, VirtualFittingActivity.class);
                intent.putExtra("product_id", currentProduct.get_id() != null ? currentProduct.get_id() : currentProduct.getId());
                intent.putExtra("size", selectedSize);
                intent.putExtra("color", selectedColor);
                intent.putExtra("from_cart", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Giỏ hàng của bạn đang trống.", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup update cart item action
        binding.btnUpdateCart.setOnClickListener(v -> updateCartItemVariant());

        // Setup Favorite button action
        binding.btnFavorite.setOnClickListener(v -> {
            if (currentProduct != null) {
                currentProduct.setFavorite(!currentProduct.isFavorite());
                updateFavoriteUI(currentProduct.isFavorite());
                Toast.makeText(this, currentProduct.isFavorite() 
                        ? "Đã thêm vào mục yêu thích" 
                        : "Đã xóa khỏi mục yêu thích", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnAiAgent.setOnClickListener(v -> navigateToAiHub());

        // Setup Bottom Navigation
        com.project.utils.ViewUtils.setupBottomNavigation(binding.bottomNavigationView, this);
    }

    private void navigateToAiHub() {
        Intent intent = new Intent(this, com.project.muse_android.main.MainActivity.class);
        intent.putExtra("open_ai_hub", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void loadCartItems() {
        CartManager.getInstance(this).getCartItems(new CartManager.CartCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> result) {
                runOnUiThread(() -> {
                    cartList.clear();
                    if (result != null && !result.isEmpty()) {
                        cartList.addAll(result);
                        adapter.notifyDataSetChanged();
                        // Display the first product by default
                        displayProduct(cartList.get(0));
                    } else {
                        Toast.makeText(FittingCartActivity.this, "Không có sản phẩm nào trong giỏ hàng.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(FittingCartActivity.this, "Lỗi tải giỏ hàng: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void displayProduct(Product product) {
        currentProduct = product;
        binding.tvProductName.setText(product.getName());
        
        double price = product.getDiscountPrice() != null && product.getDiscountPrice() > 0 
                ? product.getDiscountPrice() : product.getPrice();
        binding.tvProductPrice.setText(formatPrice(price));

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            originalColor = product.getVariants().get(0).getColor();
            originalSize = product.getVariants().get(0).getSize();
            originalQuantity = product.getVariants().get(0).getQuantity();
        } else {
            originalColor = "";
            originalSize = "";
            originalQuantity = product.getQuantity() > 0 ? product.getQuantity() : 1;
        }

        selectedColor = originalColor;
        selectedSize = originalSize;

        updateFavoriteUI(product.isFavorite());

        // Load preview image
        String imageUrl = "";
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imageUrl = product.getImages().get(0).getUrl();
        }
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (!imageUrl.startsWith("http")) {
                imageUrl = "https://server-testing-ymn9.onrender.com" + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
            }
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.demo_product)
                    .into(binding.ivProductPreview);
        }

        // Fetch full product details to get all colors and sizes
        String prodId = product.getId() != null ? product.getId() : product.get_id();
        com.project.network.ApiClient.INSTANCE.getInstance().getProductDetail(prodId).enqueue(new retrofit2.Callback<Product>() {
            @Override
            public void onResponse(retrofit2.Call<Product> call, retrofit2.Response<Product> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Product fullProduct = response.body();
                    runOnUiThread(() -> {
                        product.setColors(fullProduct.getColors());
                        product.setSizes(fullProduct.getSizes());
                        product.setVariants(fullProduct.getVariants());
                        
                        setupColorSelector(product);
                        setupSizeSelector(product);
                    });
                } else {
                    runOnUiThread(() -> {
                        setupColorSelector(product);
                        setupSizeSelector(product);
                    });
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Product> call, Throwable t) {
                runOnUiThread(() -> {
                    setupColorSelector(product);
                    setupSizeSelector(product);
                });
            }
        });
    }

    private void setupSizeSelector(Product product) {
        binding.layoutSizes.removeAllViews();
        List<String> sizeList = new ArrayList<>();

        if (product.getSizes() != null) {
            for (Product.ProductSize ps : product.getSizes()) {
                if (ps.getSize() != null && !sizeList.contains(ps.getSize())) {
                    sizeList.add(ps.getSize());
                }
            }
        }
        if (sizeList.isEmpty() && product.getVariants() != null) {
            for (ProductVariant pv : product.getVariants()) {
                if (pv.getSize() != null && !sizeList.contains(pv.getSize())) {
                    sizeList.add(pv.getSize());
                }
            }
        }
        if (sizeList.isEmpty()) {
            sizeList.add("S");
            sizeList.add("M");
            sizeList.add("L");
        }

        float density = getResources().getDisplayMetrics().density;
        for (String size : sizeList) {
            TextView tv = new TextView(this);
            tv.setText(size);
            tv.setGravity(android.view.Gravity.CENTER);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (48 * density), (int) (48 * density));
            params.setMargins(0, 0, (int) (12 * density), 0);
            tv.setLayoutParams(params);

            tv.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.montserrat));

            if (size.equalsIgnoreCase(selectedSize)) {
                tv.setBackgroundResource(R.drawable.bg_chip_pink);
                tv.setTextColor(getResources().getColor(R.color.primary_500));
            } else {
                tv.setBackgroundResource(R.drawable.bg_selectable_item);
                tv.setTextColor(android.graphics.Color.parseColor("#333333"));
            }

            tv.setOnClickListener(v -> {
                selectedSize = size;
                setupSizeSelector(product);
            });

            binding.layoutSizes.addView(tv);
        }
    }

    private void setupColorSelector(Product product) {
        binding.layoutColors.removeAllViews();
        List<String> colorList = new ArrayList<>();

        if (product.getColors() != null) {
            for (String c : product.getColors()) {
                if (c != null && !colorList.contains(c)) {
                    colorList.add(c);
                }
            }
        }
        if (colorList.isEmpty() && product.getVariants() != null) {
            for (ProductVariant pv : product.getVariants()) {
                if (pv.getColor() != null && !colorList.contains(pv.getColor())) {
                    colorList.add(pv.getColor());
                }
            }
        }
        if (colorList.isEmpty()) {
            colorList.add("Trắng");
            colorList.add("Đen");
            colorList.add("Hồng");
        }

        float density = getResources().getDisplayMetrics().density;
        for (String color : colorList) {
            View view = new View(this);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (32 * density), (int) (32 * density));
            params.setMargins(0, 0, (int) (16 * density), 0);
            view.setLayoutParams(params);

            int colorInt = android.graphics.Color.TRANSPARENT;
            try {
                if (color.startsWith("#")) {
                    colorInt = android.graphics.Color.parseColor(color);
                } else {
                    colorInt = android.graphics.Color.parseColor(mapColorNameToHex(color));
                }
            } catch (Exception e) {
                colorInt = android.graphics.Color.LTGRAY;
            }

            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            shape.setColor(colorInt);

            if (color.equalsIgnoreCase(selectedColor)) {
                shape.setStroke((int) (3 * density), getResources().getColor(R.color.primary_500));
            } else {
                shape.setStroke((int) (1 * density), android.graphics.Color.parseColor("#DDDDDD"));
            }
            view.setBackground(shape);

            view.setOnClickListener(v -> {
                selectedColor = color;
                setupColorSelector(product);
            });

            binding.layoutColors.addView(view);
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

    private void updateCartItemVariant() {
        if (currentProduct == null) {
            Toast.makeText(this, "Không có sản phẩm để cập nhật.", Toast.LENGTH_SHORT).show();
            return;
        }

        String prodId = currentProduct.getId() != null ? currentProduct.getId() : currentProduct.get_id();
        
        // If variant selection remains unchanged, show message and return
        if (selectedColor.equalsIgnoreCase(originalColor) && selectedSize.equalsIgnoreCase(originalSize)) {
            Toast.makeText(this, "Bạn chưa thay đổi màu sắc hoặc kích thước.", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = currentProduct.getDiscountPrice() != null && currentProduct.getDiscountPrice() > 0 
                ? currentProduct.getDiscountPrice() : currentProduct.getPrice();

        CartManager.getInstance(this).updateCartItemVariant(prodId, originalColor, originalSize, selectedColor, selectedSize, price, new CartManager.CartCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    Toast.makeText(FittingCartActivity.this, "Cập nhật giỏ hàng thành công! 🛒", Toast.LENGTH_SHORT).show();
                    // Reload cart items from database to show updated selections
                    loadCartItems();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(FittingCartActivity.this, "Lỗi cập nhật giỏ hàng: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateFavoriteUI(boolean isFavorite) {
        if (isFavorite) {
            binding.btnFavorite.setIconResource(R.drawable.ic_favorite_filled);
            binding.btnFavorite.setIconTint(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#B30737")));
        } else {
            binding.btnFavorite.setIconResource(R.drawable.ic_favorite);
            binding.btnFavorite.setIconTint(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#333333")));
        }
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + " VNĐ";
    }

    private class OtherCartItemsAdapter extends RecyclerView.Adapter<OtherCartItemsAdapter.ViewHolder> {
        private final List<Product> items;
        private int selectedPos = 0;

        public OtherCartItemsAdapter(List<Product> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fitting_cart_other, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Product product = items.get(position);
            
            // Load image using Glide
            String imageUrl = "";
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                imageUrl = product.getImages().get(0).getUrl();
            }
            if (imageUrl != null && !imageUrl.isEmpty()) {
                if (!imageUrl.startsWith("http")) {
                    imageUrl = "https://server-testing-ymn9.onrender.com" + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
                }
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.demo_product)
                        .into(holder.ivProductImage);
            }

            // Set stroke color for selected item
            com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) holder.itemView.findViewById(R.id.cardContainer);
            if (position == selectedPos) {
                card.setStrokeColor(android.graphics.Color.parseColor("#B30737")); // primary_500
                card.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
            } else {
                card.setStrokeColor(android.graphics.Color.parseColor("#EEEEEE"));
                card.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
            }

            holder.itemView.setOnClickListener(v -> {
                int oldPos = selectedPos;
                selectedPos = holder.getBindingAdapterPosition();
                notifyItemChanged(oldPos);
                notifyItemChanged(selectedPos);
                displayProduct(product);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivProductImage;
            ViewHolder(View itemView) {
                super(itemView);
                ivProductImage = itemView.findViewById(R.id.ivProductImage);
            }
        }
    }
}
