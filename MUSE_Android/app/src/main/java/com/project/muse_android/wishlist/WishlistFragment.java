package com.project.muse_android.wishlist;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.recyclerview.widget.GridLayoutManager;

import com.project.adapters.ProductAdapter;
import com.project.adapters.VerticalProductAdapter;
import com.project.adapters.WishlistAdapter;
import com.project.models.Category;
import com.project.models.Product;
import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentWishlistBinding;
import com.project.muse_android.main.MainActivity;
import com.project.utils.ViewUtils;
import com.project.muse_android.product.ProductDetailActivity;
import com.project.muse_android.search.SearchActivity;
import com.project.network.HomeApiClient;
import com.project.network.ApiService;
import com.project.network.HomeApiService;
import com.project.utils.CartManager;
import com.project.utils.WishlistManager;
import com.project.models.WishlistResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WishlistFragment extends Fragment implements WishlistAdapter.OnProductClickListener {

    private FragmentWishlistBinding binding;
    private WishlistAdapter wishlistAdapter;
    private VerticalProductAdapter recommendedAdapter;
    
    private List<Product> allFavoriteProducts = new ArrayList<>();
    private List<Product> displayedProducts = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    
    private String currentFilterStatus = "all"; // all, in_stock, out_of_stock
    private boolean filterDiscountOnly = false;
    private String currentFilterCategoryId = "all";
    private int currentBottomInset = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWishlistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupRecyclerViews();
        setupClickListeners();
        setupFilters();
        
        // Apply padding to avoid status bar/navigation bar overlap
        ViewUtils.applySystemBarsPadding(binding.appBarLayout, true, false);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            currentBottomInset = systemBars.bottom;
            
            // Apply bottom padding to the footer content to avoid navigation bar overlap
            binding.layoutEditFooterContent.setPadding(
                binding.layoutEditFooterContent.getPaddingLeft(),
                binding.layoutEditFooterContent.getPaddingTop(),
                binding.layoutEditFooterContent.getPaddingRight(),
                currentBottomInset
            );
            
            // Apply bottom padding to the scroll view container to ensure content isn't covered
            // We add extra padding if the footer is visible
            adjustScrollPadding();

            return insets;
        });
        
        loadData();
    }

    private void setupRecyclerViews() {
        // Wishlist RecyclerView
        wishlistAdapter = new WishlistAdapter(displayedProducts, this);
        binding.rvWishlist.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvWishlist.setAdapter(wishlistAdapter);

        // Recommended RecyclerView
        recommendedAdapter = new VerticalProductAdapter(getContext(), new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(getContext(), ProductDetailActivity.class);
                intent.putExtra("product_id", product.get_id() != null ? product.get_id() : product.getId());
                startActivity(intent);
            }

            @Override
            public void onFavoriteClick(Product product, int position) {
                // Check login first
                com.project.utils.SessionManager sessionManager = new com.project.utils.SessionManager(requireContext());
                if (!sessionManager.isLoggedIn()) {
                    Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Handle favorite toggle from recommendation list manually
                product.setFavorite(!product.isFavorite());
                recommendedAdapter.notifyItemChanged(position);

                WishlistManager.getInstance(getContext()).addToWishlist(
                        product.get_id() != null ? product.get_id() : product.getId(),
                        new WishlistManager.WishlistCallback<WishlistResponse>() {
                    @Override
                    public void onSuccess(WishlistResponse result) {
                        Toast.makeText(getContext(), "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                        loadData(); // Refresh main wishlist
                    }

                    @Override
                    public void onError(String message) {
                        product.setFavorite(false);
                        recommendedAdapter.notifyItemChanged(position);
                        Toast.makeText(getContext(), "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        binding.rvRecommended.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvRecommended.setAdapter(recommendedAdapter);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        
        binding.btnSearch.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), SearchActivity.class));
        });
        
        binding.btnCart.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.putExtra("open_cart", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        binding.btnEdit.setOnClickListener(v -> toggleEditMode(true));
        
        binding.btnDone.setOnClickListener(v -> toggleEditMode(false));
        
        binding.btnExploreNow.setOnClickListener(v -> {
            // Navigate to explore
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.putExtra("open_explore", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        binding.cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            wishlistAdapter.selectAll(isChecked);
        });

        binding.btnUnfavorite.setOnClickListener(v -> {
            List<Product> selected = wishlistAdapter.getSelectedProducts();
            if (selected.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng chọn sản phẩm", Toast.LENGTH_SHORT).show();
                return;
            }
            
            new AlertDialog.Builder(requireContext())
                    .setTitle("Bỏ thích")
                    .setMessage("Bạn có chắc chắn muốn bỏ thích " + selected.size() + " sản phẩm đã chọn?")
                    .setPositiveButton("Bỏ thích", (dialog, which) -> {
                        int[] count = {0};
                        int total = selected.size();
                        for (Product p : selected) {
                            String productId = p.get_id() != null ? p.get_id() : p.getId();
                            WishlistManager.getInstance(getContext()).removeFromWishlist(productId, new WishlistManager.WishlistCallback<WishlistResponse>() {
                                @Override
                                public void onSuccess(WishlistResponse result) {
                                    p.setFavorite(false);
                                    allFavoriteProducts.remove(p);
                                    count[0]++;
                                    
                                    if (count[0] == total) {
                                        updateWishlistCount();
                                        applyFilters();
                                        if (allFavoriteProducts.isEmpty()) {
                                            binding.layoutEmpty.setVisibility(View.VISIBLE);
                                            binding.rvWishlist.setVisibility(View.GONE);
                                        }
                                        loadRecommendations();
                                        Toast.makeText(getContext(), "Đã cập nhật danh sách", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onError(String message) {
                                    count[0]++;
                                    if (count[0] == total) {
                                        updateWishlistCount();
                                        applyFilters();
                                        loadRecommendations();
                                        Toast.makeText(getContext(), "Hoàn tất cập nhật với một số lỗi", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                        Toast.makeText(getContext(), "Đang bỏ thích " + total + " sản phẩm...", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void toggleEditMode(boolean edit) {
        wishlistAdapter.setEditMode(edit);
        binding.layoutEditFooter.setVisibility(edit ? View.VISIBLE : View.GONE);
        binding.btnEdit.setVisibility(edit ? View.GONE : View.VISIBLE);
        binding.btnSearch.setVisibility(edit ? View.GONE : View.VISIBLE);
        binding.btnCart.setVisibility(edit ? View.GONE : View.VISIBLE);
        
        if (edit) {
            binding.layoutEditFooter.setTranslationY(0);
        }
        
        adjustScrollPadding();
        
        if (!edit) {
            binding.cbSelectAll.setChecked(false);
        }
    }

    private void setupFilters() {
        binding.chipAll.setOnClickListener(v -> {
            resetFilters();
            applyFilters();
            updateChipUI();
        });

        binding.chipDiscount.setOnClickListener(v -> {
            filterDiscountOnly = !filterDiscountOnly;
            applyFilters();
            updateChipUI();
        });

        binding.chipStatus.setOnClickListener(v -> {
            String[] options = {"Tất cả", "Còn hàng", "Hết hàng"};
            new AlertDialog.Builder(requireContext())
                    .setTitle("Chọn trạng thái")
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0: currentFilterStatus = "all"; break;
                            case 1: currentFilterStatus = "in_stock"; break;
                            case 2: currentFilterStatus = "out_of_stock"; break;
                        }
                        binding.chipStatus.setText(options[which]);
                        applyFilters();
                        updateChipUI();
                    }).show();
        });

        binding.chipCategory.setOnClickListener(v -> {
            if (categories.isEmpty()) return;
            
            List<String> catNames = new ArrayList<>();
            catNames.add("Tất cả danh mục");
            for (Category c : categories) catNames.add(c.getName());
            
            String[] options = catNames.toArray(new String[0]);
            new AlertDialog.Builder(requireContext())
                    .setTitle("Chọn danh mục")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            currentFilterCategoryId = "all";
                            binding.chipCategory.setText("Danh mục");
                        } else {
                            Category selected = categories.get(which - 1);
                            currentFilterCategoryId = selected.getId();
                            binding.chipCategory.setText(selected.getName());
                        }
                        applyFilters();
                        updateChipUI();
                    }).show();
        });
    }

    private void resetFilters() {
        currentFilterStatus = "all";
        filterDiscountOnly = false;
        currentFilterCategoryId = "all";
        binding.chipStatus.setText("Trạng Thái");
        binding.chipCategory.setText("Danh mục");
    }

    private void updateChipUI() {
        // Simple UI update for chips based on active filters
        binding.chipAll.setChipBackgroundColorResource(
                (currentFilterStatus.equals("all") && !filterDiscountOnly && currentFilterCategoryId.equals("all")) 
                ? R.color.primary_500 : android.R.color.white);
        binding.chipAll.setTextColor(getResources().getColor(
                (currentFilterStatus.equals("all") && !filterDiscountOnly && currentFilterCategoryId.equals("all")) 
                ? android.R.color.white : android.R.color.black));
        
        binding.chipDiscount.setChipBackgroundColorResource(filterDiscountOnly ? R.color.primary_500 : android.R.color.white);
        binding.chipDiscount.setTextColor(getResources().getColor(filterDiscountOnly ? android.R.color.white : android.R.color.black));
    }

    private void loadRecommendations() {
        ApiService apiService = HomeApiClient.getApiService();
        apiService.getProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Product> products = response.body();
                    List<Product> nonFavorites = new ArrayList<>();
                    for (Product p : products) {
                        boolean inWishlist = false;
                        for (Product fav : allFavoriteProducts) {
                            String favId = fav.get_id() != null ? fav.get_id() : fav.getId();
                            String pId = p.get_id() != null ? p.get_id() : p.getId();
                            if (pId.equals(favId)) {
                                inWishlist = true;
                                break;
                            }
                        }
                        if (!inWishlist) {
                            nonFavorites.add(p);
                        }
                    }
                    recommendedAdapter.setData(nonFavorites.stream().limit(10).collect(Collectors.toList()));
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {}
        });
    }

    private void loadData() {
        ApiService apiService = HomeApiClient.getApiService();
        HomeApiService homeApiService = HomeApiClient.getHomeApiService();
        
        // Load Categories
        homeApiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categories = response.body();
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {}
        });

        // Load Wishlist from API
        WishlistManager.getInstance(getContext()).getWishlist(new WishlistManager.WishlistCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> result) {
                allFavoriteProducts.clear();
                if (result != null) {
                    allFavoriteProducts.addAll(result);
                    // Mark as favorite for local logic
                    for (Product p : allFavoriteProducts) {
                        p.setFavorite(true);
                    }
                }
                
                updateWishlistCount();
                
                if (allFavoriteProducts.isEmpty()) {
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                    binding.rvWishlist.setVisibility(View.GONE);
                } else {
                    binding.layoutEmpty.setVisibility(View.GONE);
                    binding.rvWishlist.setVisibility(View.VISIBLE);
                    applyFilters();
                }
                loadRecommendations();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "Lỗi tải wishlist: " + message, Toast.LENGTH_SHORT).show();
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                binding.rvWishlist.setVisibility(View.GONE);
                loadRecommendations();
            }
        });
    }

    private void applyFilters() {
        displayedProducts = allFavoriteProducts.stream()
            .filter(p -> {
                // Status Filter
                if (currentFilterStatus.equals("in_stock")) return p.getStock() > 0;
                if (currentFilterStatus.equals("out_of_stock")) return p.getStock() <= 0;
                return true;
            })
            .filter(p -> {
                // Discount Filter
                if (filterDiscountOnly) {
                    return p.getDiscountPrice() != null && p.getDiscountPrice() > 0 && p.getDiscountPrice() < p.getPrice();
                }
                return true;
            })
            .filter(p -> {
                // Category Filter
                if (currentFilterCategoryId.equals("all")) return true;
                return currentFilterCategoryId.equals(p.getCategory());
            })
            .collect(Collectors.toList());
            
        wishlistAdapter.updateList(displayedProducts);
        
        if (displayedProducts.isEmpty() && !allFavoriteProducts.isEmpty()) {
            Toast.makeText(getContext(), "Không có sản phẩm phù hợp với bộ lọc", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(getContext(), ProductDetailActivity.class);
        intent.putExtra("product_id", product.get_id());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Product product, int position) {
        com.project.utils.SessionManager sessionManager = new com.project.utils.SessionManager(requireContext());
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String productId = product.get_id() != null ? product.get_id() : product.getId();
        if (product.isFavorite()) {
            // Remove from wishlist
            WishlistManager.getInstance(getContext()).removeFromWishlist(productId, new WishlistManager.WishlistCallback<WishlistResponse>() {
                @Override
                public void onSuccess(WishlistResponse result) {
                    product.setFavorite(false);
                    allFavoriteProducts.remove(product);
                    updateWishlistCount();
                    applyFilters();
                    Toast.makeText(getContext(), "Đã xóa khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show();
                    if (allFavoriteProducts.isEmpty()) {
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                        binding.rvWishlist.setVisibility(View.GONE);
                    }
                    loadRecommendations();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(getContext(), "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Add to wishlist (should not usually happen on wishlist page itself, but just in case)
            WishlistManager.getInstance(getContext()).addToWishlist(productId, new WishlistManager.WishlistCallback<WishlistResponse>() {
                @Override
                public void onSuccess(WishlistResponse result) {
                    product.setFavorite(true);
                    if (!allFavoriteProducts.contains(product)) {
                        allFavoriteProducts.add(product);
                        updateWishlistCount();
                    }
                    applyFilters();
                    Toast.makeText(getContext(), "Đã thêm vào danh sách yêu thích", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(getContext(), "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onAddToCartClick(Product product) {
        if (product.getStock() <= 0) {
            Toast.makeText(getContext(), "Sản phẩm đã hết hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        
        com.project.muse_android.cart.ProductVariantBottomSheetFragment bottomSheet = 
                new com.project.muse_android.cart.ProductVariantBottomSheetFragment(product);
        bottomSheet.setOnVariantSelectedListener((color, size, quantity) -> {
            CartManager.getInstance(requireContext()).addToCart(product, color, size, quantity, new CartManager.CartCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(getContext(), "Đã thêm " + product.getName() + " vào giỏ hàng", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(getContext(), "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });
        bottomSheet.show(getParentFragmentManager(), "ProductVariant");
    }

    @Override
    public void onFindSimilarClick(Product product) {
        // Just search for the product name or category
        Intent intent = new Intent(getContext(), SearchActivity.class);
        intent.putExtra("search_query", product.getName());
        startActivity(intent);
    }

    @Override
    public void onSelectionChanged() {
        int selectedCount = wishlistAdapter.getSelectedProducts().size();
        binding.btnUnfavorite.setText("Bỏ thích (" + selectedCount + ")");
        binding.cbSelectAll.setChecked(selectedCount == displayedProducts.size() && !displayedProducts.isEmpty());
    }

    private void updateWishlistCount() {
        if (binding == null) return;
        String countText = "(" + allFavoriteProducts.size() + ")";
        binding.tvWishlistCount.setText(countText);
    }

    private void adjustScrollPadding() {
        if (binding == null) return;
        
        int bottomPadding = currentBottomInset;
        
        // If edit footer is visible, we need to add its height to the scroll padding
        if (binding.layoutEditFooter.getVisibility() == View.VISIBLE) {
            // We use a post to wait for layout if height is 0
            binding.layoutEditFooter.post(() -> {
                if (binding == null) return;
                int footerHeight = binding.layoutEditFooter.getHeight();
                binding.nestedScrollView.setPadding(
                    binding.nestedScrollView.getPaddingLeft(),
                    binding.nestedScrollView.getPaddingTop(),
                    binding.nestedScrollView.getPaddingRight(),
                    footerHeight > 0 ? footerHeight : (int)(80 * getResources().getDisplayMetrics().density)
                );
            });
            return;
        }
        
        binding.nestedScrollView.setPadding(
            binding.nestedScrollView.getPaddingLeft(),
            binding.nestedScrollView.getPaddingTop(),
            binding.nestedScrollView.getPaddingRight(),
            bottomPadding
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
