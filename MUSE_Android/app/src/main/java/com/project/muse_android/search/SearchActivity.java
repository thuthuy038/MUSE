package com.project.muse_android.search;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.project.adapters.SearchHistoryAdapter;
import com.project.models.Product;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivitySearchBinding;
import com.project.muse_android.main.MainActivity;
import com.project.network.ApiResponse;
import com.project.network.HomeApiClient;
import com.project.network.HomeApiService;
import com.project.utils.SessionManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    private SearchHistoryManager historyManager;
    private SearchHistoryAdapter historyAdapter;
    private List<String> historyList;
    private HomeApiService apiService;
    private SessionManager sessionManager;

    private SuggestionAdapter suggestionAdapter;
    private List<SuggestionItem> suggestionList = new ArrayList<>();
    private List<com.project.models.Category> allCategories = new ArrayList<>();

    private final ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    boolean shouldClear = result.getData().getBooleanExtra("clear_search", false);
                    if (shouldClear) {
                        binding.edtSearch.setText("");
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        historyManager = new SearchHistoryManager(this);
        apiService = HomeApiClient.getHomeApiService();
        sessionManager = new SessionManager(this);
        historyList = new ArrayList<>();

        setupUI();
        setupSuggestionRecyclerView();
        loadPopularSearches();
        loadAllCategories();
    }

    private void loadAllCategories() {
        apiService.getCategories().enqueue(new Callback<List<com.project.models.Category>>() {
            @Override
            public void onResponse(Call<List<com.project.models.Category>> call, Response<List<com.project.models.Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allCategories.clear();
                    allCategories.addAll(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<com.project.models.Category>> call, Throwable t) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> {
            // Khi nhấn back, nếu ô tìm kiếm trống thì trả kết quả về để trang trước xóa text
            String query = binding.edtSearch.getText().toString().trim();
            if (query.isEmpty()) {
                android.content.Intent resultIntent = new android.content.Intent();
                resultIntent.putExtra("clear_search", true);
                setResult(RESULT_OK, resultIntent);
            }
            finish();
        });

        binding.imgCart.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
            intent.putExtra("open_cart", true);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        binding.txtSearchAction.setOnClickListener(v -> performSearch());

        binding.edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        binding.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (!query.isEmpty()) {
                    fetchSuggestions(query);
                } else {
                    binding.rvSuggestions.setVisibility(View.GONE);
                    suggestionList.clear();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnClearHistory.setOnClickListener(v -> {
            historyManager.clearHistory();
            historyList.clear();
            historyAdapter.notifyDataSetChanged();
            updateHistoryVisibility();
        });

        binding.btnViewCategories.setOnClickListener(v -> {
            // Chuyển sang tab Explore (Khám phá) thông qua MainActivity
            android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
            intent.putExtra("open_explore", true);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        binding.imgVoiceSearch.setOnClickListener(v -> Toast.makeText(this, "Đang nghe giọng nói...", Toast.LENGTH_SHORT).show());

        binding.imgCameraSearch.setOnClickListener(v -> Toast.makeText(this, "Mở Camera tìm kiếm", Toast.LENGTH_SHORT).show());

        // Auto focus and show keyboard
        binding.edtSearch.requestFocus();
    }

    private void setupSuggestionRecyclerView() {
        suggestionAdapter = new SuggestionAdapter(suggestionList, item -> {
            if (item.type == SuggestionItem.TYPE_KEYWORD) {
                binding.edtSearch.setText(item.text);
                performSearch();
            } else if (item.type == SuggestionItem.TYPE_PRODUCT && item.product != null) {
                // Navigate to product detail
                android.content.Intent intent = new android.content.Intent(this, com.project.muse_android.product.ProductDetailActivity.class);
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
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Log.e("SearchActivity", "Fetch suggestions failed", t);
            }
        });
    }

    private void buildSuggestionList(List<Product> products) {
        suggestionList.clear();
        String currentQuery = binding.edtSearch.getText().toString().trim();
        String normalizedQuery = removeAccents(currentQuery).toLowerCase();

        // 1. "Có phải bạn muốn tìm" Section (Categories matching query)
        List<com.project.models.Category> matchingCategories = new ArrayList<>();
        for (com.project.models.Category cat : allCategories) {
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

    private void performSearch() {
        String query = binding.edtSearch.getText().toString().trim();
        if (!query.isEmpty()) {
            binding.rvSuggestions.setVisibility(View.GONE);
            historyManager.addHistory(query);
            String userId = sessionManager.getUserId();
            String currentUserId = (userId != null) ? userId : "guest";
          // Record search query on server for popular searches logic
          Log.d("SearchActivity", "Recording search: " + query + " for user: " + currentUserId);
          apiService.recordSearch(query, currentUserId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
              if (response.isSuccessful()) {
                Log.d("SearchActivity", "Search recorded successfully");
              } else {
                Log.e("SearchActivity", "Failed to record search: " + response.code() + " " + response.message());
              }
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
              Log.e("SearchActivity", "Error recording search", t);
            }
            });

            android.content.Intent intent = new android.content.Intent(this, SearchResultActivity.class);
            intent.putExtra("query", query);
            resultLauncher.launch(intent);
        } else {
            Toast.makeText(this, "Vui lòng nhập nội dung tìm kiếm", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPopularSearches() {
        apiService.getPopularSearches().enqueue(new Callback<ApiResponse<List<String>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<String>>> call, Response<ApiResponse<List<String>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null && !response.body().getData().isEmpty()) {
                    displayPopularSearches(response.body().getData());
                } else {
                    binding.txtNoPopular.setVisibility(View.VISIBLE);
                    binding.chipGroupPopular.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<String>>> call, Throwable t) {
                Log.e("SearchActivity", "Load popular failed", t);
                binding.txtNoPopular.setVisibility(View.VISIBLE);
                binding.txtNoPopular.setText("Không có tìm kiếm phổ biến nào");
            }
        });
    }

    private void displayPopularSearches(List<String> searches) {
        binding.chipGroupPopular.removeAllViews();
        binding.txtNoPopular.setVisibility(View.GONE);
        binding.chipGroupPopular.setVisibility(View.VISIBLE);

        // Limit to top 5 popular searches
        List<String> displayList = searches.size() > 5 ? searches.subList(0, 5) : searches;

        for (String search : displayList) {
            Chip chip = new Chip(this);
            chip.setText(search);
            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setChipStrokeColorResource(R.color.primary_500);
            chip.setChipStrokeWidth(2f);
            chip.setTextColor(ResourcesCompat.getColor(getResources(), R.color.primary_500, getTheme()));
            chip.setChipIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_search, getTheme()));
            chip.setChipIconTintResource(R.color.primary_500);

            chip.setOnClickListener(v -> {
                binding.edtSearch.setText(search);
                performSearch();
            });

            binding.chipGroupPopular.addView(chip);
        }
    }

    private void loadHistory() {
        historyList.clear();
        historyList.addAll(historyManager.getHistory());
        
        if (historyAdapter == null) {
            historyAdapter = new SearchHistoryAdapter(historyList, new SearchHistoryAdapter.OnHistoryClickListener() {
                @Override
                public void onHistoryItemClick(String query) {
                    binding.edtSearch.setText(query);
                    performSearch();
                }

                @Override
                public void onRemoveItemClick(String query, int position) {
                    historyManager.removeHistory(query);
                    historyList.remove(position);
                    historyAdapter.notifyItemRemoved(position);
                    updateHistoryVisibility();
                }
            });

            binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
            binding.rvHistory.setAdapter(historyAdapter);
        } else {
            historyAdapter.notifyDataSetChanged();
        }
        updateHistoryVisibility();
    }

    private void updateHistoryVisibility() {
        if (historyList.isEmpty()) {
            binding.txtNoHistory.setVisibility(View.VISIBLE);
            binding.rvHistory.setVisibility(View.GONE);
            binding.btnClearHistory.setVisibility(View.GONE);
        } else {
            binding.txtNoHistory.setVisibility(View.GONE);
            binding.rvHistory.setVisibility(View.VISIBLE);
            binding.btnClearHistory.setVisibility(View.VISIBLE);
        }
    }

    private String removeAccents(String s) {
        if (s == null) return "";
        String nfdNormalizedString = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").toLowerCase(Locale.getDefault()).replace("đ", "d").replace("Đ", "d");
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
                tv.setTextColor(Color.parseColor("#1A73E8")); // Blue like screenshot
                tv.setTextSize(14);
                tv.setBackgroundResource(android.R.drawable.list_selector_background);
                return new KeywordViewHolder(tv);
            } else if (viewType == SuggestionItem.TYPE_DIVIDER) {
                View v = new View(parent.getContext());
                v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                v.setBackgroundColor(Color.parseColor("#EEEEEE"));
                return new DividerViewHolder(v);
            } else {
                // Product View
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
