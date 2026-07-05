package com.project.muse_android.product;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.adapters.CategoryAdapter;
import com.project.adapters.ProductAdapter;
import com.project.models.Category;
import com.project.models.Product;
import com.project.muse_android.databinding.ActivityCategoryProductsBinding;
import com.project.network.HomeApiClient;
import com.project.network.HomeApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryProductsActivity extends AppCompatActivity {

    private ActivityCategoryProductsBinding binding;
    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;
    
    private final List<Category> categoryList = new ArrayList<>();
    private final List<Product> allProducts = new ArrayList<>();
    private final List<Product> displayProducts = new ArrayList<>();
    
    private String selectedCategoryId;
    private HomeApiService homeApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryProductsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        selectedCategoryId = getIntent().getStringExtra("category_id");
        homeApiService = HomeApiClient.getHomeApiService();

        setupUI();
        loadCategories();
        loadAllProducts();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());
        
        // Horizontal Categories
        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            if (category.getId().equals(selectedCategoryId)) return;
            
            selectedCategoryId = category.getId();
            binding.txtHeaderTitle.setText(category.getName().toUpperCase());
            
            reorderCategories(category);
            filterProducts();
            
            // Explicitly highlight since reorder resets positions
            categoryAdapter.setSelectedCategoryId(category.getId());
        });
        binding.rvHorizontalCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvHorizontalCategories.setAdapter(categoryAdapter);

        // Product Grid
        productAdapter = new ProductAdapter(displayProducts, ProductAdapter.TYPE_VERTICAL, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("product_id", product.get_id());
            startActivity(intent);
        });
        productAdapter.setOnFavoriteClickListener(product -> {
            Toast.makeText(this, "Đã thêm vào yêu thích: " + product.getName(), Toast.LENGTH_SHORT).show();
        });
        binding.rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvProducts.setAdapter(productAdapter);
        
        binding.btnCart.setOnClickListener(v -> Toast.makeText(this, "Giỏ hàng", Toast.LENGTH_SHORT).show());
        binding.btnSearch.setOnClickListener(v -> finish());
    }

    private void reorderCategories(Category selected) {
        if (selected == null) return;
        List<Category> newList = new ArrayList<>();
        newList.add(selected);
        for (Category c : categoryList) {
            if (c.getId() != null && !c.getId().equals(selected.getId())) {
                newList.add(c);
            }
        }
        categoryList.clear();
        categoryList.addAll(newList);
        categoryAdapter.notifyDataSetChanged();
        // Since we moved it to position 0
        categoryAdapter.setSelectedCategoryId(selected.getId());
        binding.rvHorizontalCategories.scrollToPosition(0);
    }

    private void loadCategories() {
        homeApiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryList.clear();
                    Category selectedCat = null;
                    List<Category> otherCats = new ArrayList<>();

                    for (Category cat : response.body()) {
                        if (cat.getStatus() == null || "active".equalsIgnoreCase(cat.getStatus()) || "featured".equalsIgnoreCase(cat.getStatus())) {
                            if (cat.getId().equals(selectedCategoryId)) {
                                selectedCat = cat;
                                binding.txtHeaderTitle.setText(cat.getName().toUpperCase());
                            } else {
                                otherCats.add(cat);
                            }
                        }
                    }

                    if (selectedCat != null) categoryList.add(selectedCat);
                    categoryList.addAll(otherCats);

                    categoryAdapter.notifyDataSetChanged();
                    if (selectedCategoryId != null) {
                        categoryAdapter.setSelectedCategoryId(selectedCategoryId);
                        binding.rvHorizontalCategories.scrollToPosition(0);
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {}
        });
    }

    private void loadAllProducts() {
        binding.progressBar.setVisibility(View.VISIBLE);
        HomeApiClient.getApiService().getProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allProducts.clear();
                    allProducts.addAll(response.body());
                    filterProducts();
                }
            }
            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(CategoryProductsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterProducts() {
        displayProducts.clear();
        for (Product p : allProducts) {
            if (selectedCategoryId != null && selectedCategoryId.equals(p.getCategory())) {
                displayProducts.add(p);
            }
        }
        productAdapter.notifyDataSetChanged();
        binding.txtProductCount.setText(String.format(Locale.getDefault(), "%d (các) sản phẩm", displayProducts.size()));
        
        if (displayProducts.isEmpty()) {
            Toast.makeText(this, "Không có sản phẩm trong danh mục này", Toast.LENGTH_SHORT).show();
        }
    }
}
