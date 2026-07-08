package com.project.muse_android.search;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.project.adapters.SearchHistoryAdapter;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivitySearchBinding;
import com.project.muse_android.main.MainActivity;
import com.project.network.ApiResponse;
import com.project.network.HomeApiClient;
import com.project.network.HomeApiService;
import com.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

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
        loadPopularSearches();
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

    private void performSearch() {
        String query = binding.edtSearch.getText().toString().trim();
        if (!query.isEmpty()) {
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
}
