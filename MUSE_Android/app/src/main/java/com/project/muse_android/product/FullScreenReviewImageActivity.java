package com.project.muse_android.product;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.project.models.Product;
import com.project.models.ProductReview;
import com.project.muse_android.R;
import com.project.muse_android.cart.ProductVariantBottomSheetFragment;
import com.project.muse_android.databinding.ActivityFullScreenReviewImageBinding;
import com.project.network.HomeApiClient;
import com.project.utils.CartManager;
import com.project.utils.ViewUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FullScreenReviewImageActivity extends AppCompatActivity {

    private ActivityFullScreenReviewImageBinding binding;
    private List<String> images;
    private int startPosition;
    private String productId;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityFullScreenReviewImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Đẩy các thành phần để không bị che bởi Status Bar và Navigation Bar
        ViewUtils.applySystemBarsPadding(binding.btnBack, true, false);
        ViewUtils.applySystemBarsPadding(binding.txtImageIndicator, true, false);
        ViewUtils.applySystemBarsPadding(binding.txtDate, false, true);
        ViewUtils.applySystemBarsPadding(binding.cardProduct, false, true);

        images = getIntent().getStringArrayListExtra("images");
        startPosition = getIntent().getIntExtra("position", 0);
        productId = getIntent().getStringExtra("product_id");
        boolean isReview = getIntent().getBooleanExtra("is_review", true);

        String userName = getIntent().getStringExtra("user_name");
        String userAvatar = getIntent().getStringExtra("user_avatar");
        int rating = getIntent().getIntExtra("rating", 0);
        String comment = getIntent().getStringExtra("comment");
        String variant = getIntent().getStringExtra("variant");
        String date = getIntent().getStringExtra("date");
        int helpfulCount = getIntent().getIntExtra("helpful_count", 0);

        binding.btnBack.setOnClickListener(v -> finish());

        setupViewPager();

        if (isReview) {
            displayReviewInfo(userName, userAvatar, rating, comment, variant, date, helpfulCount);

            if (productId != null) {
                loadProductInfo();
            } else {
                binding.cardProduct.setVisibility(View.GONE);
            }
            setupActionButtons();
        } else {
            // Hide all review-related overlays for pure product image viewing
            binding.layoutInfoOverlay.setVisibility(View.GONE);
            binding.cardProduct.setVisibility(View.GONE);
            binding.imgUserAvatarSmall.setVisibility(View.GONE);
            binding.btnLike.setVisibility(View.GONE);
            binding.txtDate.setVisibility(View.GONE);
            
            // Hide the "More" button container (which is a parent of the sidebar children)
            // In the layout, imgUserAvatarSmall, btnLike, and the "More" container are siblings
            View sidebar = (View) binding.imgUserAvatarSmall.getParent();
            if (sidebar instanceof android.view.ViewGroup) {
                android.view.ViewGroup group = (android.view.ViewGroup) sidebar;
                if (group.getChildCount() >= 3) {
                    group.getChildAt(2).setVisibility(View.GONE);
                }
            }
        }
    }

    private void setupViewPager() {
        if (images == null || images.isEmpty()) return;

        binding.viewPagerImages.setAdapter(new ImageAdapter(images));
        binding.viewPagerImages.setCurrentItem(startPosition, false);

        updateIndicator(startPosition);

        binding.viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicator(position);
            }
        });
    }

    private void updateIndicator(int position) {
        binding.txtImageIndicator.setText(String.format(Locale.getDefault(), "%d/%d", position + 1, images.size()));
    }

    private void displayReviewInfo(String name, String avatar, int rating, String comment, String variant, String date, int helpfulCount) {
        binding.txtUserName.setText(name != null ? "@" + name.replace(" ", "").toLowerCase() : "@user");
        binding.txtComment.setText(comment);
        binding.txtVariantInfo.setText(String.format("Phân loại: %s", (variant != null ? variant : "-")));

        // Format date if needed, but keeping it simple for now
        binding.txtDate.setText(date);
        binding.txtLikeCount.setText(String.valueOf(helpfulCount));

        // Display stars
        binding.layoutStars.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < 5; i++) {
            ImageView star = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) (14 * density), (int) (14 * density));
            if (i > 0) params.setMarginStart((int) (2 * density));
            star.setLayoutParams(params);
            star.setImageResource(R.drawable.star);
            star.setColorFilter(i < rating ? Color.parseColor("#FFD93D") : Color.parseColor("#4DFFFFFF"));
            binding.layoutStars.addView(star);
        }

        // User Avatar matching review list style
        if (avatar != null && !avatar.isEmpty()) {
            if (avatar.contains("localhost:3000")) {
                avatar = avatar.replace("http://localhost:3000", "https://server-testing-ymn9.onrender.com");
            } else if (!avatar.startsWith("http")) {
                avatar = "https://server-testing-ymn9.onrender.com" + (avatar.startsWith("/") ? "" : "/") + avatar;
            }
            Glide.with(this).load(avatar).circleCrop().into(binding.imgUserAvatarSmall);
        }
    }

    private void loadProductInfo() {
        HomeApiClient.getApiService().getProductDetail(productId).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(@NonNull Call<Product> call, @NonNull Response<Product> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentProduct = response.body();
                    binding.cardProduct.setVisibility(View.VISIBLE);
                    binding.txtProductName.setText(currentProduct.getName());
                    binding.txtProductPrice.setText(String.format(Locale.getDefault(), "%,.0f VNĐ", currentProduct.getFinalPrice()));

                    if (currentProduct.getImages() != null && !currentProduct.getImages().isEmpty()) {
                        String url = currentProduct.getImages().get(0).getUrl();
                        if (url != null && url.startsWith("/")) url = "https://server-testing-ymn9.onrender.com" + url;
                        Glide.with(FullScreenReviewImageActivity.this).load(url).into(binding.imgProductThumb);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Product> call, @NonNull Throwable t) {
                binding.cardProduct.setVisibility(View.GONE);
            }
        });
    }

    private void setupActionButtons() {
        binding.btnCart.setOnClickListener(v -> {
            if (currentProduct == null) return;
            ProductVariantBottomSheetFragment bottomSheet = new ProductVariantBottomSheetFragment(currentProduct);
            bottomSheet.setButtonText("Thêm vào giỏ hàng");
            bottomSheet.setOnVariantSelectedListener((color, size, quantity) -> {
                CartManager.getInstance(this).addToCart(currentProduct, color, size, quantity, new CartManager.CartCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(FullScreenReviewImageActivity.this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(FullScreenReviewImageActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            });
            bottomSheet.show(getSupportFragmentManager(), "variant_bottom_sheet");
        });

        binding.btnBuyNow.setOnClickListener(v -> {
            if (currentProduct == null) return;
            ProductVariantBottomSheetFragment bottomSheet = new ProductVariantBottomSheetFragment(currentProduct);
            bottomSheet.setButtonText("Mua ngay");
            bottomSheet.setOnVariantSelectedListener((color, size, quantity) -> {
                // Navigate to CheckoutActivity directly as requested
                Intent intent = new Intent(this, com.project.muse_android.checkout.CheckoutActivity.class);
                intent.putExtra("product", (android.os.Parcelable) currentProduct);
                intent.putExtra("selectedColor", color);
                intent.putExtra("selectedSize", size);
                intent.putExtra("quantity", quantity);
                startActivity(intent);
            });
            bottomSheet.show(getSupportFragmentManager(), "variant_bottom_sheet");
        });

        binding.cardProduct.setOnClickListener(v -> {
            if (productId != null) {
                Intent intent = new Intent(this, ProductDetailActivity.class);
                intent.putExtra("product_id", productId);
                startActivity(intent);
            }
        });
    }

    private static class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
        private final List<String> images;

        ImageAdapter(List<String> images) {
            this.images = images;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String url = images.get(position);
            if (url != null && url.startsWith("/")) url = "https://server-testing-ymn9.onrender.com" + url;
            Glide.with(holder.itemView.getContext()).load(url).into((ImageView) holder.itemView);
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
