package com.project.muse_android.order;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.project.network.ApiClient;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.adapters.HorizontalProductAdapter;
import com.project.models.Order;
import com.project.models.enums.HorizontalProductMode;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityOrderDetailBinding;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {

    private ActivityOrderDetailBinding binding;
    private Order order;
    private HorizontalProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get Order from intent
        order = (Order) getIntent().getSerializableExtra("order");
        if (order == null) {
            Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        populateData();
        if (order.get_id() != null) {
            fetchOrderDetail(order.get_id());
        }
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        adapter = new HorizontalProductAdapter(this, HorizontalProductMode.READ_ONLY, null);
        binding.rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrderItems.setAdapter(adapter);

        // Action Buttons
        binding.btnCancelOrder.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này không?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    cancelOrderOnServer();
                })
                .setNegativeButton("Không", null)
                .show();
        });

        binding.btnContact.setOnClickListener(v -> {
            Toast.makeText(this, "Đang kết nối với Muse để hỗ trợ cho đơn hàng " + (order != null ? order.getId() : ""), Toast.LENGTH_SHORT).show();
        });

        binding.btnReview.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng đánh giá đơn hàng " + (order != null ? order.getId() : "") + " đang được phát triển", Toast.LENGTH_SHORT).show();
        });

        binding.btnRequestRefund.setOnClickListener(v -> {
            Toast.makeText(this, "Yêu cầu trả hàng cho đơn " + (order != null ? order.getId() : "") + " đã được ghi nhận", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnChat.setOnClickListener(v -> {
            Toast.makeText(this, "Mở Chat với Muse", Toast.LENGTH_SHORT).show();
        });
        
        binding.btnHelp.setOnClickListener(v -> {
            Toast.makeText(this, "Mở Trung tâm hỗ trợ", Toast.LENGTH_SHORT).show();
        });
    }

    private void populateData() {
        // 1. Status Banner
        updateStatusUI(order.getStatus());

        // 2. Shipping Info
        if (order.getShippingMethod() != null) {
            binding.txtShippingCode.setText("Phương thức: " + order.getShippingMethod().getName());
        } else {
            binding.txtShippingCode.setText("Mã vận đơn: ORD-" + order.getId().substring(order.getId().length() - 8).toUpperCase());
        }

        // 3. Address Section
        binding.txtCustomerName.setText(order.getCustomerName());
        binding.txtCustomerPhone.setText(order.getPhone());
        binding.txtCustomerAddress.setText("Địa chỉ: " + order.getAddress());

        // 4. Order Items
        adapter.setData(order.getProducts());

        // 5. Price Summary
        double subtotal = order.getSubTotal();
        double shippingFee = order.getShippingMethod() != null ? order.getShippingMethod().getFee() : 0;
        double discount = order.getDiscount();
        double finalPrice = order.getFinalPrice();

        binding.txtTotalAmount.setText(formatPrice(subtotal));
        binding.txtShippingFee.setText(formatPrice(shippingFee));
        binding.txtShippingDiscount.setText("-" + formatPrice(0)); // Placeholder if no shipping discount
        binding.txtVoucherDiscount.setText("-" + formatPrice(discount));
        binding.txtFinalPrice.setText(formatPrice(finalPrice));

        // 6. Order Info
        binding.txtOrderCode.setText(order.getId().toUpperCase());
        binding.txtPaymentMethod.setText(order.getPaymentMethod() != null ? order.getPaymentMethod() : "Thanh toán khi nhận hàng");
        
        binding.txtOrderTime.setText(formatDate(order.getCreatedAt()));
        binding.txtPaymentTime.setText(formatDate(order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt()));

        // Conditional fields based on status
        if ("SHIPPING".equalsIgnoreCase(order.getStatus())) {
            binding.layoutPickupTime.setVisibility(View.VISIBLE);
            binding.txtPickupTime.setText(formatDate(order.getUpdatedAt()));
        }
        
        if ("DELIVERED".equalsIgnoreCase(order.getStatus()) || "COMPLETED".equalsIgnoreCase(order.getStatus())) {
            binding.layoutPickupTime.setVisibility(View.VISIBLE);
            binding.layoutCompletionTime.setVisibility(View.VISIBLE);
            binding.txtCompletionTime.setText(formatDate(order.getUpdatedAt()));
        }
    }

    private void updateStatusUI(String status) {
        if (status == null) status = "PENDING";
        
        // Hide both action layouts first
        binding.layoutPendingActions.setVisibility(View.GONE);
        binding.layoutCompletedActions.setVisibility(View.GONE);
        binding.btnReturnRefund.setVisibility(View.GONE);
        binding.lineReturn.setVisibility(View.GONE);

        switch (status.toUpperCase()) {
            case "PENDING":
                binding.txtStatusBanner.setText("Chờ xác nhận");
                binding.txtStatusBanner.setBackgroundColor(getColor(R.color.neutral_800));
                binding.layoutPendingActions.setVisibility(View.VISIBLE);
                break;
            case "PROCESSING":
                binding.txtStatusBanner.setText("Chờ lấy hàng");
                binding.txtStatusBanner.setBackgroundColor(getColor(R.color.neutral_800));
                binding.layoutPendingActions.setVisibility(View.VISIBLE);
                break;
            case "SHIPPING":
                binding.txtStatusBanner.setText("Đang giao hàng");
                binding.txtStatusBanner.setBackgroundColor(getColor(R.color.neutral_800));
                break;
            case "DELIVERED":
            case "COMPLETED":
                binding.txtStatusBanner.setText("Đơn hàng đã hoàn thành");
                binding.txtStatusBanner.setBackgroundColor(0xFF008B86); // Teal color from CSS
                binding.layoutCompletedActions.setVisibility(View.VISIBLE);
                binding.btnReturnRefund.setVisibility(View.VISIBLE);
                binding.lineReturn.setVisibility(View.VISIBLE);
                break;
            case "CANCELLED":
                binding.txtStatusBanner.setText("Đã hủy");
                binding.txtStatusBanner.setBackgroundColor(getColor(R.color.neutral_600));
                break;
            default:
                binding.txtStatusBanner.setText(status);
                binding.txtStatusBanner.setBackgroundColor(getColor(R.color.neutral_800));
                break;
        }
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + "đ";
    }

    private String formatDate(String dateStr) {
        if (dateStr == null) return "N/A";
        try {
            // Assuming ISO date from server
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private void fetchOrderDetail(String orderId) {
        ApiClient.INSTANCE.getInstance().getOrderDetail(orderId).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    order = response.body();
                    populateData();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                // Fallback to local passed intent order
            }
        });
    }

    private void cancelOrderOnServer() {
        if (order == null || order.get_id() == null) return;

        java.util.Map<String, String> statusBody = new java.util.HashMap<>();
        statusBody.put("status", "Đã hủy");

        ApiClient.INSTANCE.getInstance().updateOrderStatus(order.get_id(), statusBody).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    order = response.body();
                    populateData();
                    Toast.makeText(OrderDetailActivity.this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(OrderDetailActivity.this, "Không thể hủy đơn hàng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                Toast.makeText(OrderDetailActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
