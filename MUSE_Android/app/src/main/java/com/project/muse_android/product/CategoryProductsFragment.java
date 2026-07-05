package com.project.muse_android.product;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.adapters.CategoryAdapter;
import com.project.adapters.ProductAdapter;
import com.project.models.Category;
import com.project.models.Product;
import com.project.muse_android.databinding.FragmentCategoryProductsBinding;
import com.project.network.HomeApiClient;
import com.project.network.HomeApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryProductsFragment extends Fragment {

    private FragmentCategoryProductsBinding binding;
    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;

    private final List<Category> categoryList = new ArrayList<>();
    private final List<Product> allProducts = new ArrayList<>();
    private final List<Product> displayProducts = new ArrayList<>();

    private String selectedCategoryId;
    private HomeApiService homeApiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoryProductsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            selectedCategoryId = getArguments().getString("category_id");
        }

        homeApiService = HomeApiClient.getHomeApiService();

        setupUI();
        loadCategories();
        loadAllProducts();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Horizontal Categories
        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            if (category.getId().equals(selectedCategoryId)) return;

            selectedCategoryId = category.getId();
            binding.txtHeaderTitle.setText(category.getName().toUpperCase());

            reorderCategories(category);
            filterProducts();

            categoryAdapter.setSelectedCategoryId(category.getId());
        });
        binding.rvHorizontalCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvHorizontalCategories.setAdapter(categoryAdapter);

        // Product Grid
        productAdapter = new ProductAdapter(displayProducts, ProductAdapter.TYPE_VERTICAL, product -> {
            Intent intent = new Intent(getActivity(), ProductDetailActivity.class);
            intent.putExtra("product_id", product.get_id());
            startActivity(intent);
        });
        productAdapter.setOnFavoriteClickListener(product -> {
            Toast.makeText(getContext(), "Đã thêm vào yêu thích: " + product.getName(), Toast.LENGTH_SHORT).show();
        });
        binding.rvProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvProducts.setAdapter(productAdapter);

        binding.btnCart.setOnClickListener(v -> Toast.makeText(getContext(), "Giỏ hàng", Toast.LENGTH_SHORT).show());
        binding.btnSearch.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
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
                                if (isAdded() && binding != null) {
                                    binding.txtHeaderTitle.setText(cat.getName().toUpperCase());
                                }
                            } else {
                                otherCats.add(cat);
                            }
                        }
                    }

                    if (selectedCat != null) categoryList.add(selectedCat);
                    categoryList.addAll(otherCats);

                    if (isAdded() && binding != null) {
                        categoryAdapter.notifyDataSetChanged();
                        if (selectedCategoryId != null) {
                            categoryAdapter.setSelectedCategoryId(selectedCategoryId);
                            binding.rvHorizontalCategories.scrollToPosition(0);
                        }
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
                if (isAdded() && binding != null) {
                    binding.progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        allProducts.clear();
                        allProducts.addAll(response.body());
                        filterProducts();
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                if (isAdded() && binding != null) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
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
        if (isAdded() && binding != null) {
            productAdapter.notifyDataSetChanged();
            binding.txtProductCount.setText(String.format(Locale.getDefault(), "%d (các) sản phẩm", displayProducts.size()));

            if (displayProducts.isEmpty()) {
                Toast.makeText(getContext(), "Không có sản phẩm trong danh mục này", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
