package com.project.muse_android.product;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.project.adapters.CategoryAdapter;
import com.project.adapters.ProductAdapter;
import com.project.models.Category;
import com.project.models.Product;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityCategoryProductsBinding;
import com.project.muse_android.main.MainActivity;
import com.project.muse_android.search.SearchActivity;
import com.project.network.HomeApiClient;
import com.project.network.HomeApiService;
import com.project.utils.SessionManager;
import com.project.utils.ViewUtils;
import com.project.utils.WishlistManager;
import com.project.models.WishlistResponse;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryProductsActivity extends AppCompatActivity {

    private ActivityCategoryProductsBinding binding;
    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;

    private final List<Product> allProducts = new ArrayList<>();
    private final List<Product> displayProducts = new ArrayList<>();
    private final List<Category> categoryList = new ArrayList<>();

    private String selectedCategoryId;
    private HomeApiService homeApiService;

    // Filter states
    private final Set<String> selectedSizes = new HashSet<>();
    private double minPrice = 0;
    private double maxPrice = Double.MAX_VALUE;
    private int minRating = 0;
    private int currentSortType = 0;

    // Constants for filter sections
    private static final int FILTER_TYPE_ALL = 0;
    private static final int FILTER_TYPE_PRICE = 2;
    private static final int FILTER_TYPE_SIZE = 4;
    private static final int FILTER_TYPE_STAR = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityCategoryProductsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        selectedCategoryId = getIntent().getStringExtra("category_id");
        homeApiService = HomeApiClient.getHomeApiService();

        // Apply padding to avoid status bar overlap
        ViewUtils.applySystemBarsPadding(binding.topBar, true, false);

        setupUI();
        loadCategories();
        loadAllProducts();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        
        binding.btnCart.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("open_cart", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // Horizontal Categories
        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            if (category.getId().equals(selectedCategoryId)) return;
            selectedCategoryId = category.getId();

            if ("all".equals(selectedCategoryId)) {
                binding.txtHeaderTitle.setText("TẤT CẢ SẢN PHẨM");
                binding.rvHorizontalCategories.smoothScrollToPosition(0);
            } else {
                binding.txtHeaderTitle.setText(category.getName().toUpperCase());
                centerCategoryItem(category.getId());
            }

            applyFiltersAndSort();
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
            SessionManager sessionManager = new SessionManager(this);
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            // Manual toggle
            product.setFavorite(!product.isFavorite());
            productAdapter.notifyDataSetChanged();

            String productId = product.get_id() != null ? product.get_id() : product.getId();
            if (product.isFavorite()) {
                WishlistManager.getInstance(this).addToWishlist(productId, new WishlistManager.WishlistCallback<WishlistResponse>() {
                    @Override
                    public void onSuccess(WishlistResponse result) {
                        Toast.makeText(CategoryProductsActivity.this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onError(String message) {
                        product.setFavorite(false);
                        productAdapter.notifyDataSetChanged();
                        Toast.makeText(CategoryProductsActivity.this, "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                WishlistManager.getInstance(this).removeFromWishlist(productId, new WishlistManager.WishlistCallback<WishlistResponse>() {
                    @Override
                    public void onSuccess(WishlistResponse result) {
                        Toast.makeText(CategoryProductsActivity.this, "Đã xóa khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onError(String message) {
                        product.setFavorite(true);
                        productAdapter.notifyDataSetChanged();
                        Toast.makeText(CategoryProductsActivity.this, "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        
        binding.rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvProducts.setAdapter(productAdapter);

        // Filter Actions
        binding.btnOpenFilter.setOnClickListener(v -> showFilterBottomSheet(FILTER_TYPE_ALL));
        binding.btnFilterPrice.setOnClickListener(v -> showFilterBottomSheet(FILTER_TYPE_PRICE));
        binding.btnFilterSize.setOnClickListener(v -> showFilterBottomSheet(FILTER_TYPE_SIZE));
        binding.btnFilterStar.setOnClickListener(v -> showFilterBottomSheet(FILTER_TYPE_STAR));
        binding.btnSort.setOnClickListener(v -> showSortOptions());
    }

    private void showSortOptions() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_sort_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);

        final int[] tempSortType = {currentSortType};
        LinearLayout container = view.findViewById(R.id.sortOptionsContainer);
        String[] options = {
                "Hàng mới về", "Xếp hạng cao đến thấp", "Xếp hạng thấp đến cao",
                "Giá cao đến thấp", "Giá thấp đến cao", "Lượt mua nhiều nhất", "Lượt mua ít nhất"
        };

        for (int i = 0; i < options.length; i++) {
            final int index = i;
            View itemView = getLayoutInflater().inflate(R.layout.item_filter_category, container, false);
            TextView txtName = itemView.findViewById(R.id.txtCategoryName);
            CheckBox cb = itemView.findViewById(R.id.imgSelected);

            cb.setButtonDrawable(R.drawable.ic_custom_radio);
            txtName.setText(options[index]);
            cb.setVisibility(View.VISIBLE);
            cb.setChecked(tempSortType[0] == index);
            
            boolean isSelected = (tempSortType[0] == index);
            txtName.setTextColor(isSelected ? android.graphics.Color.BLACK : android.graphics.Color.parseColor("#999999"));
            if (isSelected) txtName.setTypeface(null, android.graphics.Typeface.BOLD);

            itemView.setOnClickListener(v -> {
                tempSortType[0] = index;
                for (int j = 0; j < container.getChildCount(); j++) {
                    View child = container.getChildAt(j);
                    CheckBox childCb = child.findViewById(R.id.imgSelected);
                    TextView childText = child.findViewById(R.id.txtCategoryName);
                    boolean isTarget = (j == index);
                    childCb.setChecked(isTarget);
                    childText.setTextColor(isTarget ? android.graphics.Color.BLACK : android.graphics.Color.parseColor("#999999"));
                    childText.setTypeface(null, isTarget ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                }
            });
            container.addView(itemView);
        }

        view.findViewById(R.id.btnResetSort).setOnClickListener(v -> {
            tempSortType[0] = 0;
            for (int j = 0; j < container.getChildCount(); j++) {
                CheckBox childCb = container.getChildAt(j).findViewById(R.id.imgSelected);
                TextView childText = container.getChildAt(j).findViewById(R.id.txtCategoryName);
                childCb.setChecked(j == 0);
                childText.setTextColor(j == 0 ? android.graphics.Color.BLACK : android.graphics.Color.parseColor("#999999"));
                childText.setTypeface(null, j == 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        });

        view.findViewById(R.id.btnApplySort).setOnClickListener(v -> {
            currentSortType = tempSortType[0];
            applyFiltersAndSort();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void showFilterBottomSheet(int filterType) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_filter_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);

        final Set<String> tempSizes = new HashSet<>(selectedSizes);
        final double[] tempPriceRange = {minPrice, maxPrice};
        final int[] tempRating = {minRating};

        view.findViewById(R.id.sectionCategory).setVisibility(View.GONE);
        TextView txtFilterTitle = view.findViewById(R.id.txtFilterTitle);

        GridLayout contentSize = view.findViewById(R.id.contentSize);
        contentSize.removeAllViews();
        String[] sizeOptions = {"XXS", "XS", "S", "M", "L", "XL", "XXL", "3XL"};
        for (String s : sizeOptions) {
            CheckBox cb = new CheckBox(this);
            cb.setText(s);
            cb.setButtonDrawable(R.drawable.ic_custom_checkbox);
            cb.setPadding(16, 0, 0, 0); 
            cb.setTextSize(14);
            cb.setTextColor(android.graphics.Color.parseColor("#333333"));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 120; 
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            cb.setLayoutParams(params);
            cb.setGravity(android.view.Gravity.CENTER_VERTICAL);

            cb.setChecked(tempSizes.contains(s));
            cb.setOnCheckedChangeListener((bv, isChecked) -> { if (isChecked) tempSizes.add(s); else tempSizes.remove(s); });
            contentSize.addView(cb);
        }

        LinearLayout priceLayout = (LinearLayout) ((LinearLayout)view.findViewById(R.id.contentPrice)).getChildAt(0);
        EditText edtMin = (EditText) priceLayout.getChildAt(0);
        EditText edtMax = (EditText) priceLayout.getChildAt(2);
        if (tempPriceRange[0] > 0) edtMin.setText(String.valueOf((int)tempPriceRange[0]));
        if (tempPriceRange[1] < Double.MAX_VALUE) edtMax.setText(String.valueOf((int)tempPriceRange[1]));

        view.findViewById(R.id.chipPrice1).setOnClickListener(v -> { edtMin.setText("0"); edtMax.setText("500000"); });
        view.findViewById(R.id.chipPrice2).setOnClickListener(v -> { edtMin.setText("500000"); edtMax.setText("1000000"); });
        view.findViewById(R.id.chipPrice3).setOnClickListener(v -> { edtMin.setText("1000000"); edtMax.setText(""); });

        LinearLayout contentStar = view.findViewById(R.id.contentStar);
        String[] starLabels = {"Từ 1 sao", "Từ 2 sao", "Từ 3 sao", "Từ 4 sao", "5 sao"};
        for (int k = 0; k < contentStar.getChildCount(); k++) {
            if (contentStar.getChildAt(k) instanceof CheckBox) {
                CheckBox cb = (CheckBox) contentStar.getChildAt(k);
                int rVal = k + 1;
                cb.setText(starLabels[k]);
                cb.setButtonDrawable(R.drawable.ic_custom_checkbox);
                cb.setPadding(16, 0, 0, 0);
                cb.setGravity(android.view.Gravity.CENTER_VERTICAL);
                cb.setChecked(tempRating[0] == rVal);
                cb.setOnClickListener(v -> {
                    for (int j = 0; j < contentStar.getChildCount(); j++) if (contentStar.getChildAt(j) instanceof CheckBox) ((CheckBox)contentStar.getChildAt(j)).setChecked(false);
                    cb.setChecked(true); tempRating[0] = rVal;
                });
            }
        }

        if (filterType != FILTER_TYPE_ALL) {
            view.findViewById(R.id.sectionSize).setVisibility(filterType == FILTER_TYPE_SIZE ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.sectionPrice).setVisibility(filterType == FILTER_TYPE_PRICE ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.sectionStar).setVisibility(filterType == FILTER_TYPE_STAR ? View.VISIBLE : View.GONE);
            if (filterType == FILTER_TYPE_SIZE) { view.findViewById(R.id.contentSize).setVisibility(View.VISIBLE); ((TextView)view.findViewById(R.id.iconSize)).setText("-"); txtFilterTitle.setText("Kích cỡ"); }
            if (filterType == FILTER_TYPE_PRICE) { view.findViewById(R.id.contentPrice).setVisibility(View.VISIBLE); ((TextView)view.findViewById(R.id.iconPrice)).setText("-"); txtFilterTitle.setText("Khoảng giá"); }
            if (filterType == FILTER_TYPE_STAR) { view.findViewById(R.id.contentStar).setVisibility(View.VISIBLE); ((TextView)view.findViewById(R.id.iconStar)).setText("-"); txtFilterTitle.setText("Sao đánh giá"); }
        } else {
            txtFilterTitle.setText("Bộ lọc");
            setupAccordion(view);
        }

        view.findViewById(R.id.btnResetFilter).setOnClickListener(v -> {
            if (filterType == FILTER_TYPE_ALL) {
                selectedSizes.clear(); minPrice = 0; maxPrice = Double.MAX_VALUE; minRating = 0;
                applyFiltersAndSort();
                bottomSheetDialog.dismiss();
            } else {
                switch(filterType) {
                    case FILTER_TYPE_SIZE: tempSizes.clear(); break;
                    case FILTER_TYPE_PRICE: tempPriceRange[0]=0; tempPriceRange[1]=Double.MAX_VALUE; edtMin.setText(""); edtMax.setText(""); break;
                    case FILTER_TYPE_STAR: tempRating[0]=0; break;
                }
                if (filterType == FILTER_TYPE_SIZE) for (int i=0; i<contentSize.getChildCount(); i++) ((CheckBox)contentSize.getChildAt(i)).setChecked(false);
                else if (filterType == FILTER_TYPE_STAR) {
                    for (int i=0; i<contentStar.getChildCount(); i++) if (contentStar.getChildAt(i) instanceof CheckBox) ((CheckBox)contentStar.getChildAt(i)).setChecked(false);
                }
            }
        });

        view.findViewById(R.id.btnApplyFilter).setOnClickListener(v -> {
            selectedSizes.clear(); selectedSizes.addAll(tempSizes);
            try { minPrice = edtMin.getText().toString().isEmpty() ? 0 : Double.parseDouble(edtMin.getText().toString()); } catch(Exception e){ minPrice=0; }
            try { maxPrice = edtMax.getText().toString().isEmpty() ? Double.MAX_VALUE : Double.parseDouble(edtMax.getText().toString()); } catch(Exception e){ maxPrice=Double.MAX_VALUE; }
            minRating = tempRating[0];
            applyFiltersAndSort();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void setupAccordion(View view) {
        View[] contents = {view.findViewById(R.id.contentSize), view.findViewById(R.id.contentPrice), view.findViewById(R.id.contentStar)};
        TextView[] icons = {view.findViewById(R.id.iconSize), view.findViewById(R.id.iconPrice), view.findViewById(R.id.iconStar)};
        View[] headers = {view.findViewById(R.id.headerSize), view.findViewById(R.id.headerPrice), view.findViewById(R.id.headerStar)};
        for (int i=0; i<headers.length; i++) {
            int idx = i;
            headers[i].setOnClickListener(v -> {
                boolean exp = contents[idx].getVisibility() == View.VISIBLE;
                for (int j=0; j<contents.length; j++) { contents[j].setVisibility(View.GONE); icons[j].setText("+"); }
                if (!exp) { contents[idx].setVisibility(View.VISIBLE); icons[idx].setText("-"); }
            });
        }
    }

    private void applyFiltersAndSort() {
        displayProducts.clear();
        for (Product p : allProducts) {
            if (p.getStatus() != null && !p.getStatus().equalsIgnoreCase("active")) continue;
            
            // Horizontal Category Filter
            if (selectedCategoryId != null && !selectedCategoryId.equals("all") && !selectedCategoryId.equals(p.getCategory())) {
                boolean catMatch = false;
                for (Category cat : categoryList) {
                    if (cat.getId().equals(selectedCategoryId) && cat.getName().equalsIgnoreCase(p.getCategory())) {
                        catMatch = true;
                        break;
                    }
                }
                if (!catMatch) continue;
            }
            
            double price = p.getFinalPrice();
            if (price < minPrice || price > maxPrice) continue;
            if (minRating > 0 && p.getRating() < (double)minRating) continue;

            if (!selectedSizes.isEmpty()) {
                boolean sm = false;
                if (p.getVariants() != null) for (com.project.models.ProductVariant v : p.getVariants()) if (v.getSize() != null && selectedSizes.contains(v.getSize().trim().toUpperCase())) { sm=true; break; }
                if (!sm && p.getSizes() != null) for (Product.ProductSize s : p.getSizes()) if (s.getSize() != null && selectedSizes.contains(s.getSize().trim().toUpperCase())) { sm=true; break; }
                if (!sm) continue;
            }
            displayProducts.add(p);
        }

        Collections.sort(displayProducts, (p1, p2) -> {
            switch (currentSortType) {
                case 0: return Boolean.compare(p2.isNew(), p1.isNew());
                case 1: return Double.compare(p2.getRating(), p1.getRating());
                case 2: return Double.compare(p1.getRating(), p2.getRating());
                case 3: return Double.compare(p2.getFinalPrice(), p1.getFinalPrice());
                case 4: return Double.compare(p1.getFinalPrice(), p2.getFinalPrice());
                case 5: return Integer.compare(p2.getSoldCount(), p1.getSoldCount());
                case 6: return Integer.compare(p1.getSoldCount(), p2.getSoldCount());
                default: return 0;
            }
        });

        productAdapter.setData(displayProducts);
        binding.txtProductCount.setText(String.format(Locale.getDefault(), "%d sản phẩm phù hợp", displayProducts.size()));
        updateFilterChipsUI();
    }

    private void updateFilterChipsUI() {
        String pTxt = "Khoảng giá";
        if (minPrice > 0 || maxPrice < Double.MAX_VALUE) {
            if (maxPrice == Double.MAX_VALUE) pTxt = "> " + (int)(minPrice/1000) + "k";
            else if (minPrice == 0) pTxt = "< " + (int)(maxPrice/1000) + "k";
            else pTxt = (int)(minPrice/1000) + "k - " + (int)(maxPrice/1000) + "k";
        }
        updateChipStyle(binding.btnFilterPrice, binding.txtFilterPrice, (minPrice > 0 || maxPrice < Double.MAX_VALUE), pTxt);

        String sTxt = "Kích cỡ";
        if (!selectedSizes.isEmpty()) sTxt = selectedSizes.size() == 1 ? selectedSizes.iterator().next() : selectedSizes.size() + " cỡ";
        updateChipStyle(binding.btnFilterSize, binding.txtFilterSize, !selectedSizes.isEmpty(), sTxt);

        String stTxt = "Sao đánh giá";
        if (minRating > 0) stTxt = "Từ " + minRating + " sao";
        updateChipStyle(binding.btnFilterStar, binding.txtFilterStar, minRating > 0, stTxt);
    }

    private void updateChipStyle(View chip, TextView tv, boolean active, String text) {
        tv.setText(text);
        if (active) {
            chip.setBackgroundResource(R.drawable.bg_filter_button_active);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setTextColor(android.graphics.Color.parseColor("#FB6F92"));
        } else {
            chip.setBackgroundResource(R.drawable.bg_filter_button);
            tv.setTypeface(null, android.graphics.Typeface.NORMAL);
            tv.setTextColor(android.graphics.Color.parseColor("#333333"));
        }
    }

    private void centerCategoryItem(String categoryId) {
        int position = -1;
        for (int i = 0; i < categoryList.size(); i++) {
            if (categoryList.get(i).getId().equals(categoryId)) {
                position = i;
                break;
            }
        }

        if (position != -1 && binding != null && binding.rvHorizontalCategories.getLayoutManager() != null) {
            final int targetPos = position;
            final RecyclerView rv = binding.rvHorizontalCategories;
            final LinearLayoutManager layoutManager = (LinearLayoutManager) rv.getLayoutManager();

            rv.post(() -> {
                View targetView = layoutManager.findViewByPosition(targetPos);
                int offset = 0;
                if (targetPos == 0) offset = 0;
                else if (targetView != null) {
                    offset = (rv.getWidth() / 2) - (targetView.getWidth() / 2);
                } else {
                    float density = getResources().getDisplayMetrics().density;
                    offset = (rv.getWidth() / 2) - (int) (50 * density);
                }
                layoutManager.scrollToPositionWithOffset(targetPos, offset);
            });
        }
    }

    private void loadCategories() {
        homeApiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryList.clear();
                    Category allCategory = new Category();
                    allCategory.setId("all");
                    allCategory.setName("Tất cả");
                    categoryList.add(allCategory);

                    if ("all".equals(selectedCategoryId)) {
                        binding.txtHeaderTitle.setText("TẤT CẢ SẢN PHẨM");
                    }

                    for (Category cat : response.body()) {
                        String status = cat.getStatus();
                        if (status == null || "active".equalsIgnoreCase(status) || "featured".equalsIgnoreCase(status)) {
                            categoryList.add(cat);
                            if (cat.getId().equals(selectedCategoryId)) {
                                binding.txtHeaderTitle.setText(cat.getName().toUpperCase());
                            }
                        }
                    }
                    categoryAdapter.notifyDataSetChanged();
                    if (selectedCategoryId != null) { 
                        categoryAdapter.setSelectedCategoryId(selectedCategoryId);
                        if ("all".equals(selectedCategoryId)) binding.rvHorizontalCategories.scrollToPosition(0);
                        else binding.rvHorizontalCategories.post(() -> centerCategoryItem(selectedCategoryId));
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {}
        });
    }

    private void loadAllProducts() {
        binding.progressBar.setVisibility(View.VISIBLE);
        homeApiService.searchProducts("").enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allProducts.clear();
                    allProducts.addAll(response.body());
                    applyFiltersAndSort();
                }
            }
            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(CategoryProductsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String removeAccents(String s) {
        if (s == null) return "";
        String nfdNormalizedString = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").toLowerCase(Locale.getDefault()).replace("đ", "d").replace("Đ", "d");
    }
}
