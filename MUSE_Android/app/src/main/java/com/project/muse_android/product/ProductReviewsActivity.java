package com.project.muse_android.product;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.adapters.ProductReviewAdapter;
import com.project.models.ReviewResponse;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityProductReviewsBinding;
import com.project.network.ApiService;
import com.project.network.HomeApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.project.models.Product;
import java.util.Locale;

public class ProductReviewsActivity extends AppCompatActivity {

    private ActivityProductReviewsBinding binding;
    private ProductReviewAdapter adapter;
    private String productId;
    private String productName;
    private double avgRating;
    private int totalReviews;
    private String currentSearchQuery = "";
    private boolean filterOnlyWithImages = false;
    private boolean isUpdatingFilters = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        productId = getIntent().getStringExtra("product_id");
        productName = getIntent().getStringExtra("product_name");
        avgRating = getIntent().getDoubleExtra("avg_rating", 0.0);
        totalReviews = getIntent().getIntExtra("total_reviews", 0);

        binding.btnBack.setOnClickListener(v -> finish());
        
        displaySummary();
        setupRecyclerView();
        setupSearch();
        setupActionButtons();

        if (productId != null) {
            loadReviews();
        }
    }

    private void displaySummary() {
        if (productName != null) {
            binding.txtProductNameHeader.setText(productName);
        }
        
        binding.txtAvgRatingLarge.setText(String.format(Locale.getDefault(), "%.1f/5", avgRating));
        
        // Mock satisfaction percentage
        int satisfaction = (int) (avgRating * 20); // roughly
        binding.txtSatisfactionDesc.setText(String.format(Locale.getDefault(), "%d%% người dùng cảm thấy hài lòng", satisfaction));
        
        binding.chipAll.setText(String.format(Locale.getDefault(), "Tất cả (%d)", totalReviews));
    }

    private void setupRecyclerView() {
        adapter = new ProductReviewAdapter(new ArrayList<>());
        adapter.setOnImageClickListener((images, position, review) -> {
            Intent intent = new Intent(this, FullScreenReviewImageActivity.class);
            intent.putStringArrayListExtra("images", new ArrayList<>(images));
            intent.putExtra("position", position);
            intent.putExtra("product_id", productId);
            
            // Pass review details
            intent.putExtra("user_name", review.getCustomerName());
            intent.putExtra("user_avatar", review.getUserAvatar());
            intent.putExtra("rating", review.getRating());
            intent.putExtra("comment", review.getContent());
            intent.putExtra("date", review.getCreatedAt()); // You might want to format this
            intent.putExtra("helpful_count", review.getHelpfulCount());
            
            // Extract variant info
            String variant = review.getVariantInfo();
            if (variant == null || variant.isEmpty() || variant.equals("-")) {
                String color = review.getColor();
                String size = review.getSize();
                if (color != null && size != null) variant = color + ", " + size;
                else if (color != null) variant = color;
                else if (size != null) variant = size;
            }
            intent.putExtra("variant", variant);

            startActivity(intent);
        });

        binding.rvAllReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAllReviews.setAdapter(adapter);

        adapter.registerAdapterDataObserver(new androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (adapter.getItemCount() == 0) {
                    binding.txtNoReviews.setVisibility(View.VISIBLE);
                } else {
                    binding.txtNoReviews.setVisibility(View.GONE);
                }
            }
        });
        

    }
    
    private void setupActionButtons() {
        binding.btnBuyNow.setOnClickListener(v -> finish()); // Go back to buy
        binding.btnChat.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng chat đang được cập nhật", Toast.LENGTH_SHORT).show();
        });
        binding.btnAddToCart.setOnClickListener(v -> finish());
    }

    private void setupSearch() {
        binding.edtSearchReview.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdatingFilters) return;
                currentSearchQuery = s.toString();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        setupFilters();
        loadProductInfo();
    }

    private Product productInfo;

    private void loadProductInfo() {
        if (productId == null) return;
        HomeApiClient.getApiService().getProductDetail(productId).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                if (response.isSuccessful()) {
                    productInfo = response.body();
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {}
        });
    }

    private void setupFilters() {
        binding.chipAll.setOnClickListener(v -> {
            resetAllFilters();
            updateChipUI(binding.chipAll);
            applyFilters();
        });
        
        binding.chipWithImages.setOnClickListener(v -> {
            boolean previous = filterOnlyWithImages;
            resetAllFilters(); // Clear others
            filterOnlyWithImages = !previous; // Toggle
            updateChipUI(null);
            applyFilters();
        });
        
        binding.chipStar.setOnClickListener(v -> showStarFilterBottomSheet());
        binding.chipVariant.setOnClickListener(v -> showVariantFilterBottomSheet());
    }

    private void resetAllFilters() {
        isUpdatingFilters = true;
        filterOnlyWithImages = false;
        selectedStarFilter = -1;
        filterColor = "";
        filterSize = "";
        currentSearchQuery = "";
        binding.edtSearchReview.setText("");
        binding.chipStar.setText("Số sao");
        binding.chipVariant.setText("Phân loại");
        isUpdatingFilters = false;
    }

    private int selectedStarFilter = -1;

    private void showStarFilterBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.CustomBottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_star_filter_bottom_sheet, null);
        dialog.setContentView(view);

        android.widget.RadioButton rb5 = view.findViewById(R.id.rbStar5);
        android.widget.RadioButton rb4 = view.findViewById(R.id.rbStar4);
        android.widget.RadioButton rb3 = view.findViewById(R.id.rbStar3);
        android.widget.RadioButton rb2 = view.findViewById(R.id.rbStar2);
        android.widget.RadioButton rb1 = view.findViewById(R.id.rbStar1);
        android.widget.RadioButton[] rbs = {rb1, rb2, rb3, rb4, rb5};

        final int[] tempSelectedStar = {selectedStarFilter};

        int[] starCounts = new int[6];
        for (com.project.models.ProductReview r : allReviewsList) {
            if (r.getRating() >= 1 && r.getRating() <= 5) {
                starCounts[r.getRating()]++;
            }
        }

        ((android.widget.TextView) view.findViewById(R.id.txtCount5)).setText(String.valueOf(starCounts[5]));
        ((android.widget.TextView) view.findViewById(R.id.txtCount4)).setText(String.valueOf(starCounts[4]));
        ((android.widget.TextView) view.findViewById(R.id.txtCount3)).setText(String.valueOf(starCounts[3]));
        ((android.widget.TextView) view.findViewById(R.id.txtCount2)).setText(String.valueOf(starCounts[2]));
        ((android.widget.TextView) view.findViewById(R.id.txtCount1)).setText(String.valueOf(starCounts[1]));

        Runnable updateRBs = () -> {
            for (int i = 0; i < 5; i++) {
                rbs[i].setChecked((i + 1) == tempSelectedStar[0]);
            }
        };

        updateRBs.run();

        // Row clicks
        view.findViewById(R.id.layoutStar5).setOnClickListener(v -> { tempSelectedStar[0] = 5; updateRBs.run(); });
        view.findViewById(R.id.layoutStar4).setOnClickListener(v -> { tempSelectedStar[0] = 4; updateRBs.run(); });
        view.findViewById(R.id.layoutStar3).setOnClickListener(v -> { tempSelectedStar[0] = 3; updateRBs.run(); });
        view.findViewById(R.id.layoutStar2).setOnClickListener(v -> { tempSelectedStar[0] = 2; updateRBs.run(); });
        view.findViewById(R.id.layoutStar1).setOnClickListener(v -> { tempSelectedStar[0] = 1; updateRBs.run(); });

        // Direct RadioButton clicks
        rb5.setOnClickListener(v -> { tempSelectedStar[0] = 5; updateRBs.run(); });
        rb4.setOnClickListener(v -> { tempSelectedStar[0] = 4; updateRBs.run(); });
        rb3.setOnClickListener(v -> { tempSelectedStar[0] = 3; updateRBs.run(); });
        rb2.setOnClickListener(v -> { tempSelectedStar[0] = 2; updateRBs.run(); });
        rb1.setOnClickListener(v -> { tempSelectedStar[0] = 1; updateRBs.run(); });

        view.findViewById(R.id.btnResetStar).setOnClickListener(v -> {
            selectedStarFilter = -1;
            binding.chipStar.setText("Số sao");
            resetChip(binding.chipStar);
            updateChipUI(null);
            applyFilters();
            dialog.dismiss();
        });

        view.findViewById(R.id.btnApplyStar).setOnClickListener(v -> {
            if (tempSelectedStar[0] == -1) {
                Toast.makeText(this, "Vui lòng chọn số sao để lọc", Toast.LENGTH_SHORT).show();
                return;
            }

            resetAllFilters(); // Exclusive selection
            selectedStarFilter = tempSelectedStar[0];
            binding.chipStar.setText(String.format(Locale.getDefault(), "%d Sao ⭐️", selectedStarFilter));

            updateChipUI(null);
            applyFilters();
            dialog.dismiss();
        });

        dialog.show();
    }

    private String filterColor = "";
    private String filterSize = "";

    private void showVariantFilterBottomSheet() {
        if (productInfo == null) {
            Toast.makeText(this, "Đang tải thông tin sản phẩm...", Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.CustomBottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_variant_filter_bottom_sheet, null);
        dialog.setContentView(view);

        android.widget.ImageView imgThumb = view.findViewById(R.id.imgProductThumb);
        android.widget.TextView txtName = view.findViewById(R.id.txtProductNameVariant);
        txtName.setText(productInfo.getName());

        if (productInfo.getImages() != null && !productInfo.getImages().isEmpty()) {
            String url = productInfo.getImages().get(0).getUrl();
            if (url != null) {
                if (url.startsWith("/")) url = "https://server-testing-ymn9.onrender.com" + url;
                com.bumptech.glide.Glide.with(this).load(url).into(imgThumb);
            }
        }

        android.widget.LinearLayout layoutColors = view.findViewById(R.id.layoutColorsFilter);
        com.google.android.material.chip.ChipGroup groupSizes = view.findViewById(R.id.chipGroupSizesFilter);

        java.util.List<String> sizeNames = new java.util.ArrayList<>();
        if (productInfo.getVariants() != null) {
            for (com.project.models.ProductVariant v : productInfo.getVariants()) {
                if (v.getSize() != null && !sizeNames.contains(v.getSize())) {
                    sizeNames.add(v.getSize());
                }
            }
        }
        if (productInfo.getSizes() != null) {
            for (Product.ProductSize ps : productInfo.getSizes()) {
                if (ps.getSize() != null && !sizeNames.contains(ps.getSize())) {
                    sizeNames.add(ps.getSize());
                }
            }
        }

        // Setup colors
        if (productInfo.getColors() != null) {
            float density = getResources().getDisplayMetrics().density;
            int dotSize = (int) (32 * density);
            int margin = (int) (12 * density);

            for (String colorName : productInfo.getColors()) {
                android.widget.FrameLayout colorFrame = new android.widget.FrameLayout(this);
                android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(dotSize + (int)(4*density), dotSize + (int)(4*density));
                params.setMargins(0, 0, margin, 0);
                colorFrame.setLayoutParams(params);

                View border = new View(this);
                android.widget.FrameLayout.LayoutParams borderParams = new android.widget.FrameLayout.LayoutParams(dotSize + (int)(4*density), dotSize + (int)(4*density));
                border.setLayoutParams(borderParams);
                android.graphics.drawable.GradientDrawable borderShape = new android.graphics.drawable.GradientDrawable();
                borderShape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                borderShape.setStroke((int)(2*density), android.graphics.Color.BLACK);
                border.setBackground(borderShape);
                border.setVisibility(colorName.equalsIgnoreCase(filterColor) ? View.VISIBLE : View.GONE);

                View dot = new View(this);
                android.widget.FrameLayout.LayoutParams dotParams = new android.widget.FrameLayout.LayoutParams(dotSize, dotSize);
                dotParams.gravity = android.view.Gravity.CENTER;
                dot.setLayoutParams(dotParams);
                android.graphics.drawable.GradientDrawable dotShape = new android.graphics.drawable.GradientDrawable();
                dotShape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                dotShape.setColor(android.graphics.Color.parseColor(mapColorNameToHex(colorName)));
                dot.setBackground(dotShape);

                colorFrame.addView(border);
                colorFrame.addView(dot);

                colorFrame.setOnClickListener(v -> {
                    if (filterColor.equalsIgnoreCase(colorName)) {
                        filterColor = "";
                        border.setVisibility(View.GONE);
                    } else {
                        filterColor = colorName;
                        for (int i = 0; i < layoutColors.getChildCount(); i++) {
                            ((android.view.ViewGroup) layoutColors.getChildAt(i)).getChildAt(0).setVisibility(View.GONE);
                        }
                        border.setVisibility(View.VISIBLE);
                    }
                });
                layoutColors.addView(colorFrame);
            }
        }

        // Setup sizes
        for (String sizeName : sizeNames) {
            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) getLayoutInflater().inflate(R.layout.item_chip_variant, groupSizes, false);
            chip.setText(sizeName);
            chip.setCheckable(true);
            if (sizeName.equalsIgnoreCase(filterSize)) {
                chip.setChecked(true);
            }
            
            chip.setOnClickListener(v -> {
                if (chip.isChecked()) {
                    filterSize = sizeName;
                } else {
                    filterSize = "";
                }
            });
            groupSizes.addView(chip);
        }

        view.findViewById(R.id.btnResetVariant).setOnClickListener(v -> {
            filterColor = "";
            filterSize = "";
            binding.chipVariant.setText("Phân loại");
            applyFilters();
            updateChipUI(null);
            dialog.dismiss();
        });

        view.findViewById(R.id.btnApplyVariant).setOnClickListener(v -> {
            String newColor = filterColor;
            String newSize = filterSize;
            
            resetAllFilters(); // Exclusive selection
            filterColor = newColor;
            filterSize = newSize;

            if (!filterColor.isEmpty() || !filterSize.isEmpty()) {
                String lbl = "";
                if (!filterColor.isEmpty() && !filterSize.isEmpty()) lbl = filterColor + ", " + filterSize;
                else lbl = filterColor + filterSize;
                binding.chipVariant.setText(lbl);
            } else {
                binding.chipVariant.setText("Phân loại");
            }
            updateChipUI(null);
            applyFilters();
            dialog.dismiss();
        });

        dialog.show();
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

    private void applyFilters() {
        List<com.project.models.ProductReview> filtered = new ArrayList<>();
        String query = currentSearchQuery.toLowerCase(Locale.getDefault()).trim();
        
        // Remove accents for broader search (optional but usually expected for Vietnamese)
        String normalizedQuery = normalizeString(query);

        for (com.project.models.ProductReview r : allReviewsList) {
            // 1. With Images filter
            if (filterOnlyWithImages && (r.getImages() == null || r.getImages().isEmpty())) {
                continue;
            }

            // 2. Star Rating filter
            if (selectedStarFilter != -1 && r.getRating() != selectedStarFilter) {
                continue;
            }

            // 3. Variant filter (Color)
            if (!filterColor.isEmpty()) {
                boolean colorMatch = r.getColor() != null && r.getColor().equalsIgnoreCase(filterColor);
                if (!colorMatch) continue;
            }

            // 4. Variant filter (Size)
            if (!filterSize.isEmpty()) {
                boolean sizeMatch = r.getSize() != null && r.getSize().equalsIgnoreCase(filterSize);
                if (!sizeMatch) continue;
            }

            // 5. Broad Keyword search (Name and Content)
            if (!normalizedQuery.isEmpty()) {
                String name = r.getCustomerName() != null ? normalizeString(r.getCustomerName().toLowerCase(Locale.getDefault())) : "";
                String content = r.getContent() != null ? normalizeString(r.getContent().toLowerCase(Locale.getDefault())) : "";
                
                if (!name.contains(normalizedQuery) && !content.contains(normalizedQuery)) {
                    continue;
                }
            }

            filtered.add(r);
        }
        adapter.updateData(filtered);
    }

    private String normalizeString(String input) {
        if (input == null) return "";
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                         .replace('đ', 'd').replace('Đ', 'D');
    }

    private void updateChipUI(com.google.android.material.chip.Chip ignored) {
        // Reset all
        resetChip(binding.chipAll);
        resetChip(binding.chipWithImages);
        resetChip(binding.chipStar);
        resetChip(binding.chipVariant);
        
        // Highlight active chips based on current filter state
        if (filterOnlyWithImages) highlightChip(binding.chipWithImages);
        if (selectedStarFilter != -1) highlightChip(binding.chipStar);
        if (!filterColor.isEmpty() || !filterSize.isEmpty()) highlightChip(binding.chipVariant);
        
        // If nothing is selected, highlight "All"
        if (!filterOnlyWithImages && selectedStarFilter == -1 && filterColor.isEmpty() && filterSize.isEmpty()) {
            highlightChip(binding.chipAll);
        }
    }

    private void highlightChip(com.google.android.material.chip.Chip chip) {
        chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.primary_500)));
        chip.setTextColor(android.graphics.Color.WHITE);
        chip.setChipStrokeWidth(0);
    }
    
    private void resetChip(com.google.android.material.chip.Chip chip) {
        chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        chip.setTextColor(android.graphics.Color.parseColor("#666666"));
        chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#DDDDDD")));
        chip.setChipStrokeWidth(getResources().getDisplayMetrics().density * 1);
    }

    private List<com.project.models.ProductReview> allReviewsList = new ArrayList<>();

    private void loadReviews() {
        ApiService service = HomeApiClient.getApiService();
        service.getProductReviews(productId).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(Call<ReviewResponse> call, Response<ReviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allReviewsList = response.body().getData();
                    adapter.updateData(allReviewsList);
                    
                    // Update counts
                    int withImages = 0;
                    for (com.project.models.ProductReview r : allReviewsList) {
                        if (r.getImages() != null && !r.getImages().isEmpty()) withImages++;
                    }
                    binding.chipWithImages.setText(String.format(Locale.getDefault(), "Có hình ảnh (%d)", withImages));
                } else {
                    Toast.makeText(ProductReviewsActivity.this, "Không thể tải đánh giá", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ReviewResponse> call, Throwable t) {
                Toast.makeText(ProductReviewsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
