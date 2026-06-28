package com.project.muse_android.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentHomeBinding;
import com.project.models.Banner;
import com.project.models.Category;
import com.project.adapters.BannerAdapter;
import com.project.adapters.CategoryAdapter;
import com.project.muse_android.search.SearchActivity;
import com.project.network.ApiResponse;
import com.project.network.HomeApiClient;
import com.project.network.HomeApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private CategoryAdapter categoryAdapter;
    private BannerAdapter bannerAdapter;
    private List<Category> categoryList = new ArrayList<>();
    private List<Banner> bannerList = new ArrayList<>();
    private HomeApiService homeApiService;

    private float dX, dY;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        homeApiService = HomeApiClient.getHomeApiService();
        setupRecyclerView();
        setupViewPager();
        setupDraggableAI();
        setupClickEffects();
        loadBanners();
        loadCategories();
    }

    private void setupRecyclerView() {
        binding.rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 3));
        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            Toast.makeText(getContext(), "Chọn danh mục: " + category.getName(), Toast.LENGTH_SHORT).show();
        });
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    private void setupViewPager() {
        bannerAdapter = new BannerAdapter(bannerList);
        binding.vpBanners.setAdapter(bannerAdapter);
    }

    private void setupClickEffects() {
        applyClickAnimation(binding.imgCart);
        applyClickAnimation(binding.searchBar);

        binding.imgCart.setOnClickListener(v -> Toast.makeText(getContext(), "Giỏ hàng", Toast.LENGTH_SHORT).show());
        
        View.OnClickListener openSearch = v -> {
            Intent intent = new Intent(getContext(), SearchActivity.class);
            startActivity(intent);
        };

        binding.searchBar.setOnClickListener(openSearch);
        binding.edtSearch.setOnClickListener(openSearch);

        
        // Prevent keyboard on home
        binding.edtSearch.setFocusable(false);
        binding.edtSearch.setClickable(true);
    }

    private void applyClickAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
                    break;
            }
            return false;
        });
    }

    private void setupDraggableAI() {
        binding.btnAIDraggable.setOnLongClickListener(v -> true);

        binding.btnAIDraggable.setOnTouchListener(new View.OnTouchListener() {
            private long startClickTime;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        startClickTime = System.currentTimeMillis();
                        // Hồng khi chạm
                        binding.btnAIDraggable.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_500));
                        binding.txtAI.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                        break;
                    case MotionEvent.ACTION_MOVE:
                        view.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        break;
                    case MotionEvent.ACTION_UP:
                        // Trắng khi thả
                        binding.btnAIDraggable.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                        binding.txtAI.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_500));
                        
                        long clickDuration = System.currentTimeMillis() - startClickTime;
                        if (clickDuration < 200) {
                            view.performClick();
                        }
                        snapToEdges(view);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });

        binding.btnAIDraggable.setOnClickListener(v -> Toast.makeText(getContext(), "MUSE AI xin chào!", Toast.LENGTH_SHORT).show());
    }

    private void snapToEdges(View view) {
        if (binding == null) return;
        float screenWidth = binding.homeRoot.getWidth();
        float viewWidth = view.getWidth();
        float xPos = view.getX();

        if (xPos < screenWidth / 2) {
            view.animate().x(16 * getResources().getDisplayMetrics().density).setDuration(200).start();
        } else {
            view.animate().x(screenWidth - viewWidth - (16 * getResources().getDisplayMetrics().density)).setDuration(200).start();
        }
    }

    private void loadBanners() {
        homeApiService.getBanners().enqueue(new Callback<List<Banner>>() {
            @Override
            public void onResponse(Call<List<Banner>> call, Response<List<Banner>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bannerList.clear();
                    for (Banner banner : response.body()) {
                        if ("active".equalsIgnoreCase(banner.getStatus())) {
                            bannerList.add(banner);
                        }
                    }
                    bannerAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Banner>> call, Throwable t) {
                Log.e("HomeFragment", "Load Banners failed", t);
            }
        });
    }

    private void loadCategories() {
        homeApiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryList.clear();
                    for (Category cat : response.body()) {
                        if ("active".equalsIgnoreCase(cat.getStatus())) {
                            categoryList.add(cat);
                        }
                    }
                    categoryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Log.e("HomeFragment", "Load Categories failed", t);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
