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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupOrderRecyclerView();
        setupTabs();
        setupSuggestionRecyclerView();
        
        loadMockOrders();
        loadSuggestions();
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
        binding.tabPending.setOnClickListener(v -> filterOrders("PENDING"));
        binding.tabProcessing.setOnClickListener(v -> filterOrders("PROCESSING"));
        binding.tabShipping.setOnClickListener(v -> filterOrders("SHIPPING"));
        binding.tabDelivered.setOnClickListener(v -> filterOrders("DELIVERED"));
        binding.tabReturned.setOnClickListener(v -> filterOrders("RETURNED"));
        binding.tabCancelled.setOnClickListener(v -> filterOrders("CANCELLED"));
        
        // Default select All
        filterOrders("ALL");
    }

    private void filterOrders(String status) {
        currentStatus = status;
        
        // Update UI Tabs
        resetTabStyles();
        if (status.equals("ALL")) setTabSelected(binding.tabAll);
        else if (status.equals("PENDING")) setTabSelected(binding.tabPending);
        else if (status.equals("PROCESSING")) setTabSelected(binding.tabProcessing);
        else if (status.equals("SHIPPING")) setTabSelected(binding.tabShipping);
        else if (status.equals("DELIVERED")) setTabSelected(binding.tabDelivered);
        else if (status.equals("RETURNED")) setTabSelected(binding.tabReturned);
        else if (status.equals("CANCELLED")) setTabSelected(binding.tabCancelled);

        // Filter Logic
        filteredOrders.clear();
        if (status.equals("ALL")) {
            filteredOrders.addAll(allOrders);
        } else {
            for (Order o : allOrders) {
                if (status.equalsIgnoreCase(o.getStatus())) {
                    filteredOrders.add(o);
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

    private void loadMockOrders() {
        // Product for mock
        Product p1 = new Product();
        p1.setName("Đầm babydoll xếp ly đính nơ lụa ngọt ngào Abeline");
        p1.setPrice(790000);
        p1.setDiscountPrice(690000.0);
        p1.setQuantity(1);
        List<com.project.models.ProductVariant> v1 = new ArrayList<>();
        com.project.models.ProductVariant var1 = new com.project.models.ProductVariant();
        var1.setColor("Hồng");
        var1.setSize("S");
        v1.add(var1);
        p1.setVariants(v1);

        allOrders.add(new Order("1", "PENDING", Arrays.asList(p1), 700000));
        allOrders.add(new Order("2", "SHIPPING", Arrays.asList(p1), 700000));
        allOrders.add(new Order("3", "PROCESSING", Arrays.asList(p1, p1), 1390000));
        allOrders.add(new Order("4", "DELIVERED", Arrays.asList(p1), 700000));
        allOrders.add(new Order("5", "CANCELLED", Arrays.asList(p1), 700000));
        
        filterOrders(currentStatus);
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
