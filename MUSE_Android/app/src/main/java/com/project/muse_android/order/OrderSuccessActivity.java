package com.project.muse_android.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.project.adapters.ProductAdapter;
import com.project.models.Product;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityOrderSuccessBinding;
import com.project.muse_android.main.MainActivity;
import com.project.network.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderSuccessActivity extends AppCompatActivity {

    private ActivityOrderSuccessBinding binding;
    private ProductAdapter suggestionAdapter;
    private List<Product> suggestionList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityOrderSuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupUI();
        loadSuggestions();
    }

    private void setupUI() {
        binding.btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            // Bỏ các flag xóa stack để có thể quay lại bằng nút Back
            startActivity(intent);
        });

        binding.btnOrders.setOnClickListener(v -> {
            // Navigate to Order History
            Intent intent = new Intent(this, com.project.muse_android.order.OrderActivity.class);
            startActivity(intent);
            finish();
        });

        // Setup Suggestions Grid
        suggestionAdapter = new ProductAdapter(suggestionList, ProductAdapter.TYPE_VERTICAL, product -> {
            // Navigate to Product Detail
            Intent intent = new Intent(this, com.project.muse_android.product.ProductDetailActivity.class);
            intent.putExtra("product_id", product.getId());
            startActivity(intent);
        });

        binding.rvSuggestions.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvSuggestions.setAdapter(suggestionAdapter);
    }

    private void loadSuggestions() {
        ApiClient.INSTANCE.getInstance().getProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    suggestionList.clear();
                    // Take first 10 products as suggestions
                    List<Product> body = response.body();
                    if (body.size() > 10) {
                        suggestionList.addAll(body.subList(0, 10));
                    } else {
                        suggestionList.addAll(body);
                    }
                    suggestionAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                Toast.makeText(OrderSuccessActivity.this, "Lỗi tải gợi ý", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
