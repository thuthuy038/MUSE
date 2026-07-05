package com.project.muse_android.cart;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.project.adapters.VerticalProductAdapter;
import com.project.models.Product;
import com.project.network.ApiClient;
import com.project.muse_android.databinding.FragmentCartBinding;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartFragment extends Fragment {

    private static final String TAG = "CartFragment_API";
    private FragmentCartBinding binding;
    private VerticalProductAdapter productAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Nút quay lại
        binding.ivBack.setOnClickListener(v -> {
            Navigation.findNavController(view).navigateUp();
        });

        // Nút Mua sắm ngay (khi giỏ hàng trống)
        binding.btnShopNow.setOnClickListener(v -> {
            Navigation.findNavController(view).popBackStack();
        });

        // 1. Cấu hình RecyclerView và Adapter cho phần gợi ý sản phẩm
        setupSuggestedProductsRecyclerView();

        // 2. Gọi API lấy dữ liệu sản phẩm từ Server
        fetchProductsFromServer();
    }

    private void setupSuggestedProductsRecyclerView() {
        // Thiết lập Grid hiển thị dạng ô lưới 2 cột
        binding.rvSuggestedProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Khởi tạo Adapter với Listener xử lý sự kiện click
        productAdapter = new VerticalProductAdapter(getContext(), new VerticalProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                // Xử lý khi click vào sản phẩm (Chuyển đến màn hình chi tiết)
                Toast.makeText(getContext(), "Chi tiết: " + product.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFavoriteClick(Product product, int position) {
                // Xử lý khi nhấn nút yêu thích
                Toast.makeText(getContext(), "Đã thêm vào yêu thích: " + product.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        // Gắn adapter vào RecyclerView
        binding.rvSuggestedProducts.setAdapter(productAdapter);
    }

    private void fetchProductsFromServer() {
        // Thông báo đang tải (Vì Render.com hay bị ngủ đông, có thể mất 30s để phản hồi lần đầu)
        if (isAdded()) {
            Toast.makeText(getContext(), "Đang tải sản phẩm gợi ý...", Toast.LENGTH_SHORT).show();
        }

        // Sử dụng ApiClient để gọi lấy danh sách sản phẩm
        ApiClient.INSTANCE.getInstance().getProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Product> products = response.body();
                    Log.d(TAG, "Tải sản phẩm thành công: " + products.size());
                    
                    if (products.isEmpty()) {
                        Log.w(TAG, "Danh sách sản phẩm trống!");
                    }

                    // Nạp dữ liệu vào adapter
                    productAdapter.setData(products);
                } else {
                    Log.e(TAG, "Lỗi phản hồi server: " + response.code());
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Lỗi server (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                Log.e(TAG, "Lỗi Retrofit", t);
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi kết nối máy chủ! " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
