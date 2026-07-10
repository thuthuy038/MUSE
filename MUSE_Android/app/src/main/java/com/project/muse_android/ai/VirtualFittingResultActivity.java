package com.project.muse_android.ai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.models.Product;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityVirtualFittingResultBinding;
import com.project.network.HomeApiClient;
import com.project.utils.CartManager;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VirtualFittingResultActivity extends AppCompatActivity {

    private ActivityVirtualFittingResultBinding binding;
    private String imagePath = "";
    private String productId = "";
    private String selectedSize = "";
    private String selectedColor = "";
    private boolean fromCart = false;
    private final List<Product> recommendedProducts = new ArrayList<>();
    private RecommendedProductsAdapter adapter;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVirtualFittingResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        com.project.utils.ViewUtils.applySystemBarsPadding(binding.layoutHeader, true, false);
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAiAgent.setOnClickListener(v -> navigateToAiHub());

        // Read extras
        Intent intent = getIntent();
        if (intent != null) {
            imagePath = intent.getStringExtra("image_path");
            productId = intent.getStringExtra("product_id");
            selectedSize = intent.getStringExtra("size");
            selectedColor = intent.getStringExtra("color");
            fromCart = intent.getBooleanExtra("from_cart", false);
        }

        // Show/Hide Add to Cart based on entry flow
        if (fromCart) {
            binding.btnAddToCart.setVisibility(View.GONE);
        } else {
            binding.btnAddToCart.setVisibility(View.VISIBLE);
        }

        // Load preview image and overlay clothes
        loadPreviewImage();

        // Setup RecyclerView
        binding.rvRecommendedProducts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new RecommendedProductsAdapter(recommendedProducts);
        binding.rvRecommendedProducts.setAdapter(adapter);

        // Fetch products and load recommendations
        loadProductsAndRecommendations();

        // Show Try-On scores
        showTryOnScores();

        // Action Buttons Setup
        setupActionButtons();
    }

    private void navigateToAiHub() {
        Intent intent = new Intent(this, com.project.muse_android.main.MainActivity.class);
        intent.putExtra("open_ai_hub", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void loadPreviewImage() {
        if (imagePath == null || imagePath.isEmpty()) {
            binding.ivResultImage.setImageResource(R.drawable.demo_product);
            return;
        }

        // Initially show user body photo
        Glide.with(this)
                .load(imagePath)
                .placeholder(R.drawable.demo_product)
                .into(binding.ivResultImage);

        // Overlay clothes on body photo using background thread
        if (productId != null && !productId.isEmpty()) {
            HomeApiClient.getHomeApiService().searchProducts("").enqueue(new Callback<List<Product>>() {
                @Override
                public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (Product p : response.body()) {
                            String pid = p.get_id() != null ? p.get_id() : p.getId();
                            if (productId.equals(pid)) {
                                currentProduct = p;
                                populateGarmentTried(p);
                                
                                // Fetch product image to blend
                                if (p.getImages() != null && !p.getImages().isEmpty()) {
                                    String imageUrl = p.getImages().get(0).getUrl();
                                    if (!imageUrl.startsWith("http")) {
                                        imageUrl = "https://server-testing-ymn9.onrender.com" + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
                                    }
                                    
                                    String finalImageUrl = imageUrl;
                                    new Thread(() -> {
                                        try {
                                            // Get body bitmap
                                            Bitmap bodyBitmap = Glide.with(VirtualFittingResultActivity.this)
                                                    .asBitmap()
                                                    .load(imagePath)
                                                    .submit()
                                                    .get();
                                                    
                                            // Get garment bitmap
                                            Bitmap garmentBitmap = Glide.with(VirtualFittingResultActivity.this)
                                                    .asBitmap()
                                                    .load(finalImageUrl)
                                                    .submit()
                                                    .get();
                                                    
                                            // Process blending
                                            Bitmap processedGarment = makeWhiteTransparent(garmentBitmap);
                                            Bitmap mergedBitmap = mergeBitmaps(bodyBitmap, processedGarment);
                                            
                                            // Save merged image back to imagePath so it archives with the overlay!
                                            try {
                                                File tempFile = File.createTempFile("tryon_merged_", ".jpg", getCacheDir());
                                                FileOutputStream out = new FileOutputStream(tempFile);
                                                mergedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                                                out.flush();
                                                out.close();
                                                imagePath = tempFile.getAbsolutePath();
                                            } catch (Exception ignored) {}

                                            runOnUiThread(() -> {
                                                binding.ivResultImage.setImageBitmap(mergedBitmap);
                                            });
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }).start();
                                }
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<Product>> call, Throwable t) {}
            });
        }
    }

    private Bitmap makeWhiteTransparent(Bitmap src) {
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];
        src.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int r = (p >> 16) & 0xff;
            int g = (p >> 8) & 0xff;
            int b = p & 0xff;

            // Make white/light grey transparent
            if (r > 230 && g > 230 && b > 230) {
                pixels[i] = Color.TRANSPARENT;
            }
        }
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    private Bitmap mergeBitmaps(Bitmap body, Bitmap garment) {
        Bitmap result = Bitmap.createBitmap(body.getWidth(), body.getHeight(), body.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(body, 0f, 0f, null);

        // Position garment in center-torso area of body image
        int gWidth = body.getWidth() * 55 / 100; // scale garment width to 55% of body width
        int gHeight = (garment.getHeight() * gWidth) / garment.getWidth();
        Bitmap scaledGarment = Bitmap.createScaledBitmap(garment, gWidth, gHeight, true);

        float left = (body.getWidth() - gWidth) / 2f;
        float top = body.getHeight() * 32 / 100; // Position on torso

        canvas.drawBitmap(scaledGarment, left, top, null);
        return result;
    }

    private void setupActionButtons() {
        binding.btnAddToCart.setOnClickListener(v -> {
            if (currentProduct != null) {
                CartManager.getInstance(this).addToCart(currentProduct, selectedColor, selectedSize, 1, new CartManager.CartCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> {
                            Toast.makeText(VirtualFittingResultActivity.this, "Đã thêm " + currentProduct.getName() + " vào giỏ hàng! 🛒", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(VirtualFittingResultActivity.this, "Lỗi thêm giỏ hàng: " + message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                Toast.makeText(this, "Không có thông tin sản phẩm.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnSaveOutfit.setOnClickListener(v -> {
            if (currentProduct != null) {
                String tryOnId = "tryon_" + (selectedColor != null ? selectedColor : "Mặc định") 
                        + "_" + (selectedSize != null ? selectedSize : "Mặc định");
                
                String productImg = "";
                if (currentProduct.getImages() != null && !currentProduct.getImages().isEmpty()) {
                    productImg = currentProduct.getImages().get(0).getUrl();
                }

                SavedOutfit saved = new SavedOutfit(
                        "Thử đồ: " + currentProduct.getName(),
                        currentProduct.getName(),
                        currentProduct.getPrice(),
                        productImg,
                        currentProduct.get_id() != null ? currentProduct.get_id() : currentProduct.getId(),
                        "Ảnh thử đồ ảo AI",
                        0.0,
                        imagePath, // Path to local merged try-on image
                        tryOnId,
                        new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new java.util.Date())
                );
                
                AiStorageManager.saveOutfit(this, saved);
                Toast.makeText(this, "Đã lưu bộ phối đồ thử vào Archival! ✨", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Chưa tải xong dữ liệu để lưu.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTryOnScores() {
        binding.pbSchool.setProgress(85);
        binding.tvSchoolScore.setText("85đ");
        binding.pbWork.setProgress(80);
        binding.tvWorkScore.setText("80đ");
        binding.pbDate.setProgress(90);
        binding.tvDateScore.setText("90đ");
        binding.pbParty.setProgress(75);
        binding.tvPartyScore.setText("75đ");
        binding.pbTravel.setProgress(95);
        binding.tvTravelScore.setText("95đ");
    }

    private void loadProductsAndRecommendations() {
        HomeApiClient.getHomeApiService().searchProducts("").enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Product> list = new ArrayList<>(response.body());
                    java.util.Collections.shuffle(list);
                    
                    // Intelligent rule-based styling recommendation
                    if (currentProduct != null) {
                        String category = currentProduct.getCategory() != null ? currentProduct.getCategory().toLowerCase() : "";
                        for (Product p : list) {
                            String pCat = p.getCategory() != null ? p.getCategory().toLowerCase() : "";
                            
                            // If trying on a top, recommend bottom/accessory/shoes
                            if (category.contains("áo") || category.contains("top") || category.contains("sơ mi") || category.contains("blazer") || category.contains("khoác")) {
                                if (pCat.contains("quần") || pCat.contains("váy") || pCat.contains("chân váy") || pCat.contains("túi") || pCat.contains("giày")) {
                                    recommendedProducts.add(p);
                                }
                            } 
                            // If trying on a bottom, recommend top/jacket/blazer
                            else if (category.contains("quần") || category.contains("váy") || category.contains("chân váy") || category.contains("đầm")) {
                                if (pCat.contains("áo") || pCat.contains("top") || pCat.contains("blazer") || pCat.contains("khoác") || pCat.contains("sơ mi")) {
                                    recommendedProducts.add(p);
                                }
                            }
                            // Fallback to match
                            else {
                                if (p.get_id() != null && !p.get_id().equals(currentProduct.get_id())) {
                                    recommendedProducts.add(p);
                                }
                            }
                            if (recommendedProducts.size() >= 3) break;
                        }
                    }

                    // Fallback to fill up recommendations if matching rule is empty
                    if (recommendedProducts.size() < 3) {
                        for (Product p : list) {
                            if (!recommendedProducts.contains(p)) {
                                String pid = p.get_id() != null ? p.get_id() : p.getId();
                                if (currentProduct == null || !pid.equals(currentProduct.get_id() != null ? currentProduct.get_id() : currentProduct.getId())) {
                                    recommendedProducts.add(p);
                                    if (recommendedProducts.size() >= 3) break;
                                }
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {}
        });
    }

    private void populateGarmentTried(Product p) {
        binding.tvGarmentName.setText(p.getName());
        binding.tvGarmentDetails.setText(
                String.format("Size %s — Màu %s", 
                        selectedSize != null && !selectedSize.isEmpty() ? selectedSize : "Mặc định", 
                        selectedColor != null && !selectedColor.isEmpty() ? selectedColor : "Mặc định")
        );

        String imageUrl = "";
        if (p.getImages() != null && !p.getImages().isEmpty()) {
            imageUrl = p.getImages().get(0).getUrl();
        }
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (!imageUrl.startsWith("http")) {
                imageUrl = "https://server-testing-ymn9.onrender.com" + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
            }
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.demo_product)
                    .into(binding.ivGarmentImage);
        }
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + " VNĐ";
    }

    private class RecommendedProductsAdapter extends RecyclerView.Adapter<RecommendedProductsAdapter.ViewHolder> {
        private final List<Product> items;

        public RecommendedProductsAdapter(List<Product> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fitting_search_result, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Product product = items.get(position);
            holder.tvProductName.setText(product.getName());
            
            double price = product.getDiscountPrice() != null && product.getDiscountPrice() > 0 
                    ? product.getDiscountPrice() : product.getPrice();
            holder.tvProductPrice.setText(formatPrice(price));

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

            // Clicking open details
            holder.itemView.setOnClickListener(v -> {
                Intent detailIntent = new Intent(VirtualFittingResultActivity.this, com.project.muse_android.product.ProductDetailActivity.class);
                detailIntent.putExtra("product_id", product.get_id() != null ? product.get_id() : product.getId());
                startActivity(detailIntent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivProductImage;
            TextView tvProductName;
            TextView tvProductPrice;

            ViewHolder(View itemView) {
                super(itemView);
                ivProductImage = itemView.findViewById(R.id.ivProductImage);
                tvProductName = itemView.findViewById(R.id.tvProductName);
                tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            }
        }
    }
}
