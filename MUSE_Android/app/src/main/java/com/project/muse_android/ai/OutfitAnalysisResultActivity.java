package com.project.muse_android.ai;

import android.content.Intent;
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
import com.project.models.GeminiResponse;
import com.project.network.GeminiClient;
import com.project.muse_android.BuildConfig;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityOutfitResultBinding;
import com.project.network.HomeApiClient;
import com.project.utils.CartManager;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutfitAnalysisResultActivity extends AppCompatActivity {

    private ActivityOutfitResultBinding binding;
    private String mode = "scanner"; // "tryon" or "scanner"
    private String imagePath = "";
    private String productId = "";
    private String selectedSize = "";
    private String selectedColor = "";
    private final List<Product> recommendedProducts = new ArrayList<>();
    private RecommendedProductsAdapter adapter;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOutfitResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        com.project.utils.ViewUtils.applySystemBarsPadding(binding.layoutHeader, true, false);
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAiAgent.setOnClickListener(v -> navigateToAiHub());

        // Read extras
        Intent intent = getIntent();
        if (intent != null) {
            mode = intent.getStringExtra("mode");
            if (mode == null) mode = "scanner";
            imagePath = intent.getStringExtra("image_path");
            productId = intent.getStringExtra("product_id");
            selectedSize = intent.getStringExtra("size");
            selectedColor = intent.getStringExtra("color");
        }

        // Setup UI base states
        setupUIStates();

        // Load preview image
        loadPreviewImage();

        // Setup RecyclerView
        binding.rvRecommendedProducts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new RecommendedProductsAdapter(recommendedProducts);
        binding.rvRecommendedProducts.setAdapter(adapter);

        // Fetch products and load recommendations
        loadProductsAndRecommendations();

        // Run Gemini API for Real Outfit Scanning if in scanner mode
        if ("scanner".equals(mode)) {
            runGeminiAnalysis();
        } else {
            // In Try-On mode, show realistic high scores for the chosen product
            showTryOnScores();
        }

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

    private void setupUIStates() {
        if ("tryon".equals(mode)) {
            binding.tvTitle.setText("KẾT QUẢ THỬ ĐỒ");
            binding.tvModeBadge.setText("MUSE VIRTUAL TRY-ON ✨");
            binding.btnResultAction.setText("THÊM OUTFIT NÀY VÀO GIỎ HÀNG");
            binding.btnSaveOutfit.setText("LƯU BỘ PHỐI ĐỒ NÀY");
        } else {
            binding.tvTitle.setText("KẾT QUẢ PHÂN TÍCH");
            binding.tvModeBadge.setText("MUSE OUTFIT SCANNER 🔍");
            binding.btnResultAction.setText("THÊM VÀO TỦ ĐỒ CÁ NHÂN");
            binding.btnSaveOutfit.setText("LƯU KẾT QUẢ QUÉT");
        }
    }

    private void loadPreviewImage() {
        if (imagePath != null && !imagePath.isEmpty()) {
            Glide.with(this)
                    .load(imagePath)
                    .placeholder(R.drawable.demo_product)
                    .into(binding.ivResultImage);
        } else {
            binding.ivResultImage.setImageResource(R.drawable.demo_product);
        }
    }

    private void setupActionButtons() {
        binding.btnResultAction.setOnClickListener(v -> {
            if ("tryon".equals(mode) && currentProduct != null) {
                // Add the garment to cart
                CartManager.getInstance(this).addToCart(currentProduct, selectedColor, selectedSize, 1, new CartManager.CartCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> {
                            Toast.makeText(OutfitAnalysisResultActivity.this, "Đã thêm set đồ vào giỏ hàng! 🛒", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(OutfitAnalysisResultActivity.this, "Lỗi thêm giỏ hàng: " + message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                Toast.makeText(this, "Đã thêm vào tủ đồ cá nhân!", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnSaveOutfit.setOnClickListener(v -> {
            Toast.makeText(this, "Đã lưu bộ phối đồ vào Nhật ký AI! ✨", Toast.LENGTH_SHORT).show();
        });
    }

    private void showTryOnScores() {
        // Mock realistic high scores for try-on garments
        binding.pbSchool.setProgress(85);
        binding.tvSchoolScore.setText("85đ");
        binding.pbWork.setProgress(70);
        binding.tvWorkScore.setText("70đ");
        binding.pbDate.setProgress(90);
        binding.tvDateScore.setText("90đ");
        binding.pbParty.setProgress(75);
        binding.tvPartyScore.setText("75đ");
        binding.pbTravel.setProgress(95);
        binding.tvTravelScore.setText("95đ");

        binding.tvStyleName.setText("PREMIUM FITTING");
        binding.tvStylistAdvice.setText("Set đồ này cực kỳ hợp thời trang và tôn dáng. Bạn nên kết hợp với một vài phụ kiện tối giản như kính râm và một đôi sneaker trắng trẻ trung năng động.");
    }

    private void runGeminiAnalysis() {
        String base64 = getBase64FromImagePath(imagePath);
        if (base64.isEmpty()) {
            Toast.makeText(this, "Không thể đọc dữ liệu ảnh, đang dùng chế độ giả lập.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Construct request body
        Map<String, Object> body = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> contentMap = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();

        // Text Prompt part
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", "Hãy phân tích hình ảnh trang phục của người này. Trả về kết quả dưới định dạng JSON có cấu trúc chính xác như sau:\n" +
                "{\n" +
                "  \"style\": \"Tên phong cách (ví dụ: Casual, Elegant, Streetwear)\",\n" +
                "  \"school_score\": 80,\n" +
                "  \"work_score\": 90,\n" +
                "  \"date_score\": 75,\n" +
                "  \"party_score\": 60,\n" +
                "  \"travel_score\": 85,\n" +
                "  \"fashion_advice\": \"Lời khuyên thời trang chi tiết\"\n" +
                "}\n" +
                "Lưu ý: Chỉ trả về chuỗi JSON thô, không kèm định dạng markdown ```json.");
        parts.add(textPart);

        // Image inlineData part
        Map<String, Object> imagePart = new HashMap<>();
        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mimeType", "image/jpeg");
        inlineData.put("data", base64);
        imagePart.put("inlineData", inlineData);
        parts.add(imagePart);

        contentMap.put("parts", parts);
        contents.add(contentMap);
        body.put("contents", contents);

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        body.put("generationConfig", generationConfig);

        // API Call
        String apiKey = BuildConfig.GEMINI_API_KEY;
        GeminiClient.getClient().generateContent(apiKey, body).enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String rawText = response.body().getText().trim();
                        // Clean markdown blocks if present
                        if (rawText.startsWith("```json")) {
                            rawText = rawText.substring(7);
                        }
                        if (rawText.endsWith("```")) {
                            rawText = rawText.substring(0, rawText.length() - 3);
                        }
                        rawText = rawText.trim();
                        
                        JSONObject json = new JSONObject(rawText);
                        
                        // Update UI on main thread
                        String style = json.optString("style", "CASUAL");
                        int school = json.optInt("school_score", 80);
                        int work = json.optInt("work_score", 75);
                        int date = json.optInt("date_score", 85);
                        int party = json.optInt("party_score", 65);
                        int travel = json.optInt("travel_score", 90);
                        String advice = json.optString("fashion_advice", "");

                        runOnUiThread(() -> {
                            binding.tvStyleName.setText(style.toUpperCase());
                            binding.pbSchool.setProgress(school);
                            binding.tvSchoolScore.setText(school + "đ");
                            binding.pbWork.setProgress(work);
                            binding.tvWorkScore.setText(work + "đ");
                            binding.pbDate.setProgress(date);
                            binding.tvDateScore.setText(date + "đ");
                            binding.pbParty.setProgress(party);
                            binding.tvPartyScore.setText(party + "đ");
                            binding.pbTravel.setProgress(travel);
                            binding.tvTravelScore.setText(travel + "đ");
                            if (!advice.isEmpty()) {
                                binding.tvStylistAdvice.setText(advice);
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                // Fail silently or fallback to mock
            }
        });
    }

    private String getBase64FromImagePath(String path) {
        try {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(path);
            if (bitmap == null) return "";
            // Resize to keep the base64 string lightweight
            android.graphics.Bitmap resized = android.graphics.Bitmap.createScaledBitmap(
                    bitmap, 600, 800, true
            );
            java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
            resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void loadProductsAndRecommendations() {
        HomeApiClient.getHomeApiService().searchProducts("").enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Product> list = new ArrayList<>(response.body());
                    java.util.Collections.shuffle(list);
                    
                    // Filter recommendations
                    recommendedProducts.clear();
                    int count = 0;
                    for (Product p : list) {
                        // Pick 3 random active items for styling recommendation
                        if (p.getStatus() == null || "active".equalsIgnoreCase(p.getStatus())) {
                            recommendedProducts.add(p);
                            count++;
                            if (count >= 3) break;
                        }
                    }
                    adapter.notifyDataSetChanged();

                    // Find current product if in Try-On mode
                    if (productId != null && !productId.isEmpty()) {
                        for (Product p : list) {
                            String pid = p.get_id() != null ? p.get_id() : p.getId();
                            if (productId.equals(pid)) {
                                currentProduct = p;
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                // Fail silently
            }
        });
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
                Intent detailIntent = new Intent(OutfitAnalysisResultActivity.this, com.project.muse_android.product.ProductDetailActivity.class);
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
