package com.project.muse_android.home;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LayoutAnimationController;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.project.adapters.BannerAdapter;
import com.project.adapters.CategoryAdapter;
import com.project.adapters.ProductAdapter;
import com.project.models.Banner;
import com.project.models.Category;
import com.project.models.Product;
import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentHomeBinding;
import com.project.muse_android.product.ProductDetailActivity;
import com.project.muse_android.search.SearchActivity;
import com.project.network.HomeApiClient;
import com.project.network.HomeApiService;
import com.project.network.ApiService;
import com.project.utils.SessionManager;
import com.project.utils.ViewUtils;
import com.project.utils.WishlistManager;
import com.project.models.WishlistResponse;
import com.project.muse_android.dialog.NewMemberOfferBottomSheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static boolean isOfferDialogShownInSession = false;

    private FragmentHomeBinding binding;
    private CategoryAdapter categoryAdapter;
    private BannerAdapter bannerAdapter;
    private ProductAdapter productAdapter;

    private final List<Category> categoryList = new ArrayList<>();
    private final List<Banner> bannerList = new ArrayList<>();
    private final List<Product> allProducts = new ArrayList<>();
    private final List<Product> hotProducts = new ArrayList<>();
    private final List<Product> newProducts = new ArrayList<>();
    private final List<Product> displayProducts = new ArrayList<>();

    private HomeApiService homeApiService;
    private ApiService apiService;

    private final Handler slideHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;
    private int selectedTab = 0; // 0: Hot, 1: New

    private final List<Category> allCategories = new ArrayList<>();

    private final ActivityResultLauncher<Intent> searchLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    boolean shouldClear = result.getData().getBooleanExtra("clear_search", false);
                    if (shouldClear && binding != null) {
                        binding.edtSearch.setText("");
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo các API Service từ HomeApiClient để đảm bảo cấu hình GSON đúng
        homeApiService = HomeApiClient.getHomeApiService();
        apiService = HomeApiClient.getApiService();

        setupRecyclerViews();
        setupViewPager();
        setupClickEffects();
        setupSearchBarInteraction();
        setupTabInteraction();
        setupScrollBehavior();

        // Sử dụng Helper để tự động đẩy Header xuống dưới Status Bar
        ViewUtils.applySystemBarsPadding(binding.header, true, false);

        binding.btnViewAllCategories.setVisibility(View.GONE);

        View.OnClickListener toExplore = v -> {
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), com.project.muse_android.main.MainActivity.class);
                intent.putExtra("open_explore", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        };

        binding.txtCategoryTitle.setOnClickListener(toExplore);
        binding.btnViewAllCategories.setOnClickListener(toExplore);

        setInitialStates();

        loadBanners();
        loadCategories();
        loadProducts();

        new Handler(Looper.getMainLooper()).postDelayed(this::playEntranceAnimation, 300);
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAndShowNewMemberOffer, 2000);
    }

    private void setInitialStates() {
        if (binding == null) return;
        // Đặt alpha là 1 để không bị mờ lúc bắt đầu
        binding.header.setAlpha(1f);
        binding.searchBar.setAlpha(1f);

        if (binding.rvCategories != null && binding.rvCategories.getParent() instanceof LinearLayout) {
            LinearLayout contentLayout = (LinearLayout) binding.rvCategories.getParent();
            for (int i = 0; i < contentLayout.getChildCount(); i++) {
                contentLayout.getChildAt(i).setAlpha(1f);
            }
        } else {
            binding.rvProducts.setAlpha(1f);
        }
    }

    private void playEntranceAnimation() {
        if (binding == null) return;
        long duration = 600;
        float startY = 40f * getResources().getDisplayMetrics().density;

        animateEntrance(binding.header, 0, duration, startY);
        animateEntrance(binding.searchBar, 100, duration, startY);

        if (binding.rvCategories != null && binding.rvCategories.getParent() instanceof LinearLayout) {
            LinearLayout content = (LinearLayout) binding.rvCategories.getParent();
            // Indices based on fragment_home.xml: 0:Banner, 1:Title, 2:rvCategories, 3:Tabs, 4:rvProducts
            if (content.getChildCount() > 0) animateEntrance(content.getChildAt(0), 200, duration, startY); // Banner
            if (content.getChildCount() > 1) animateEntrance(content.getChildAt(1), 300, duration, startY); // Title
            if (content.getChildCount() > 2) animateEntrance(content.getChildAt(2), 400, duration, startY); // rvCategories
            if (content.getChildCount() > 3) animateEntrance(content.getChildAt(3), 500, duration, startY); // Tabs
        }

        animateEntrance(binding.rvProducts, 600, duration, startY);
        
        // Cảnh báo: Nếu sau 1.5s mà vẫn có view bị ẩn (alpha=0), hiện nó lên ngay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (binding != null) {
                binding.header.setAlpha(1f);
                binding.rvProducts.setAlpha(1f);
                // v.v... đảm bảo UI không bị "chết" ở trạng thái alpha=0
            }
        }, 1500);
    }

    private void animateEntrance(View view, long delay, long duration, float startY) {
        if (view == null) return;
        view.setTranslationY(startY);
        view.animate()
                .alpha(1f) // Giữ alpha 1 để không bị hiệu ứng mờ (fade in)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void setupRecyclerViews() {
        // Category RV
        binding.rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 3));
        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            if ("all".equals(category.getId())) {
                // Chuyển sang tab Explore (Khám phá) nếu chọn "Tất cả"
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), com.project.muse_android.main.MainActivity.class);
                    intent.putExtra("open_explore", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
            } else {
                // Chuyển sang trang danh mục cụ thể (CategoryProductsFragment)
                Bundle bundle = new Bundle();
                bundle.putString("category_id", category.getId());
                Navigation.findNavController(binding.getRoot())
                        .navigate(R.id.navigation_category_products, bundle);
            }
        });

        // DISABLE selection highlight for Home screen
        categoryAdapter.setSelectionEnabled(false);

        binding.rvCategories.setAdapter(categoryAdapter);

        // Product RV
        binding.rvProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));
        productAdapter = new ProductAdapter(displayProducts, ProductAdapter.TYPE_VERTICAL, product -> {
            Intent intent = new Intent(getContext(), ProductDetailActivity.class);
            intent.putExtra("product_id", product.get_id());
            startActivity(intent);
        });
        productAdapter.setOnFavoriteClickListener(product -> {
            SessionManager sessionManager = new SessionManager(requireContext());
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            // Manually toggle because we removed auto-toggle from Adapter
            product.setFavorite(!product.isFavorite());
            productAdapter.notifyDataSetChanged();

            String productId = product.get_id() != null ? product.get_id() : product.getId();
            if (product.isFavorite()) {
                WishlistManager.getInstance(getContext()).addToWishlist(productId, new WishlistManager.WishlistCallback<WishlistResponse>() {
                    @Override
                    public void onSuccess(WishlistResponse result) {
                        Toast.makeText(getContext(), "Đã thêm vào yêu thích: " + product.getName(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        product.setFavorite(false);
                        productAdapter.notifyDataSetChanged();
                        Toast.makeText(getContext(), "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                WishlistManager.getInstance(getContext()).removeFromWishlist(productId, new WishlistManager.WishlistCallback<WishlistResponse>() {
                    @Override
                    public void onSuccess(WishlistResponse result) {
                        Toast.makeText(getContext(), "Đã xóa khỏi yêu thích: " + product.getName(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        product.setFavorite(true);
                        productAdapter.notifyDataSetChanged();
                        Toast.makeText(getContext(), "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        binding.rvProducts.setAdapter(productAdapter);

        // Ta tạm thời gỡ bỏ LayoutAnimation để kiểm tra hiển thị thuần túy
        binding.rvProducts.setLayoutAnimation(null);
    }

    private void filterProductsByCategory(Category category) {
        // Hàm này không còn dùng nữa
    }

    private void resetTabStyles() {
        // Hàm này không còn dùng nữa
    }

    private void setupViewPager() {
        bannerAdapter = new BannerAdapter(bannerList, banner -> {
            // Chuyển sang tab Explore (Khám phá) thông qua MainActivity
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), com.project.muse_android.main.MainActivity.class);
                intent.putExtra("open_explore", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });
        binding.vpBanners.setAdapter(bannerAdapter);

        binding.vpBanners.setPageTransformer((page, position) -> {
            page.setAlpha(1 - Math.abs(position));
            page.setScaleX(0.9f + (1 - Math.abs(position)) * 0.1f);
        });

        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding != null && !bannerList.isEmpty()) {
                    int nextItem = (binding.vpBanners.getCurrentItem() + 1) % bannerList.size();
                    binding.vpBanners.setCurrentItem(nextItem, true);
                    slideHandler.postDelayed(this, 5000);
                }
            }
        };
        slideHandler.postDelayed(sliderRunnable, 5000);
    }

    private void setupSearchBarInteraction() {
        final GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(24 * getResources().getDisplayMetrics().density);
        bg.setColor(Color.parseColor("#F2F2F2"));
        bg.setStroke((int)(1 * getResources().getDisplayMetrics().density), Color.parseColor("#E0E0E0"));
        binding.searchBar.setBackground(bg);

        binding.edtSearch.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.03f : 1.0f;
            binding.searchBar.animate().scaleX(scale).scaleY(scale).setDuration(180).start();
            int color = hasFocus ? ContextCompat.getColor(requireContext(), R.color.primary_500) : Color.parseColor("#E0E0E0");
            bg.setStroke((int)(1 * getResources().getDisplayMetrics().density), color);
        });
    }

    private void setupTabInteraction() {
        binding.tabHot.setOnClickListener(v -> switchTab(0));
        binding.tabNew.setOnClickListener(v -> switchTab(1));

        // Initial state
        binding.tabHot.setScaleX(1.1f);
        binding.tabHot.setScaleY(1.1f);
        binding.tabHot.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_500));
        binding.tabHot.setTypeface(null, android.graphics.Typeface.BOLD);

        binding.tabNew.setAlpha(0.6f);
    }

    private void switchTab(int index) {
        if (index == selectedTab) return;
        selectedTab = index;

        TextView[] tabs = {binding.tabHot, binding.tabNew};

        for (int i = 0; i < tabs.length; i++) {
            if (i == index) {
                tabs[i].animate().scaleX(1.1f).scaleY(1.1f).alpha(1f).setDuration(250).start();
                tabs[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_500));
                tabs[i].setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tabs[i].animate().scaleX(1f).scaleY(1f).alpha(0.6f).setDuration(250).start();
                tabs[i].setTextColor(Color.parseColor("#D3D3D3"));
                tabs[i].setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }

        updateProductList();
    }

    private void updateProductList() {
        if (binding == null) return;
        displayProducts.clear();

        List<Product> source;
        if (selectedTab == 0) {
            source = hotProducts;
        } else {
            source = newProducts;
        }

        displayProducts.addAll(source);
        Log.d("HomeFragment", "Cập nhật hiển thị: " + displayProducts.size() + " items cho tab " + selectedTab);

        if (productAdapter != null) {
            productAdapter.notifyDataSetChanged();
            // Đảm bảo RV hiển thị và có alpha = 1
            binding.rvProducts.setAlpha(1.0f);
            binding.rvProducts.setVisibility(View.VISIBLE);
            binding.rvProducts.post(() -> {
                binding.rvProducts.requestLayout();
                Log.d("HomeFragment", "RV requestLayout called");
            });
        }
    }

    private void setupScrollBehavior() {
        binding.nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            float ratio = Math.min(scrollY / 400f, 1.0f);
            binding.header.setElevation(ratio * 12f);

            float scale = 1.0f - (ratio * 0.03f);
            binding.searchBar.setScaleX(scale);
            binding.searchBar.setScaleY(scale);
        });
    }

    private void setupClickEffects() {
        View.OnClickListener toSearch = v -> {
            Intent intent = new Intent(getContext(), SearchActivity.class);
            searchLauncher.launch(intent);
        };
        binding.searchBar.setOnClickListener(toSearch);
        binding.edtSearch.setOnClickListener(toSearch);

        applyRipple(binding.searchBar);
        applyRipple(binding.btnChatHome);

        binding.btnChatHome.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), com.project.muse_android.profile.ShopChatActivity.class);
            startActivity(intent);
        });
    }

    private void applyRipple(View view) {
        if (view == null) return;
        view.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ripple_primary_light));
    }

    private void loadBanners() {
        if (binding == null) return;
        binding.vpBanners.setAlpha(0.5f);
        homeApiService.getBanners().enqueue(new Callback<List<Banner>>() {
            @Override
            public void onResponse(Call<List<Banner>> call, Response<List<Banner>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    bannerList.clear();
                    for (Banner banner : response.body()) {
                        if (banner.getStatus() == null || "active".equalsIgnoreCase(banner.getStatus())) {
                            bannerList.add(banner);
                        }
                    }
                    bannerAdapter.notifyDataSetChanged();
                    binding.vpBanners.animate().alpha(1f).setDuration(400).start();
                }
            }
            @Override
            public void onFailure(Call<List<Banner>> call, Throwable t) {}
        });
    }

    private void loadCategories() {
        if (binding == null) return;
        binding.rvCategories.setAlpha(0.5f);
        homeApiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    allCategories.clear();

                    // Thêm danh mục "Tất cả" với Logo
                    Category allCategory = new Category();
                    allCategory.setId("all");
                    allCategory.setName("Tất cả");
                    allCategories.add(allCategory);

                    for (Category cat : response.body()) {
                        String status = cat.getStatus();
                        if (status == null || "active".equalsIgnoreCase(status) || "featured".equalsIgnoreCase(status)) {
                            allCategories.add(cat);
                        }
                    }

                    updateCategoryList();
                    binding.rvCategories.animate().alpha(1f).setDuration(400).start();
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {}
        });
    }

    private void updateCategoryList() {
        if (binding == null) return;
        categoryList.clear();
        // Luôn hiện tất cả danh mục
        categoryList.addAll(allCategories);
        categoryAdapter.notifyDataSetChanged();

        // Luôn ẩn nút "Xem tất cả"
        binding.btnViewAllCategories.setVisibility(View.GONE);
    }

    private void loadProducts() {
        binding.rvProducts.setAlpha(1.0f);

        // Dùng apiService.getProducts() để lấy toàn bộ sản phẩm
        apiService.getProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<Product> products = response.body();
                    Log.d("HomeFragment", "Tải thành công " + products.size() + " sản phẩm");

                    allProducts.clear();
                    for (Product p : products) {
                        // Chỉ lấy sản phẩm có trạng thái "active"
                        if (p.getStatus() == null || "active".equalsIgnoreCase(p.getStatus())) {
                            allProducts.add(p);
                        }
                    }

                    // Sync favorites with wishlist API if logged in
                    WishlistManager.getInstance(getContext()).getWishlist(new WishlistManager.WishlistCallback<List<Product>>() {
                        @Override
                        public void onSuccess(List<Product> wishlist) {
                            for (Product p : allProducts) {
                                String pId = p.get_id() != null ? p.get_id() : p.getId();
                                boolean isFav = false;
                                for (Product fav : wishlist) {
                                    String favId = fav.get_id() != null ? fav.get_id() : fav.getId();
                                    if (pId.equals(favId)) {
                                        isFav = true;
                                        break;
                                    }
                                }
                                p.setFavorite(isFav);
                            }
                            processProductsAndDisplay();
                        }

                        @Override
                        public void onError(String message) {
                            for (Product p : allProducts) {
                                p.setFavorite(false);
                            }
                            processProductsAndDisplay();
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                if (binding == null) return;
                Log.e("HomeFragment", "Lỗi: ", t);
                Toast.makeText(getContext(), "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processProductsAndDisplay() {
        if (binding == null) return;
        // 1. Sắp xếp NỔI BẬT (theo lượt bán) - Lấy tối đa 10 sản phẩm
        hotProducts.clear();
        List<Product> sortedHot = new ArrayList<>(allProducts);
        Collections.sort(sortedHot, (p1, p2) -> Integer.compare(p2.getSoldCount(), p1.getSoldCount()));
        int hotLimit = Math.min(sortedHot.size(), 10);
        hotProducts.addAll(sortedHot.subList(0, hotLimit));

        // 2. Sắp xếp MỚI (theo ID) - Lấy tối đa 10 sản phẩm mới nhất
        newProducts.clear();
        List<Product> sortedNew = new ArrayList<>(allProducts);
        Collections.sort(sortedNew, (p1, p2) -> {
            String id1 = p1.get_id() != null ? p1.get_id() : "";
            String id2 = p2.get_id() != null ? p2.get_id() : "";
            return id2.compareTo(id1);
        });
        int newLimit = Math.min(sortedNew.size(), 10);
        newProducts.addAll(sortedNew.subList(0, newLimit));

        updateProductList();
    }

    private void checkAndShowNewMemberOffer() {
        if (isAdded() && getContext() != null) {
            SessionManager sessionManager = new SessionManager(requireContext());
            
            // Chỉ hiện nếu: 
            // 1. Chưa đăng nhập
            // 2. Chưa tick "Không hiện lại"
            // 3. Vừa mới hoàn thành onboarding (lần đầu tải app)
            // 4. Chưa hiện trong session này
            if (!sessionManager.isLoggedIn() 
                    && !sessionManager.isDontShowOfferAgain() 
                    && sessionManager.shouldShowOffer() 
                    && !isOfferDialogShownInSession) {
                
                isOfferDialogShownInSession = true;
                // Sau khi chuẩn bị hiện thì tắt flag đi để lần sau vào lại Home không hiện nữa
                sessionManager.setShouldShowOffer(false);

                NewMemberOfferBottomSheet offerBottomSheet = NewMemberOfferBottomSheet.newInstance();
                offerBottomSheet.show(getParentFragmentManager(), "NewMemberOfferBottomSheet");
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        slideHandler.removeCallbacks(sliderRunnable);
        binding = null;
    }
}
