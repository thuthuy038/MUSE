package com.project.muse_android.product;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.adapters.ProductReviewAdapter;
import com.project.models.ReviewResponse;
import com.project.muse_android.databinding.ActivityProductReviewsBinding;
import com.project.network.ApiService;
import com.project.network.HomeApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductReviewsActivity extends AppCompatActivity {

    private ActivityProductReviewsBinding binding;
    private ProductReviewAdapter adapter;
    private String productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        productId = getIntent().getStringExtra("product_id");

        binding.btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        setupSearch();

        if (productId != null) {
            loadReviews();
        }
    }

    private void setupRecyclerView() {
        adapter = new ProductReviewAdapter(new ArrayList<>());
        adapter.setOnImageClickListener((images, position) -> {
            Intent intent = new Intent(this, FullScreenImageActivity.class);
            intent.putStringArrayListExtra("images", new ArrayList<>(images));
            intent.putExtra("position", position);
            startActivity(intent);
        });

        binding.rvAllReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAllReviews.setAdapter(adapter);

        adapter.registerAdapterDataObserver(new androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (adapter.getItemCount() == 0) {
                    binding.txtNoReviews.setVisibility(View.VISIBLE);
                } else {
                    binding.txtNoReviews.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupSearch() {
        binding.edtSearchReview.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadReviews() {
        ApiService service = HomeApiClient.getApiService();
        service.getProductReviews(productId).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(Call<ReviewResponse> call, Response<ReviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.updateData(response.body().getData());
                } else {
                    Toast.makeText(ProductReviewsActivity.this, "Không thể tải đánh giá", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ReviewResponse> call, Throwable t) {
                Toast.makeText(ProductReviewsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
