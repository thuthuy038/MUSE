package com.project.muse_android.search;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.project.adapters.ProductAdapter;
import com.project.models.Category;
import com.project.models.Product;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivitySearchResultBinding;
import com.project.muse_android.product.ProductDetailActivity;
import com.project.muse_android.main.MainActivity;
import com.project.network.ApiResponse;
import com.project.network.HomeApiClient;
import com.project.network.HomeApiService;
import com.project.utils.SessionManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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

public class SearchResultActivity extends AppCompatActivity {

    private ActivitySearchResultBinding binding;
    private ProductAdapter productAdapter;
    private List<Product> allSearchResults = new ArrayList<>();
    private List<Product> displayList = new ArrayList<>();
    private List<Category> categoryList = new ArrayList<>();
    
    private HomeApiService apiService;
    private SearchHistoryManager historyManager;
    private SessionManager sessionManager;

    private SuggestionAdapter suggestionAdapter;
    private List<SuggestionItem> suggestionList = new ArrayList<>();

    private final androidx.activity.result.ActivityResultLauncher<String> recordAudioPermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showVoiceSearchDialog();
                } else {
                    Toast.makeText(this, "Quyền ghi âm âm thanh bị từ chối.", Toast.LENGTH_SHORT).show();
                }
            });

    // Filter types
    private static final int FILTER_TYPE_ALL = 0;
    private static final int FILTER_TYPE_CATEGORY = 1;
    private static final int FILTER_TYPE_PRICE = 2;
    private static final int FILTER_TYPE_SIZE = 4;
    private static final int FILTER_TYPE_STAR = 5;

    // Filter states
    private final Set<String> selectedCategoryIds = new HashSet<>();
    private final Set<String> selectedSizes = new HashSet<>();
    private double minPrice = 0;
    private double maxPrice = Double.MAX_VALUE;
    private int minRating = 0;
    private int currentSortType = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String query = getIntent().getStringExtra("query");
        if (query == null) query = "";

        binding.edtSearchQuery.setText(query);
        apiService = HomeApiClient.getHomeApiService();
        historyManager = new SearchHistoryManager(this);
        sessionManager = new SessionManager(this);

        setupUI();
        setupRecyclerView();
        setupSuggestionRecyclerView();
        loadCategories();
        performSearch(query);
    }

    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryList.clear();
                    for (Category cat : response.body()) {
                        if (cat.getStatus() == null || "active".equalsIgnoreCase(cat.getStatus())) {
                            categoryList.add(cat);
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {}
        });
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> {
            String query = binding.edtSearchQuery.getText().toString().trim();
            if (query.isEmpty()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("clear_search", true);
                setResult(RESULT_OK, resultIntent);
            }
            finish();
        });
        binding.btnClearSearch.setOnClickListener(v -> {
            binding.edtSearchQuery.setText("");
            binding.rvSuggestions.setVisibility(View.GONE);
            performSearch(""); 
            binding.edtSearchQuery.requestFocus();
        });

        binding.imgSearchIcon.setOnClickListener(v -> {
            String newQuery = binding.edtSearchQuery.getText().toString().trim();
            performSearch(newQuery);
            binding.rvSuggestions.setVisibility(View.GONE);
            hideKeyboard();
        });

        binding.edtSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String newQuery = binding.edtSearchQuery.getText().toString().trim();
                performSearch(newQuery);
                binding.rvSuggestions.setVisibility(View.GONE);
                hideKeyboard();
                return true;
            }
            return false;
        });

        binding.edtSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim();
                if (!q.isEmpty()) {
                    fetchSuggestions(q);
                } else {
                    binding.rvSuggestions.setVisibility(View.GONE);
                    suggestionList.clear();
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.imgCart.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("open_cart", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        binding.btnOpenFilter.setOnClickListener(v -> showFilterBottomSheet(FILTER_TYPE_ALL));
        binding.btnFilterCategory.setOnClickListener(v -> showFilterBottomSheet(FILTER_TYPE_CATEGORY));
        binding.btnFilterPrice.setOnClickListener(v -> showFilterBottomSheet(FILTER_TYPE_PRICE));
        binding.btnFilterSize.setOnClickListener(v -> showFilterBottomSheet(FILTER_TYPE_SIZE));
        binding.btnFilterStar.setOnClickListener(v -> showFilterBottomSheet(FILTER_TYPE_STAR));
        binding.btnSort.setOnClickListener(v -> showSortOptions());

        binding.imgVoiceSearch.setOnClickListener(v -> {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                showVoiceSearchDialog();
            } else {
                recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO);
            }
        });

        binding.imgCameraSearch.setOnClickListener(v -> showCameraSearchBottomSheet());
    }

    private void setupSuggestionRecyclerView() {
        suggestionAdapter = new SuggestionAdapter(suggestionList, item -> {
            if (item.type == SuggestionItem.TYPE_KEYWORD) {
                binding.edtSearchQuery.setText(item.text);
                binding.rvSuggestions.setVisibility(View.GONE);
                performSearch(item.text);
            } else if (item.type == SuggestionItem.TYPE_PRODUCT && item.product != null) {
                Intent intent = new Intent(this, ProductDetailActivity.class);
                intent.putExtra("product_id", item.product.get_id());
                startActivity(intent);
            }
        });
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSuggestions.setAdapter(suggestionAdapter);
    }

    private void fetchSuggestions(String query) {
        apiService.searchProducts(query).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    buildSuggestionList(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {}
        });
    }

    private void buildSuggestionList(List<Product> products) {
        suggestionList.clear();
        String currentQuery = binding.edtSearchQuery.getText().toString().trim();
        String normalizedQuery = removeAccents(currentQuery).toLowerCase();

        // 1. "Có phải bạn muốn tìm" Section
        List<Category> matchingCategories = new ArrayList<>();
        for (Category cat : categoryList) {
            String catName = cat.getName();
            if (catName != null && removeAccents(catName).toLowerCase().contains(normalizedQuery)) {
                matchingCategories.add(cat);
            }
        }

        if (!matchingCategories.isEmpty()) {
            suggestionList.add(new SuggestionItem(SuggestionItem.TYPE_HEADER, "Có phải bạn muốn tìm"));
            for (int i = 0; i < Math.min(2, matchingCategories.size()); i++) {
                suggestionList.add(new SuggestionItem(SuggestionItem.TYPE_KEYWORD, matchingCategories.get(i).getName()));
            }
            suggestionList.add(new SuggestionItem(SuggestionItem.TYPE_DIVIDER, ""));
        }

        // 2. "Sản phẩm gợi ý" Section
        List<Product> matchingProducts = new ArrayList<>();
        for (Product p : products) {
            String pName = p.getName();
            String pCat = p.getCategory();
            boolean matches = false;
            if (pName != null && removeAccents(pName).toLowerCase().contains(normalizedQuery)) matches = true;
            if (!matches && pCat != null && removeAccents(pCat).toLowerCase().contains(normalizedQuery)) matches = true;
            if (matches) matchingProducts.add(p);
        }

        if (!matchingProducts.isEmpty()) {
            suggestionList.add(new SuggestionItem(SuggestionItem.TYPE_HEADER, "Sản phẩm gợi ý"));
            int count = 0;
            for (Product p : matchingProducts) {
                suggestionList.add(new SuggestionItem(SuggestionItem.TYPE_PRODUCT, p));
                if (++count >= 5) break;
            }
        }

        if (suggestionList.isEmpty()) {
            binding.rvSuggestions.setVisibility(View.GONE);
        } else {
            suggestionAdapter.notifyDataSetChanged();
            binding.rvSuggestions.setVisibility(View.VISIBLE);
        }
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
            txtName.setText(options[i]);
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

        final Set<String> tempCategoryIds = new HashSet<>(selectedCategoryIds);
        final Set<String> tempSizes = new HashSet<>(selectedSizes);
        final double[] tempPriceRange = {minPrice, maxPrice};
        final int[] tempRating = {minRating};

        TextView txtFilterTitle = view.findViewById(R.id.txtFilterTitle);
        
        View sectionCategory = view.findViewById(R.id.sectionCategory);
        View sectionSize = view.findViewById(R.id.sectionSize);
        View sectionPrice = view.findViewById(R.id.sectionPrice);
        View sectionStar = view.findViewById(R.id.sectionStar);

        LinearLayout contentCategory = view.findViewById(R.id.contentCategory);
        contentCategory.removeAllViews();
        addCategoryFilterItem(contentCategory, "Tất cả", "all", tempCategoryIds);
        for (Category cat : categoryList) {
            addCategoryFilterItem(contentCategory, cat.getName(), cat.getId(), tempCategoryIds);
        }

        GridLayout contentSize = view.findViewById(R.id.contentSize);
        contentSize.removeAllViews();
        String[] sizeOptions = {"XXS", "XS", "S", "M", "L", "XL", "XXL", "3XL"};
        for (String s : sizeOptions) {
            CheckBox cb = new CheckBox(this);
            cb.setText(s);
            cb.setButtonDrawable(R.drawable.ic_custom_checkbox);
            cb.setPadding(16, 0, 0, 0); // Space between box and text
            cb.setTextSize(14);
            cb.setTextColor(android.graphics.Color.parseColor("#333333"));

            // Set margins for grid items
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 120; // Approximately 48dp
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            cb.setLayoutParams(params);
            cb.setGravity(android.view.Gravity.CENTER_VERTICAL);

            cb.setChecked(tempSizes.contains(s));
            cb.setOnCheckedChangeListener((bv, isChecked) -> { if (isChecked) tempSizes.add(s); else tempSizes.remove(s); });
            contentSize.addView(cb);
        }

        LinearLayout priceInputLayout = (LinearLayout) ((LinearLayout)view.findViewById(R.id.contentPrice)).getChildAt(0);
        EditText edtMin = (EditText) priceInputLayout.getChildAt(0);
        EditText edtMax = (EditText) priceInputLayout.getChildAt(2);
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
                cb.setButtonDrawable(R.drawable.ic_custom_checkbox); // Set custom checkbox
                cb.setPadding(16, 0, 0, 0); // Add space for consistency
                cb.setGravity(android.view.Gravity.CENTER_VERTICAL);
                cb.setChecked(tempRating[0] == rVal);
                cb.setOnClickListener(v -> {
                    for (int j = 0; j < contentStar.getChildCount(); j++) if (contentStar.getChildAt(j) instanceof CheckBox) ((CheckBox)contentStar.getChildAt(j)).setChecked(false);
                    cb.setChecked(true); tempRating[0] = rVal;
                });
            }
        }

        if (filterType != FILTER_TYPE_ALL) {
            sectionCategory.setVisibility(filterType == FILTER_TYPE_CATEGORY ? View.VISIBLE : View.GONE);
            sectionSize.setVisibility(filterType == FILTER_TYPE_SIZE ? View.VISIBLE : View.GONE);
            sectionPrice.setVisibility(filterType == FILTER_TYPE_PRICE ? View.VISIBLE : View.GONE);
            sectionStar.setVisibility(filterType == FILTER_TYPE_STAR ? View.VISIBLE : View.GONE);
            if (filterType == FILTER_TYPE_CATEGORY) { view.findViewById(R.id.contentCategory).setVisibility(View.VISIBLE); ((TextView)view.findViewById(R.id.iconCategory)).setText("-"); txtFilterTitle.setText("Danh mục"); }
            if (filterType == FILTER_TYPE_SIZE) { view.findViewById(R.id.contentSize).setVisibility(View.VISIBLE); ((TextView)view.findViewById(R.id.iconSize)).setText("-"); txtFilterTitle.setText("Kích cỡ"); }
            if (filterType == FILTER_TYPE_PRICE) { view.findViewById(R.id.contentPrice).setVisibility(View.VISIBLE); ((TextView)view.findViewById(R.id.iconPrice)).setText("-"); txtFilterTitle.setText("Khoảng giá"); }
            if (filterType == FILTER_TYPE_STAR) { view.findViewById(R.id.contentStar).setVisibility(View.VISIBLE); ((TextView)view.findViewById(R.id.iconStar)).setText("-"); txtFilterTitle.setText("Sao đánh giá"); }
        } else {
            setupAccordion(view);
            txtFilterTitle.setText("Bộ lọc");
        }

        view.findViewById(R.id.btnResetFilter).setOnClickListener(v -> {
            if (filterType == FILTER_TYPE_ALL) {
                selectedCategoryIds.clear(); selectedSizes.clear(); minPrice = 0; maxPrice = Double.MAX_VALUE; minRating = 0;
                applyFiltersAndSort(); bottomSheetDialog.dismiss();
            } else {
                switch(filterType) {
                    case FILTER_TYPE_CATEGORY: tempCategoryIds.clear(); break;
                    case FILTER_TYPE_SIZE: tempSizes.clear(); break;
                    case FILTER_TYPE_PRICE: tempPriceRange[0]=0; tempPriceRange[1]=Double.MAX_VALUE; edtMin.setText(""); edtMax.setText(""); break;
                    case FILTER_TYPE_STAR: tempRating[0]=0; break;
                }
                if (filterType == FILTER_TYPE_CATEGORY) {
                    for (int i=0; i<contentCategory.getChildCount(); i++) {
                        View child = contentCategory.getChildAt(i);
                        CheckBox childCb = child.findViewById(R.id.imgSelected);
                        TextView childText = child.findViewById(R.id.txtCategoryName);
                        childCb.setChecked(i == 0);
                        childText.setTextColor(i == 0 ? android.graphics.Color.BLACK : android.graphics.Color.parseColor("#999999"));
                        childText.setTypeface(null, i == 0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                    }
                } else if (filterType == FILTER_TYPE_SIZE) for (int i=0; i<contentSize.getChildCount(); i++) ((CheckBox)contentSize.getChildAt(i)).setChecked(false);
                else if (filterType == FILTER_TYPE_STAR) for (int i=0; i<contentStar.getChildCount(); i++) ((CheckBox)contentStar.getChildAt(i)).setChecked(false);
            }
        });

        view.findViewById(R.id.btnApplyFilter).setOnClickListener(v -> {
            selectedCategoryIds.clear(); selectedCategoryIds.addAll(tempCategoryIds);
            selectedSizes.clear(); selectedSizes.addAll(tempSizes);
            try { minPrice = edtMin.getText().toString().isEmpty() ? 0 : Double.parseDouble(edtMin.getText().toString()); } catch(Exception e){ minPrice=0; }
            try { maxPrice = edtMax.getText().toString().isEmpty() ? Double.MAX_VALUE : Double.parseDouble(edtMax.getText().toString()); } catch(Exception e){ maxPrice=Double.MAX_VALUE; }
            minRating = tempRating[0];
            applyFiltersAndSort();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void addCategoryFilterItem(LinearLayout container, String name, String id, Set<String> tempSet) {
        View item = getLayoutInflater().inflate(R.layout.item_filter_category, container, false);
        TextView txtName = item.findViewById(R.id.txtCategoryName);
        CheckBox cb = item.findViewById(R.id.imgSelected);
        txtName.setText(name);
        boolean isSelected = (id.equals("all") && tempSet.isEmpty()) || tempSet.contains(id);
        cb.setVisibility(View.VISIBLE); cb.setChecked(isSelected);
        txtName.setTextColor(isSelected ? android.graphics.Color.BLACK : android.graphics.Color.parseColor("#999999"));
        if (isSelected) txtName.setTypeface(null, android.graphics.Typeface.BOLD);

        item.setOnClickListener(v -> {
            if (id.equals("all")) {
                tempSet.clear();
            } else {
                if (tempSet.contains(id)) tempSet.remove(id);
                else tempSet.add(id);
            }
            
            // Update UI for all category items in the bottom sheet
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                String childId = (i == 0) ? "all" : categoryList.get(i - 1).getId();
                boolean childSelected = (childId.equals("all") && tempSet.isEmpty()) || tempSet.contains(childId);
                
                CheckBox childCb = child.findViewById(R.id.imgSelected);
                if (childCb != null) {
                    childCb.setChecked(childSelected);
                    childCb.setVisibility(View.VISIBLE);
                }
                
                TextView childText = child.findViewById(R.id.txtCategoryName);
                if (childText != null) {
                    childText.setTextColor(childSelected ? android.graphics.Color.BLACK : android.graphics.Color.parseColor("#999999"));
                    childText.setTypeface(null, childSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                }
            }
        });
        container.addView(item);
    }

    private void setupAccordion(View view) {
        View[] contents = {view.findViewById(R.id.contentCategory), view.findViewById(R.id.contentSize), view.findViewById(R.id.contentPrice), view.findViewById(R.id.contentStar)};
        TextView[] icons = {view.findViewById(R.id.iconCategory), view.findViewById(R.id.iconSize), view.findViewById(R.id.iconPrice), view.findViewById(R.id.iconStar)};
        View[] headers = {view.findViewById(R.id.headerCategory), view.findViewById(R.id.headerSize), view.findViewById(R.id.headerPrice), view.findViewById(R.id.headerStar)};
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
        displayList.clear();
        for (Product p : allSearchResults) {
            if (p.getStatus() != null && !p.getStatus().equalsIgnoreCase("active")) continue;
            if (!selectedCategoryIds.isEmpty()) {
                String pCat = p.getCategory();
                boolean match = false;
                if (pCat != null) {
                    if (selectedCategoryIds.contains(pCat)) match = true;
                    else {
                        for (String selId : selectedCategoryIds) {
                            for (Category cat : categoryList) if (cat.getId().equals(selId) && cat.getName().equalsIgnoreCase(pCat)) { match = true; break; }
                            if (match) break;
                        }
                    }
                }
                if (!match) continue;
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
            displayList.add(p);
        }

        Collections.sort(displayList, (p1, p2) -> {
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

        productAdapter.setData(displayList);
        binding.txtProductCount.setText(String.format(Locale.getDefault(), "%d sản phẩm phù hợp", displayList.size()));
        updateFilterChipsUI();
    }

    private void updateFilterChipsUI() {
        String catText = "Danh mục";
        if (!selectedCategoryIds.isEmpty()) {
            List<String> selectedNames = new ArrayList<>();
            for (String id : selectedCategoryIds) for (Category cat : categoryList) if (cat.getId().equals(id)) { selectedNames.add(cat.getName()); break; }
            if (selectedNames.size() == 1) catText = selectedNames.get(0);
            else if (selectedNames.size() > 1) catText = selectedNames.size() + " danh mục";
        }
        updateChipStyle(binding.btnFilterCategory, binding.txtFilterCategory, !selectedCategoryIds.isEmpty(), catText);
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

    private void updateChipStyle(android.view.View chipView, android.widget.TextView textView, boolean isActive, String text) {
        textView.setText(text);
        if (isActive) {
            chipView.setBackgroundResource(R.drawable.bg_filter_button_active);
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
            textView.setTextColor(android.graphics.Color.parseColor("#FB6F92"));
        } else {
            chipView.setBackgroundResource(R.drawable.bg_filter_button);
            textView.setTypeface(null, android.graphics.Typeface.NORMAL);
            textView.setTextColor(android.graphics.Color.parseColor("#333333"));
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(displayList, ProductAdapter.TYPE_VERTICAL, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("product_id", product.get_id());
            startActivity(intent);
        });
        productAdapter.setOnFavoriteClickListener(product -> Toast.makeText(this, "Đã thêm vào yêu thích: " + product.getName(), Toast.LENGTH_SHORT).show());
        binding.rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvProducts.setAdapter(productAdapter);
    }

    private void performSearch(String searchQuery) {
        String finalQuery = (searchQuery == null) ? "" : searchQuery.trim();
        boolean isVoice = getIntent().getBooleanExtra("is_voice", false);
        boolean isCamera = getIntent().getBooleanExtra("is_camera", false);
        String imagePath = getIntent().getStringExtra("image_path");

        binding.loadingLayout.setVisibility(View.VISIBLE);
        binding.rvProducts.setVisibility(View.GONE);
        binding.txtSimilarProductsLabel.setVisibility(View.GONE);
        binding.txtLoadingMessage.setText("Đang tìm kiếm sản phẩm...");
        binding.txtProductCount.setText(finalQuery.isEmpty() ? "Đang tải sản phẩm..." : "Đang tìm kiếm...");

        apiService.searchProducts("").enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Product> results = response.body();
                    if (isCamera && imagePath != null && !imagePath.isEmpty()) {
                        getIntent().putExtra("is_camera", false);
                        runGeminiCameraSearch(imagePath, results);
                    } else if (isVoice && !finalQuery.isEmpty()) {
                        getIntent().putExtra("is_voice", false);
                        runGeminiVoiceSearch(finalQuery, results);
                    } else {
                        binding.loadingLayout.setVisibility(View.GONE);
                        binding.rvProducts.setVisibility(View.VISIBLE);
                        allSearchResults.clear();
                        String normalizedQuery = removeAccents(finalQuery).trim();
                        for (Product p : results) {
                            if (p.getStatus() != null && !p.getStatus().equalsIgnoreCase("active")) continue;
                            if (finalQuery.isEmpty() || (p.getName() != null && removeAccents(p.getName()).contains(normalizedQuery))) allSearchResults.add(p);
                        }
                        if (!finalQuery.isEmpty()) {
                            Collections.sort(allSearchResults, (p1, p2) -> {
                                String n1 = removeAccents(p1.getName()); String n2 = removeAccents(p2.getName());
                                boolean s1 = n1.startsWith(normalizedQuery); boolean s2 = n2.startsWith(normalizedQuery);
                                if (s1 && !s2) return -1; if (!s1 && s2) return 1;
                                return Integer.compare(n1.indexOf(normalizedQuery), n2.indexOf(normalizedQuery));
                            });
                            historyManager.addHistory(finalQuery);
                            String userId = sessionManager.getUserId();
                            apiService.recordSearch(finalQuery, (userId != null) ? userId : "guest").enqueue(new Callback<ApiResponse<Void>>() {
                                @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {}
                                @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
                            });
                        }
                        applyFiltersAndSort();
                    }
                } else {
                    binding.loadingLayout.setVisibility(View.GONE);
                    binding.rvProducts.setVisibility(View.VISIBLE);
                    allSearchResults.clear();
                    applyFiltersAndSort();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                binding.loadingLayout.setVisibility(View.GONE);
                binding.rvProducts.setVisibility(View.VISIBLE);
                Toast.makeText(SearchResultActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void runGeminiCameraSearch(String imagePath, List<Product> allProducts) {
        android.util.Log.d("MUSE_CameraSearch", "Starting runGeminiCameraSearch for path: " + imagePath);
        binding.loadingLayout.setVisibility(View.VISIBLE);
        binding.rvProducts.setVisibility(View.GONE);
        binding.txtLoadingMessage.setText("MUSE AI đang xử lý yêu cầu...");
        binding.txtProductCount.setText("Đang phân tích hình ảnh...");

        new Thread(() -> {
            String base64 = getBase64FromImagePath(imagePath);
            if (base64.isEmpty()) {
                android.util.Log.e("MUSE_CameraSearch", "Base64 encoding failed for image: " + imagePath);
                runOnUiThread(() -> performLocalTextSearch("", allProducts, true));
                return;
            }
            android.util.Log.d("MUSE_CameraSearch", "Base64 encoding succeeded. Length: " + base64.length());

            StringBuilder productsCtx = new StringBuilder();
            productsCtx.append("Danh sách sản phẩm cửa hàng:\n");
            for (Product p : allProducts) {
                String pid = p.get_id() != null ? p.get_id() : p.getId();
                productsCtx.append(String.format("- ID: %s | Tên: %s | Danh mục: %s\n", pid, p.getName(), p.getCategory()));
            }

            String prompt = "Hãy phân tích hình ảnh trang phục/phụ kiện của người này. Xác định xem có món đồ nào khớp hoặc gần giống nhất với danh sách sản phẩm hiện có ở shop dưới đây hay không.\n" +
                    "Hãy trả về duy nhất một chuỗi JSON thô chứa các trường sau:\n" +
                    "{\n" +
                    "  \"matched_product_id\": \"ID sản phẩm khớp nhất từ danh sách của shop (nếu khớp trên 85%, ngược lại để null)\",\n" +
                    "  \"similar_product_ids\": [\"ID_1\", \"ID_2\", \"ID_3\"]\n" +
                    "}\n" +
                    "Lưu ý: Chỉ trả về JSON thô, không kèm định dạng markdown ```json.\n\n" +
                    productsCtx.toString();

            java.util.Map<String, Object> body = new java.util.HashMap<>();
            List<java.util.Map<String, Object>> contents = new ArrayList<>();
            java.util.Map<String, Object> contentMap = new java.util.HashMap<>();
            List<java.util.Map<String, Object>> parts = new ArrayList<>();

            java.util.Map<String, Object> textPart = new java.util.HashMap<>();
            textPart.put("text", prompt);
            parts.add(textPart);

            java.util.Map<String, Object> imagePart = new java.util.HashMap<>();
            java.util.Map<String, Object> inlineData = new java.util.HashMap<>();
            inlineData.put("mimeType", "image/jpeg");
            inlineData.put("data", base64);
            imagePart.put("inlineData", inlineData);
            parts.add(imagePart);

            contentMap.put("parts", parts);
            contents.add(contentMap);
            body.put("contents", contents);

            java.util.Map<String, Object> generationConfig = new java.util.HashMap<>();
            generationConfig.put("responseMimeType", "application/json");
            body.put("generationConfig", generationConfig);

            String apiKey = com.project.muse_android.BuildConfig.GEMINI_API_KEY;
            android.util.Log.d("MUSE_CameraSearch", "Sending request to Gemini API key: " + (apiKey != null && !apiKey.isEmpty() ? "Present" : "Empty"));
            
            com.project.muse_android.ai.GeminiClient.getClient().generateContent(apiKey, body).enqueue(new Callback<com.project.muse_android.ai.GeminiResponse>() {
                @Override
                public void onResponse(Call<com.project.muse_android.ai.GeminiResponse> call, Response<com.project.muse_android.ai.GeminiResponse> response) {
                    binding.loadingLayout.setVisibility(View.GONE);
                    binding.rvProducts.setVisibility(View.VISIBLE);

                    android.util.Log.d("MUSE_CameraSearch", "Gemini response code: " + response.code() + ", isSuccessful: " + response.isSuccessful());
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String rawText = response.body().getText().trim();
                            android.util.Log.d("MUSE_CameraSearch", "Gemini raw response text: " + rawText);
                            if (rawText.startsWith("```json")) rawText = rawText.substring(7);
                            if (rawText.endsWith("```")) rawText = rawText.substring(0, rawText.length() - 3);
                            rawText = rawText.trim();

                            org.json.JSONObject jsonObject = new org.json.JSONObject(rawText);
                            String matchedId = jsonObject.optString("matched_product_id", "").trim();
                            org.json.JSONArray similarArray = jsonObject.optJSONArray("similar_product_ids");
                            
                            boolean hasExactMatch = (matchedId != null && !matchedId.isEmpty() && !matchedId.equalsIgnoreCase("null") && !matchedId.equalsIgnoreCase("none"));
                            android.util.Log.d("MUSE_CameraSearch", "Extracted matchedId: " + matchedId + ", hasExactMatch: " + hasExactMatch);
                            
                            // Set similar products label visibility
                            binding.txtSimilarProductsLabel.setVisibility(hasExactMatch ? View.GONE : View.VISIBLE);
                            android.util.Log.d("MUSE_CameraSearch", "Setting txtSimilarProductsLabel visibility to: " + (hasExactMatch ? "GONE" : "VISIBLE"));

                            List<String> matchedIds = new ArrayList<>();
                            if (hasExactMatch) {
                                matchedIds.add(matchedId);
                            }
                            if (similarArray != null) {
                                for (int i = 0; i < similarArray.length(); i++) {
                                    matchedIds.add(similarArray.getString(i));
                                }
                            }
                            android.util.Log.d("MUSE_CameraSearch", "Parsed matchedIds: " + matchedIds.toString());

                            allSearchResults.clear();
                            List<Product> orderedResults = new ArrayList<>();
                            for (String id : matchedIds) {
                                for (Product p : allProducts) {
                                    if (p.getStatus() != null && !p.getStatus().equalsIgnoreCase("active")) continue;
                                    String pid = p.get_id() != null ? p.get_id() : p.getId();
                                    if (pid != null && pid.equalsIgnoreCase(id.trim()) && !orderedResults.contains(p)) {
                                        orderedResults.add(p);
                                        break;
                                    }
                                }
                            }
                            allSearchResults.addAll(orderedResults);
                            android.util.Log.d("MUSE_CameraSearch", "Filtered search results count: " + allSearchResults.size());

                            if (allSearchResults.isEmpty()) {
                                android.util.Log.d("MUSE_CameraSearch", "No matching products found in database. Falling back to local search.");
                                performLocalTextSearch("", allProducts, true);
                            } else {
                                applyFiltersAndSort();
                            }
                        } catch (Exception e) {
                            android.util.Log.e("MUSE_CameraSearch", "JSON parsing failed", e);
                            performLocalTextSearch("", allProducts, true);
                        }
                    } else {
                        try {
                            String err = response.errorBody() != null ? response.errorBody().string() : "Empty error";
                            android.util.Log.e("MUSE_CameraSearch", "API error: " + err);
                        } catch (Exception ignored) {}
                        performLocalTextSearch("", allProducts, true);
                    }
                }

                @Override
                public void onFailure(Call<com.project.muse_android.ai.GeminiResponse> call, Throwable t) {
                    binding.loadingLayout.setVisibility(View.GONE);
                    binding.rvProducts.setVisibility(View.VISIBLE);
                    android.util.Log.e("MUSE_CameraSearch", "Network failure calling Gemini", t);
                    performLocalTextSearch("", allProducts, true);
                }
            });
        }).start();
    }

    private String getBase64FromImagePath(String path) {
        try {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(path);
            if (bitmap == null) return "";
            android.graphics.Bitmap resized = android.graphics.Bitmap.createScaledBitmap(
                    bitmap, 600, 800, true
            );
            java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
            resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void runGeminiVoiceSearch(String query, List<Product> allProducts) {
        binding.loadingLayout.setVisibility(View.VISIBLE);
        binding.rvProducts.setVisibility(View.GONE);
        binding.txtLoadingMessage.setText("MUSE AI đang xử lý yêu cầu...");
        binding.txtProductCount.setText("Đang phân tích...");
        
        StringBuilder productsCtx = new StringBuilder();
        productsCtx.append("Danh sách sản phẩm hiện có:\n");
        for (Product p : allProducts) {
            String pid = p.get_id() != null ? p.get_id() : p.getId();
            productsCtx.append(String.format("- ID: %s | Tên: %s | Danh mục: %s\n", pid, p.getName(), p.getCategory()));
        }

        String prompt = "Người dùng tìm kiếm bằng giọng nói với từ khóa: \"" + query + "\".\n" +
                "Dựa vào danh sách sản phẩm dưới đây, hãy tìm những sản phẩm phù hợp nhất với yêu cầu tìm kiếm của người dùng (ví dụ: khớp về loại quần áo, màu sắc, phong cách).\n" +
                "Hãy trả về duy nhất một chuỗi JSON thô chứa mảng các ID sản phẩm phù hợp nhất (tối đa 10 sản phẩm). Định dạng trả về:\n" +
                "[\"id1\", \"id2\", ...]\n" +
                "Lưu ý: Chỉ trả về JSON thô, không kèm định dạng markdown ```json.\n\n" +
                productsCtx.toString();

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        List<java.util.Map<String, Object>> contents = new ArrayList<>();
        java.util.Map<String, Object> contentMap = new java.util.HashMap<>();
        List<java.util.Map<String, Object>> parts = new ArrayList<>();
        java.util.Map<String, Object> partMap = new java.util.HashMap<>();
        partMap.put("text", prompt);
        parts.add(partMap);
        contentMap.put("parts", parts);
        contents.add(contentMap);
        body.put("contents", contents);

        java.util.Map<String, Object> generationConfig = new java.util.HashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        body.put("generationConfig", generationConfig);

        String apiKey = com.project.muse_android.BuildConfig.GEMINI_API_KEY;
        com.project.muse_android.ai.GeminiClient.getClient().generateContent(apiKey, body).enqueue(new Callback<com.project.muse_android.ai.GeminiResponse>() {
            @Override
            public void onResponse(Call<com.project.muse_android.ai.GeminiResponse> call, Response<com.project.muse_android.ai.GeminiResponse> response) {
                binding.loadingLayout.setVisibility(View.GONE);
                binding.rvProducts.setVisibility(View.VISIBLE);
                
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String rawText = response.body().getText().trim();
                        if (rawText.startsWith("```json")) rawText = rawText.substring(7);
                        if (rawText.endsWith("```")) rawText = rawText.substring(0, rawText.length() - 3);
                        rawText = rawText.trim();
                        
                        org.json.JSONArray jsonArray = new org.json.JSONArray(rawText);
                        List<String> matchedIds = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            matchedIds.add(jsonArray.getString(i));
                        }

                        allSearchResults.clear();
                        for (Product p : allProducts) {
                            if (p.getStatus() != null && !p.getStatus().equalsIgnoreCase("active")) continue;
                            String pid = p.get_id() != null ? p.get_id() : p.getId();
                            if (pid != null && matchedIds.contains(pid)) {
                                allSearchResults.add(p);
                            }
                        }

                        if (allSearchResults.isEmpty()) {
                            performLocalTextSearch(query, allProducts, false);
                        } else {
                            applyFiltersAndSort();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        performLocalTextSearch(query, allProducts, false);
                    }
                } else {
                    performLocalTextSearch(query, allProducts, false);
                }
            }

            @Override
            public void onFailure(Call<com.project.muse_android.ai.GeminiResponse> call, Throwable t) {
                binding.loadingLayout.setVisibility(View.GONE);
                binding.rvProducts.setVisibility(View.VISIBLE);
                performLocalTextSearch(query, allProducts, false);
            }
        });
    }

    private void performLocalTextSearch(String query, List<Product> allProducts, boolean isCamera) {
        binding.txtSimilarProductsLabel.setVisibility(isCamera ? View.VISIBLE : View.GONE);
        allSearchResults.clear();
        String normalizedQuery = removeAccents(query).trim();
        for (Product p : allProducts) {
            if (p.getStatus() != null && !p.getStatus().equalsIgnoreCase("active")) continue;
            if (query.isEmpty() || (p.getName() != null && removeAccents(p.getName()).contains(normalizedQuery))) {
                allSearchResults.add(p);
            }
        }
        applyFiltersAndSort();
    }

    private String removeAccents(String s) {
        if (s == null) return "";
        String nfdNormalizedString = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").toLowerCase(Locale.getDefault()).replace("đ", "d").replace("Đ", "d");
    }

    private void showVoiceSearchDialog() {
        VoiceSearchDialog dialog = new VoiceSearchDialog();
        dialog.setVoiceSearchListener(result -> {
            if (result != null && !result.trim().isEmpty()) {
                binding.edtSearchQuery.setText(result);
                getIntent().putExtra("is_voice", true);
                performSearch(result);
            }
        });
        dialog.show(getSupportFragmentManager(), "VoiceSearchDialog");
    }

    private void showCameraSearchBottomSheet() {
        CameraSearchBottomSheet bottomSheet = new CameraSearchBottomSheet();
        bottomSheet.setCameraSearchListener(imagePath -> {
            if (imagePath != null && !imagePath.isEmpty()) {
                getIntent().putExtra("image_path", imagePath);
                getIntent().putExtra("is_camera", true);
                binding.edtSearchQuery.setText("");
                performSearch("");
            }
        });
        bottomSheet.show(getSupportFragmentManager(), "CameraSearchBottomSheet");
    }

    // --- Suggestion Models ---
    static class SuggestionItem {
        static final int TYPE_HEADER = 0;
        static final int TYPE_KEYWORD = 1;
        static final int TYPE_PRODUCT = 2;
        static final int TYPE_DIVIDER = 3;

        int type;
        String text;
        Product product;

        SuggestionItem(int type, String text) { this.type = type; this.text = text; }
        SuggestionItem(int type, Product product) { this.type = type; this.product = product; }
    }

    // --- Suggestion Adapter ---
    private static class SuggestionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<SuggestionItem> items;
        private final OnSuggestionClickListener listener;

        interface OnSuggestionClickListener { void onClick(SuggestionItem item); }

        SuggestionAdapter(List<SuggestionItem> items, OnSuggestionClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).type;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == SuggestionItem.TYPE_HEADER) {
                TextView tv = new TextView(parent.getContext());
                tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                tv.setPadding(40, 32, 40, 16);
                tv.setTextColor(Color.parseColor("#999999"));
                tv.setTextSize(12);
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                return new HeaderViewHolder(tv);
            } else if (viewType == SuggestionItem.TYPE_KEYWORD) {
                TextView tv = new TextView(parent.getContext());
                tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                tv.setPadding(40, 24, 40, 24);
                tv.setTextColor(Color.parseColor("#1A73E8"));
                tv.setTextSize(14);
                tv.setBackgroundResource(android.R.drawable.list_selector_background);
                return new KeywordViewHolder(tv);
            } else if (viewType == SuggestionItem.TYPE_DIVIDER) {
                View v = new View(parent.getContext());
                v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                v.setBackgroundColor(Color.parseColor("#EEEEEE"));
                return new DividerViewHolder(v);
            } else {
                LinearLayout layout = new LinearLayout(parent.getContext());
                layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout.setOrientation(LinearLayout.HORIZONTAL);
                layout.setPadding(40, 24, 40, 24);
                layout.setBackgroundResource(android.R.drawable.list_selector_background);
                layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

                ImageView img = new ImageView(parent.getContext());
                img.setLayoutParams(new LinearLayout.LayoutParams(140, 140));
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);

                LinearLayout textLayout = new LinearLayout(parent.getContext());
                textLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                lp.setMarginStart(32);
                textLayout.setLayoutParams(lp);

                TextView name = new TextView(parent.getContext());
                name.setTextColor(Color.BLACK);
                name.setTextSize(14);
                name.setMaxLines(1);
                name.setEllipsize(android.text.TextUtils.TruncateAt.END);

                LinearLayout priceRow = new LinearLayout(parent.getContext());
                priceRow.setOrientation(LinearLayout.HORIZONTAL);
                priceRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

                TextView price = new TextView(parent.getContext());
                price.setTextColor(Color.RED);
                price.setTextSize(13);
                price.setTypeface(null, android.graphics.Typeface.BOLD);

                TextView oldPrice = new TextView(parent.getContext());
                oldPrice.setTextColor(Color.GRAY);
                oldPrice.setTextSize(11);
                oldPrice.setPaintFlags(oldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

                TextView discount = new TextView(parent.getContext());
                discount.setTextColor(Color.parseColor("#FF5722"));
                discount.setTextSize(11);

                LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mlp.setMarginStart(12);
                oldPrice.setLayoutParams(mlp);
                discount.setLayoutParams(mlp);

                priceRow.addView(price);
                priceRow.addView(oldPrice);
                priceRow.addView(discount);

                textLayout.addView(name);
                textLayout.addView(priceRow);

                layout.addView(img);
                layout.addView(textLayout);
                return new ProductViewHolder(layout, img, name, price, oldPrice, discount);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            SuggestionItem item = items.get(position);
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).tv.setText(item.text);
            } else if (holder instanceof KeywordViewHolder) {
                ((KeywordViewHolder) holder).tv.setText(item.text);
                holder.itemView.setOnClickListener(v -> listener.onClick(item));
            } else if (holder instanceof ProductViewHolder) {
                Product p = item.product;
                ProductViewHolder vh = (ProductViewHolder) holder;
                vh.name.setText(p.getName());

                Double dPrice = p.getDiscountPrice();
                if (dPrice != null && dPrice > 0) {
                    vh.price.setText(formatPrice(dPrice));
                    vh.oldPrice.setText(formatPrice(p.getPrice()));
                    vh.oldPrice.setVisibility(View.VISIBLE);
                    int pct = (int) ((1 - (dPrice / p.getPrice())) * 100);
                    vh.discount.setText("-" + pct + "%");
                    vh.discount.setVisibility(View.VISIBLE);
                } else {
                    vh.price.setText(formatPrice(p.getPrice()));
                    vh.oldPrice.setVisibility(View.GONE);
                    vh.discount.setVisibility(View.GONE);
                }

                String imgUrl = (p.getImages() != null && !p.getImages().isEmpty()) ? p.getImages().get(0).getUrl() : null;
                if (imgUrl != null) {
                    if (!imgUrl.startsWith("http")) imgUrl = "https://server-testing-ymn9.onrender.com" + (imgUrl.startsWith("/") ? "" : "/") + imgUrl;
                    Glide.with(vh.img.getContext()).load(imgUrl).placeholder(android.R.drawable.ic_menu_gallery).into(vh.img);
                }
                holder.itemView.setOnClickListener(v -> listener.onClick(item));
            }
        }

        private String formatPrice(double price) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
            symbols.setGroupingSeparator('.');
            DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
            return decimalFormat.format(price) + "đ";
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView tv;
            HeaderViewHolder(View itemView) { super(itemView); tv = (TextView) itemView; }
        }
        static class KeywordViewHolder extends RecyclerView.ViewHolder {
            TextView tv;
            KeywordViewHolder(View itemView) { super(itemView); tv = (TextView) itemView; }
        }
        static class DividerViewHolder extends RecyclerView.ViewHolder {
            DividerViewHolder(View itemView) { super(itemView); }
        }
        static class ProductViewHolder extends RecyclerView.ViewHolder {
            ImageView img; TextView name, price, oldPrice, discount;
            ProductViewHolder(View v, ImageView img, TextView name, TextView price, TextView oldPrice, TextView discount) {
                super(v); this.img = img; this.name = name; this.price = price; this.oldPrice = oldPrice; this.discount = discount;
            }
        }
    }
}
