package com.project.muse_android.search;

import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.project.adapters.ProductAdapter;
import com.project.models.Product;
import com.project.muse_android.databinding.ActivitySearchResultBinding;
import com.project.network.ApiResponse;
import com.project.network.HomeApiClient;
import com.project.network.HomeApiService;

import java.text.Normalizer;
import java.util.ArrayList;
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

        binding.edtSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String newQuery = binding.edtSearchQuery.getText().toString().trim();
                if (!newQuery.isEmpty()) {
                    performSearch(newQuery);
                }
                return true;
            }
            return false;
        });
        
        binding.btnFilterPrice.setOnClickListener(v -> Toast.makeText(this, "Lọc giá", Toast.LENGTH_SHORT).show());
        binding.btnFilterColor.setOnClickListener(v -> Toast.makeText(this, "Lọc màu", Toast.LENGTH_SHORT).show());
        binding.btnFilterSize.setOnClickListener(v -> Toast.makeText(this, "Lọc size", Toast.LENGTH_SHORT).show());
        binding.btnSort.setOnClickListener(v -> Toast.makeText(this, "Sắp xếp", Toast.LENGTH_SHORT).show());
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(productList);
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
                    String normalizedQuery = removeAccents(searchQuery);
                    
                    for (Product p : results) {
                        if (p.getName() != null) {
                            String normalizedName = removeAccents(p.getName());
                            if (normalizedName.contains(normalizedQuery)) {
                                relevantProducts.add(p);
                            }
                        }
                    }
                    
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

    private String removeAccents(String s) {
        if (s == null) return "";
        String nfdNormalizedString = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").toLowerCase(Locale.getDefault())
                .replace("đ", "d").replace("Đ", "d");
    }
}
