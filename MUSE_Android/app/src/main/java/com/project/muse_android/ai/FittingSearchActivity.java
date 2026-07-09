package com.project.muse_android.ai;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.project.models.Product;
import com.project.models.ProductVariant;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityFittingSearchBinding;
import com.project.network.HomeApiClient;
import com.project.utils.CartManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FittingSearchActivity extends AppCompatActivity {

    private ActivityFittingSearchBinding binding;
    private final List<Product> allProducts = new ArrayList<>();
    private final List<Product> displayedProducts = new ArrayList<>();
    private SearchResultsAdapter adapter;
    private Product currentProduct;

    private String selectedSize = "";
    private String selectedColor = "";
    private ProductAutoCompleteAdapter autoCompleteAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFittingSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        com.project.utils.ViewUtils.applySystemBarsPadding(binding.layoutHeader, true, false);
        binding.btnBack.setOnClickListener(v -> finish());

        // Setup AI Agent button to go back to AI Hub
        binding.btnAiAgent.setOnClickListener(v -> navigateToAiHub());

        // Hide bottom actions and navigation when keyboard is open
        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                android.graphics.Rect r = new android.graphics.Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                if (keypadHeight > screenHeight * 0.15) {
                    binding.layoutBottomActions.setVisibility(View.GONE);
                    binding.bottomNavigationView.setVisibility(View.GONE);
                } else {
                    binding.layoutBottomActions.setVisibility(View.VISIBLE);
                    binding.bottomNavigationView.setVisibility(View.VISIBLE);
                }
            }
        });

        // Setup RecyclerView
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new SearchResultsAdapter(displayedProducts);
        binding.rvSearchResults.setAdapter(adapter);

        // Fetch products from database
        loadAllProducts();

        // Setup Autocomplete selection
        binding.edtSearch.setOnItemClickListener((parent, view, position, id) -> {
            Product selectedProduct = (Product) parent.getItemAtPosition(position);
            displayProduct(selectedProduct);
            filterSimilarProducts(selectedProduct);
            
            // Clear input text as requested
            binding.edtSearch.setText("");

            // Hide keyboard after selecting suggestion
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(binding.edtSearch.getWindowToken(), 0);
            }
        });

        // Setup Search bar action (IME key and editor listener)
        binding.edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performFilter();
                return true;
            }
            return false;
        });

        // Add to cart action
        binding.btnAddToCart.setOnClickListener(v -> {
            if (currentProduct != null) {
                if (selectedSize.isEmpty()) {
                    Toast.makeText(this, "Vui lòng chọn kích cỡ.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedColor.isEmpty()) {
                    Toast.makeText(this, "Vui lòng chọn màu sắc.", Toast.LENGTH_SHORT).show();
                    return;
                }
                CartManager.getInstance(this).addToCart(currentProduct, selectedColor, selectedSize, 1, new CartManager.CartCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        runOnUiThread(() -> {
                            Toast.makeText(FittingSearchActivity.this, "Đã thêm " + currentProduct.getName() + " vào giỏ hàng! 🛒", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(FittingSearchActivity.this, "Lỗi thêm giỏ hàng: " + message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                Toast.makeText(this, "Vui lòng chọn sản phẩm.", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup Favorite button action
        binding.btnFavoriteSearch.setOnClickListener(v -> {
            if (currentProduct != null) {
                currentProduct.setFavorite(!currentProduct.isFavorite());
                updateFavoriteUI(currentProduct.isFavorite());
                Toast.makeText(this, currentProduct.isFavorite() 
                        ? "Đã thêm vào mục yêu thích" 
                        : "Đã xóa khỏi mục yêu thích", Toast.LENGTH_SHORT).show();
            }
        });

        // Go to photo room
        binding.btnGoToPhotoRoom.setOnClickListener(v -> {
            if (currentProduct != null) {
                if (selectedSize.isEmpty()) {
                    Toast.makeText(this, "Vui lòng chọn kích cỡ.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedColor.isEmpty()) {
                    Toast.makeText(this, "Vui lòng chọn màu sắc.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(this, VirtualFittingActivity.class);
                intent.putExtra("product_id", currentProduct.get_id() != null ? currentProduct.get_id() : currentProduct.getId());
                intent.putExtra("size", selectedSize);
                intent.putExtra("color", selectedColor);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Vui lòng chọn sản phẩm.", Toast.LENGTH_SHORT).show();
            }
        });
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

    private void loadAllProducts() {
        HomeApiClient.getHomeApiService().searchProducts("").enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allProducts.clear();
                    for (Product p : response.body()) {
                        if (p.getStatus() == null || "active".equalsIgnoreCase(p.getStatus())) {
                            allProducts.add(p);
                        }
                    }
                    // Load all initially
                    displayedProducts.clear();
                    displayedProducts.addAll(allProducts);
                    adapter.notifyDataSetChanged();
                    if (!displayedProducts.isEmpty()) {
                        displayProduct(displayedProducts.get(0));
                    }

                    // Setup Autocomplete Suggestion Adapter
                    autoCompleteAdapter = new ProductAutoCompleteAdapter(allProducts);
                    binding.edtSearch.setAdapter(autoCompleteAdapter);
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Toast.makeText(FittingSearchActivity.this, "Lỗi kết nối dữ liệu.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performFilter() {
        String query = binding.edtSearch.getText().toString().trim();
        displayedProducts.clear();
        String normalizedQuery = removeAccents(query).trim();
        for (Product p : allProducts) {
            if (query.isEmpty() || (p.getName() != null && removeAccents(p.getName()).contains(normalizedQuery))) {
                displayedProducts.add(p);
            }
        }
        adapter.notifyDataSetChanged();
        if (!displayedProducts.isEmpty()) {
            adapter.selectedPos = 0;
            displayProduct(displayedProducts.get(0));
        } else {
            Toast.makeText(this, "Không tìm thấy sản phẩm phù hợp.", Toast.LENGTH_SHORT).show();
        }
    }

    private void filterSimilarProducts(Product selectedProduct) {
        displayedProducts.clear();
        String targetCategory = selectedProduct.getCategory();
        for (Product p : allProducts) {
            if (targetCategory != null && targetCategory.equalsIgnoreCase(p.getCategory())) {
                displayedProducts.add(p);
            }
        }

        // Fallback: if no similar products found, display all products
        if (displayedProducts.isEmpty()) {
            displayedProducts.addAll(allProducts);
        }

        // Update selection in search results adapter
        adapter.selectedPos = 0;
        for (int i = 0; i < displayedProducts.size(); i++) {
            Product p = displayedProducts.get(i);
            String pid = p.get_id() != null ? p.get_id() : p.getId();
            String selPid = selectedProduct.get_id() != null ? selectedProduct.get_id() : selectedProduct.getId();
            if (pid != null && pid.equals(selPid)) {
                adapter.selectedPos = i;
                break;
            }
        }
        adapter.notifyDataSetChanged();
    }

    private String removeAccents(String src) {
        if (src == null) return "";
        String temp = Normalizer.normalize(src, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").toLowerCase();
    }

    private void displayProduct(Product product) {
        currentProduct = product;
        binding.tvProductName.setText(product.getName());
        binding.tvSubtitle.setText(product.getCategory() != null ? product.getCategory().toUpperCase() : "DANH MỤC");

        double price = product.getDiscountPrice() != null && product.getDiscountPrice() > 0 
                ? product.getDiscountPrice() : product.getPrice();
        binding.tvPrice.setText(formatPrice(price));

        if (product.getDiscountPrice() != null && product.getDiscountPrice() > 0) {
            binding.tvOriginalPriceDemo.setText(formatPrice(product.getPrice()));
            binding.tvOriginalPriceDemo.setVisibility(View.VISIBLE);
            binding.tvOriginalPriceDemo.setPaintFlags(binding.tvOriginalPriceDemo.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            binding.tvOriginalPriceDemo.setVisibility(View.GONE);
        }

        // Load main product image
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
                    .into(binding.ivProductDetailImage);
        } else {
            binding.ivProductDetailImage.setImageResource(R.drawable.demo_product);
        }

        // Reset selections
        selectedSize = "";
        selectedColor = "";

        // Setup dynamic Size and Color options
        setupSizes(product);
        setupColors(product);

        // Sync Favorite Button State
        updateFavoriteUI(product.isFavorite());
    }

    private void updateFavoriteUI(boolean isFavorite) {
        if (isFavorite) {
            binding.btnFavoriteSearch.setImageResource(R.drawable.ic_favorite_filled);
            binding.btnFavoriteSearch.setImageTintList(null);
        } else {
            binding.btnFavoriteSearch.setImageResource(R.drawable.ic_favorite);
            binding.btnFavoriteSearch.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#333333")));
        }
    }

    private void setupColors(Product product) {
        binding.layoutColors.removeAllViews();
        List<String> colors = new ArrayList<>();
        if (product.getVariants() != null) {
            for (ProductVariant v : product.getVariants()) {
                if (v.getColor() != null && !colors.contains(v.getColor()) && !v.getColor().isEmpty()) {
                    colors.add(v.getColor());
                }
            }
        }

        if (colors.isEmpty()) {
            binding.tvSelectColorLabel.setVisibility(View.GONE);
            binding.layoutColors.setVisibility(View.GONE);
            return;
        }

        binding.tvSelectColorLabel.setVisibility(View.VISIBLE);
        binding.layoutColors.setVisibility(View.VISIBLE);
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
                binding.tvSelectColorLabel.setText(String.format("MÀU SẮC: %s", colorStr.toUpperCase()));
            });

            binding.layoutColors.addView(dot);

            // Select first color by default
            if (i == 0) {
                dot.performClick();
            }
        }
    }

    private void setupSizes(Product product) {
        binding.chipGroupSizes.removeAllViews();
        List<String> sizeNames = new ArrayList<>();
        
        if (product.getVariants() != null) {
            for (ProductVariant v : product.getVariants()) {
                if (v.getSize() != null && !sizeNames.contains(v.getSize()) && !v.getSize().isEmpty()) {
                    sizeNames.add(v.getSize());
                }
            }
        }

        if (product.getSizes() != null) {
            for (Product.ProductSize ps : product.getSizes()) {
                if (ps.getSize() != null && !sizeNames.contains(ps.getSize()) && !ps.getSize().isEmpty()) {
                    sizeNames.add(ps.getSize());
                }
            }
        }

        if (sizeNames.isEmpty()) {
            binding.tvSelectSizeLabel.setVisibility(View.GONE);
            binding.chipGroupSizes.setVisibility(View.GONE);
            return;
        }

        // Sort sizes
        try {
            sizeNames.sort((s1, s2) -> {
                try {
                    Double d1 = Double.parseDouble(s1.replaceAll("[^0-9.]", ""));
                    Double d2 = Double.parseDouble(s2.replaceAll("[^0-9.]", ""));
                    return d1.compareTo(d2);
                } catch (Exception e) {
                    String order = "XXS XS S M L XL XXL 2XL 3XL";
                    int i1 = order.indexOf(s1.toUpperCase());
                    int i2 = order.indexOf(s2.toUpperCase());
                    if (i1 != -1 && i2 != -1) return Integer.compare(i1, i2);
                    return s1.compareTo(s2);
                }
            });
        } catch (Exception ignored) {}

        binding.tvSelectSizeLabel.setVisibility(View.VISIBLE);
        binding.chipGroupSizes.setVisibility(View.VISIBLE);
        binding.tvSelectSizeLabel.setText("KÍCH CỠ");

        int firstAvailableId = -1;

        for (String sizeName : sizeNames) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_variant, binding.chipGroupSizes, false);
            chip.setText(sizeName);
            
            int chipId = View.generateViewId();
            chip.setId(chipId);

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedSize = sizeName;
                    binding.tvSelectSizeLabel.setText(String.format("KÍCH CỠ: %s", sizeName));
                }
            });

            // Check stock
            boolean hasStock = false;
            if (product.getVariants() != null) {
                for (ProductVariant v : product.getVariants()) {
                    if (sizeName.equals(v.getSize()) && v.getQuantity() > 0) {
                        hasStock = true;
                        break;
                    }
                }
            }

            if (!hasStock) {
                chip.setEnabled(false);
                chip.setAlpha(0.3f);
                chip.setCheckable(false);
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

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + " VNĐ";
    }

    private class ProductAutoCompleteAdapter extends android.widget.BaseAdapter implements android.widget.Filterable {
        private final List<Product> originalList;
        private List<Product> filteredList;
        private final android.widget.Filter filter = new android.widget.Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<Product> suggestions = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    suggestions.addAll(originalList);
                } else {
                    String filterPattern = removeAccents(constraint.toString().trim());
                    for (Product item : originalList) {
                        if (item.getName() != null && removeAccents(item.getName()).contains(filterPattern)) {
                            suggestions.add(item);
                        }
                    }
                }
                results.values = suggestions;
                results.count = suggestions.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredList = (List<Product>) results.values;
                notifyDataSetChanged();
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                return ((Product) resultValue).getName();
            }
        };

        public ProductAutoCompleteAdapter(List<Product> list) {
            this.originalList = list;
            this.filteredList = new ArrayList<>(list);
        }

        @Override
        public int getCount() {
            return filteredList.size();
        }

        @Override
        public Product getItem(int position) {
            return filteredList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_suggestion, parent, false);
            }
            Product product = getItem(position);
            
            TextView tvName = convertView.findViewById(R.id.tvSuggestProductName);
            TextView tvPrice = convertView.findViewById(R.id.tvSuggestProductPrice);
            ImageView ivImage = convertView.findViewById(R.id.ivSuggestProductImage);
            
            tvName.setText(product.getName());
            double price = product.getDiscountPrice() != null && product.getDiscountPrice() > 0 
                    ? product.getDiscountPrice() : product.getPrice();
            tvPrice.setText(formatPrice(price));
            
            String imageUrl = "";
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                imageUrl = product.getImages().get(0).getUrl();
            }
            if (imageUrl != null && !imageUrl.isEmpty()) {
                if (!imageUrl.startsWith("http")) {
                    imageUrl = "https://server-testing-ymn9.onrender.com" + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
                }
                Glide.with(convertView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.demo_product)
                        .into(ivImage);
            } else {
                ivImage.setImageResource(R.drawable.demo_product);
            }
            
            return convertView;
        }

        @Override
        public android.widget.Filter getFilter() {
            return filter;
        }
    }

    private class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {
        private final List<Product> items;
        private int selectedPos = 0;

        public SearchResultsAdapter(List<Product> items) {
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
