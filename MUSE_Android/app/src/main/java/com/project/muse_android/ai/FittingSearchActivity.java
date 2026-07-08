package com.project.muse_android.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFittingSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        com.project.utils.ViewUtils.applySystemBarsPadding(binding.layoutHeader, true, false);
        binding.btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new SearchResultsAdapter(displayedProducts);
        binding.rvSearchResults.setAdapter(adapter);

        // Fetch products from database
        loadAllProducts();

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
                String color = "";
                String size = "";
                if (currentProduct.getVariants() != null && !currentProduct.getVariants().isEmpty()) {
                    ProductVariant variant = currentProduct.getVariants().get(0);
                    color = variant.getColor();
                    size = variant.getSize();
                }
                CartManager.getInstance(this).addToCart(currentProduct, color, size, 1, new CartManager.CartCallback<Void>() {
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

        // Go to photo room
        binding.btnGoToPhotoRoom.setOnClickListener(v -> {
            if (currentProduct != null) {
                Intent intent = new Intent(this, VirtualFittingActivity.class);
                intent.putExtra("product_id", currentProduct.get_id() != null ? currentProduct.get_id() : currentProduct.getId());
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
            displayProduct(displayedProducts.get(0));
        } else {
            Toast.makeText(this, "Không tìm thấy sản phẩm phù hợp.", Toast.LENGTH_SHORT).show();
        }
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
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + " VNĐ";
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
