package com.project.muse_android.order;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.adapters.OrderAdapter;
import com.project.adapters.ProductAdapter;
import com.project.models.Order;
import com.project.models.Product;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityOrderBinding;
import com.project.network.ApiClient;
import com.project.network.ApiResponse;
import com.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderActivity extends AppCompatActivity {

    private ActivityOrderBinding binding;
    private OrderAdapter orderAdapter;
    private List<Order> allOrders = new ArrayList<>();
    private List<Order> filteredOrders = new ArrayList<>();

    private ProductAdapter suggestionAdapter;
    private List<Product> suggestionList = new ArrayList<>();

    private String currentStatus = "ALL";
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupOrderRecyclerView();
        setupTabs();
        setupSuggestionRecyclerView();
        
        fetchOrders();
        loadSuggestions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchOrders();
    }

    private void setupOrderRecyclerView() {
        orderAdapter = new OrderAdapter(this, filteredOrders);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(orderAdapter);
    }

    private void setupTabs() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSearch.setOnClickListener(v -> Toast.makeText(this, "Tìm kiếm", Toast.LENGTH_SHORT).show());

        binding.tabAll.setOnClickListener(v -> filterOrders("ALL"));
        binding.tabPending.setOnClickListener(v -> filterOrders("Đang xử lý"));
        binding.tabProcessing.setOnClickListener(v -> filterOrders("Đã xác nhận"));
        binding.tabShipping.setOnClickListener(v -> filterOrders("Đang giao"));
        binding.tabDelivered.setOnClickListener(v -> filterOrders("Đã giao"));
        binding.tabReturned.setOnClickListener(v -> filterOrders("Trả hàng"));
        binding.tabCancelled.setOnClickListener(v -> filterOrders("Đã hủy"));
        
        // Default select All
        filterOrders("ALL");
    }

    private void filterOrders(String status) {
        currentStatus = status;
        
        // Update UI Tabs
        resetTabStyles();
        if (status.equals("ALL")) setTabSelected(binding.tabAll);
        else if (status.equalsIgnoreCase("Đang xử lý") || status.equalsIgnoreCase("PENDING")) setTabSelected(binding.tabPending);
        else if (status.equalsIgnoreCase("Đã xác nhận") || status.equalsIgnoreCase("PROCESSING")) setTabSelected(binding.tabProcessing);
        else if (status.equalsIgnoreCase("Đang giao") || status.equalsIgnoreCase("SHIPPING")) setTabSelected(binding.tabShipping);
        else if (status.equalsIgnoreCase("Đã giao") || status.equalsIgnoreCase("DELIVERED") || status.equalsIgnoreCase("COMPLETED")) setTabSelected(binding.tabDelivered);
        else if (status.equalsIgnoreCase("Trả hàng") || status.equalsIgnoreCase("RETURNED")) setTabSelected(binding.tabReturned);
        else if (status.equalsIgnoreCase("Đã hủy") || status.equalsIgnoreCase("CANCELLED")) setTabSelected(binding.tabCancelled);

        // Filter Logic
        filteredOrders.clear();
        if (status.equals("ALL")) {
            filteredOrders.addAll(allOrders);
        } else {
            for (Order o : allOrders) {
                String orderStatus = o.getStatus();
                if (status.equalsIgnoreCase(orderStatus)) {
                    filteredOrders.add(o);
                } else {
                    // Map groups
                    if (status.equalsIgnoreCase("Đang xử lý") && orderStatus.equalsIgnoreCase("PENDING")) filteredOrders.add(o);
                    else if (status.equalsIgnoreCase("Đã xác nhận") && orderStatus.equalsIgnoreCase("PROCESSING")) filteredOrders.add(o);
                    else if (status.equalsIgnoreCase("Đang giao") && orderStatus.equalsIgnoreCase("SHIPPING")) filteredOrders.add(o);
                    else if (status.equalsIgnoreCase("Đã giao") && (orderStatus.equalsIgnoreCase("DELIVERED") || orderStatus.equalsIgnoreCase("COMPLETED"))) filteredOrders.add(o);
                    else if (status.equalsIgnoreCase("Trả hàng") && orderStatus.equalsIgnoreCase("RETURNED")) filteredOrders.add(o);
                    else if (status.equalsIgnoreCase("Đã hủy") && orderStatus.equalsIgnoreCase("CANCELLED")) filteredOrders.add(o);
                }
            }
        }
        orderAdapter.notifyDataSetChanged();
    }

    private void resetTabStyles() {
        TextView[] tabs = {binding.tabAll, binding.tabPending, binding.tabProcessing, binding.tabShipping, binding.tabDelivered, binding.tabReturned, binding.tabCancelled};
        for (TextView t : tabs) {
            t.setBackground(null);
            t.setTextColor(android.graphics.Color.BLACK);
            t.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void setTabSelected(TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_order_tab_selected);
        tab.setTextColor(android.graphics.Color.WHITE);
        tab.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void setupSuggestionRecyclerView() {
        suggestionAdapter = new ProductAdapter(suggestionList, ProductAdapter.TYPE_VERTICAL, product -> {
            android.content.Intent intent = new android.content.Intent(this, com.project.muse_android.product.ProductDetailActivity.class);
            intent.putExtra("product_id", product.getId());
            startActivity(intent);
        });
        binding.rvOrderSuggestions.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvOrderSuggestions.setAdapter(suggestionAdapter);
    }

    private void fetchOrders() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem đơn hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.rvOrders.setVisibility(View.GONE);
        
        ApiClient.INSTANCE.getInstance().getMyOrders(userId).enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(@NonNull Call<List<Order>> call, @NonNull Response<List<Order>> response) {
                binding.rvOrders.setVisibility(View.VISIBLE);
                android.util.Log.d("OrderActivity", "Response code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    allOrders.clear();
                    allOrders.addAll(response.body());
                    android.util.Log.d("OrderActivity", "Orders count: " + allOrders.size());
                    filterOrders(currentStatus);
                } else {
                    String errorMsg = "Không thể lấy danh sách đơn hàng";
                    if (response.errorBody() != null) {
                        try {
                            String errorStr = response.errorBody().string();
                            android.util.Log.e("OrderActivity", "Error body: " + errorStr);
                            errorMsg += ": " + errorStr;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(OrderActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Order>> call, @NonNull Throwable t) {
                binding.rvOrders.setVisibility(View.VISIBLE);
                android.util.Log.e("OrderActivity", "Fetch orders failed", t);
                Toast.makeText(OrderActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSuggestions() {
        ApiClient.INSTANCE.getInstance().getProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    suggestionList.clear();
                    List<Product> products = response.body();
                    if (products.size() > 6) suggestionList.addAll(products.subList(0, 6));
                    else suggestionList.addAll(products);
                    suggestionAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {}
        });
    }
}
