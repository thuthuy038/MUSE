package com.project.muse_android.search;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.project.adapters.ProductAdapter;
import com.project.models.Product;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivitySearchResultBinding;
import com.project.muse_android.product.ProductDetailActivity;
import com.project.network.HomeApiClient;
import com.project.network.HomeApiService;
import android.content.Intent;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchResultActivity extends AppCompatActivity {

    private ActivitySearchResultBinding binding;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private HomeApiService apiService;
    private String query;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        query = getIntent().getStringExtra("query");
        if (query == null) query = "";

        binding.edtSearchQuery.setText(query);
        apiService = HomeApiClient.getHomeApiService();

        setupUI();
        setupRecyclerView();
        performSearch(query);
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnClearSearch.setOnClickListener(v -> {
            binding.edtSearchQuery.setText("");
            binding.edtSearchQuery.requestFocus();
        });

        // Search icon click logic
        binding.imgSearchIcon.setOnClickListener(v -> {
            String newQuery = binding.edtSearchQuery.getText().toString().trim();
            if (!newQuery.isEmpty()) {
                performSearch(newQuery);
                hideKeyboard();
            }
        });

        binding.edtSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String newQuery = binding.edtSearchQuery.getText().toString().trim();
                if (!newQuery.isEmpty()) {
                    performSearch(newQuery);
                    hideKeyboard();
                }
                return true;
            }
            return false;
        });
        
        binding.imgCart.setOnClickListener(v -> Toast.makeText(this, "Giỏ hàng", Toast.LENGTH_SHORT).show());
        
        binding.btnOpenFilter.setOnClickListener(v -> showFilterBottomSheet());
        binding.btnFilterPrice.setOnClickListener(v -> showFilterBottomSheet());
        binding.btnFilterColor.setOnClickListener(v -> showFilterBottomSheet());
        binding.btnFilterSize.setOnClickListener(v -> showFilterBottomSheet());
        binding.btnSort.setOnClickListener(v -> Toast.makeText(this, "Sắp xếp", Toast.LENGTH_SHORT).show());
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_filter_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);

        TextView txtCountFilter = view.findViewById(R.id.txtCountFilter);
        txtCountFilter.setText(String.format(Locale.getDefault(), "%d (các) sản phẩm", productList.size()));

        // Accordion Logic
        final View contentCategory = view.findViewById(R.id.contentCategory);
        final View contentSize = view.findViewById(R.id.contentSize);
        final View contentColor = view.findViewById(R.id.contentColor);
        final View contentPrice = view.findViewById(R.id.contentPrice);
        final View contentStar = view.findViewById(R.id.contentStar);

        final TextView iconCategory = view.findViewById(R.id.iconCategory);
        final TextView iconSize = view.findViewById(R.id.iconSize);
        final TextView iconColor = view.findViewById(R.id.iconColor);
        final TextView iconPrice = view.findViewById(R.id.iconPrice);
        final TextView iconStar = view.findViewById(R.id.iconStar);

        final View[] contents = {contentCategory, contentSize, contentColor, contentPrice, contentStar};
        final TextView[] icons = {iconCategory, iconSize, iconColor, iconPrice, iconStar};
        final View[] headers = {
                view.findViewById(R.id.headerCategory),
                view.findViewById(R.id.headerSize),
                view.findViewById(R.id.headerColor),
                view.findViewById(R.id.headerPrice),
                view.findViewById(R.id.headerStar)
        };

        for (int i = 0; i < headers.length; i++) {
            final int index = i;
            headers[i].setOnClickListener(v -> {
                boolean isExpanded = contents[index].getVisibility() == View.VISIBLE;
                
                // Close all
                for (int j = 0; j < contents.length; j++) {
                    contents[j].setVisibility(View.GONE);
                    icons[j].setText("+");
                }

                // Toggle current
                if (!isExpanded) {
                    contents[index].setVisibility(View.VISIBLE);
                    icons[index].setText("-");
                }
            });
        }

        view.findViewById(R.id.btnApplyFilter).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Đã áp dụng bộ lọc", Toast.LENGTH_SHORT).show();
        });

        bottomSheetDialog.show();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.clearFocus();
        }
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(productList, ProductAdapter.TYPE_VERTICAL, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("product_id", product.get_id());
            startActivity(intent);
        });
        productAdapter.setOnFavoriteClickListener(product -> {
            Toast.makeText(this, "Đã thêm vào yêu thích: " + product.getName(), Toast.LENGTH_SHORT).show();
        });
        binding.rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvProducts.setAdapter(productAdapter);
    }

    private void performSearch(String searchQuery) {
        apiService.searchProducts(searchQuery).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Product> results = response.body();
                    
                    List<Product> relevantProducts = new ArrayList<>();
                    String normalizedQuery = removeAccents(searchQuery).trim();
                    
                    for (Product p : results) {
                        // Only show active products. Skip hidden, inactive, disabled, etc.
                        if (p.getStatus() == null || !p.getStatus().equalsIgnoreCase("active")) {
                            continue;
                        }

                        if (p.getName() != null) {
                            String normalizedName = removeAccents(p.getName());
                            
                            // High priority: Word boundary match
                            if (isWordMatch(normalizedName, normalizedQuery)) {
                                relevantProducts.add(p);
                            }
                        }
                    }
                    
                    // Rank results:
                    // 1. Starts with query (e.g. searching "quần" matches "Quần jean")
                    // 2. Exact word match earlier in the name
                    Collections.sort(relevantProducts, (p1, p2) -> {
                        String n1 = removeAccents(p1.getName());
                        String n2 = removeAccents(p2.getName());
                        
                        boolean s1 = n1.startsWith(normalizedQuery);
                        boolean s2 = n2.startsWith(normalizedQuery);
                        
                        if (s1 && !s2) return -1;
                        if (!s1 && s2) return 1;
                        
                        // Otherwise, sort by position of the word
                        int i1 = n1.indexOf(normalizedQuery);
                        int i2 = n2.indexOf(normalizedQuery);
                        if (i1 != i2) return Integer.compare(i1, i2);
                        
                        return n1.compareTo(n2);
                    });
                    
                    productList.clear();
                    productList.addAll(relevantProducts);
                    productAdapter.notifyDataSetChanged();
                    binding.txtProductCount.setText(String.format(Locale.getDefault(), "%d sản phẩm phù hợp", productList.size()));
                } else {
                    handleEmptyResult();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Log.e("SearchResult", "Search failed: " + t.getMessage());
                Toast.makeText(SearchResultActivity.this, "Lỗi kết nối server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
            
            private void handleEmptyResult() {
                productList.clear();
                productAdapter.notifyDataSetChanged();
                binding.txtProductCount.setText("0 sản phẩm phù hợp");
            }
        });
    }

    private boolean isWordMatch(String text, String query) {
        int index = text.indexOf(query);
        while (index >= 0) {
            boolean startOk = (index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1)));
            boolean endOk = (index + query.length() == text.length() || 
                             !Character.isLetterOrDigit(text.charAt(index + query.length())));
            
            if (startOk && endOk) return true;
            index = text.indexOf(query, index + 1);
        }
        return false;
    }

    private String removeAccents(String s) {
        if (s == null) return "";
        String nfdNormalizedString = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").toLowerCase(Locale.getDefault())
                .replace("đ", "d").replace("Đ", "d");
    }
}
