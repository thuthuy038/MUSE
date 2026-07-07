package com.project.muse_android.explore;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.project.adapters.CategoryAdapter;
import com.project.models.Category;
import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentExploreBinding;
import com.project.muse_android.search.SearchActivity;
import com.project.network.HomeApiClient;
import com.project.network.HomeApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExploreFragment extends Fragment {

    private FragmentExploreBinding binding;
    private CategoryAdapter categoryAdapter;
    private final List<Category> categoryList = new ArrayList<>();
    private HomeApiService homeApiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        homeApiService = HomeApiClient.getHomeApiService();

        setupUI();
        loadCategories();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.btnSearch.setOnClickListener(v -> startActivity(new Intent(getActivity(), SearchActivity.class)));
        binding.btnCart.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.navigation_cart));

        // Category Grid Adapter
        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            Bundle bundle = new Bundle();
            bundle.putString("category_id", category.getId());
            Navigation.findNavController(binding.getRoot())
                    .navigate(R.id.navigation_category_products, bundle);
        });

        // Disable selection highlight for this overview screen
        categoryAdapter.setSelectionEnabled(false);

        binding.rvGridCategories.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.rvGridCategories.setAdapter(categoryAdapter);
    }

    private void loadCategories() {
        if (binding == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);

        homeApiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    categoryList.clear();

                    // Add "Tất cả" category
                    Category allCategory = new Category();
                    allCategory.setId("all");
                    allCategory.setName("Tất cả");
                    categoryList.add(allCategory);

                    for (Category cat : response.body()) {
                        String status = cat.getStatus();
                        if (status == null || "active".equalsIgnoreCase(status) || "featured".equalsIgnoreCase(status)) {
                            categoryList.add(cat);
                        }
                    }
                    categoryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
