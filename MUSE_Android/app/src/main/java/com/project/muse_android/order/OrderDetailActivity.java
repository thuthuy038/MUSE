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
import com.project.adapters.ProductAdapter;
import com.project.models.Order;
import com.project.models.Product;
import com.project.models.enums.HorizontalProductMode;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityOrderDetailBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.recyclerview.widget.GridLayoutManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import java.util.List;

public class OrderDetailActivity extends AppCompatActivity {

    private ActivityOrderDetailBinding binding;
    private Order order;
    private HorizontalProductAdapter adapter;
    private ProductAdapter suggestionAdapter;
    private List<Product> suggestionList = new ArrayList<>();

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
        setupSuggestionRecyclerView();
        populateData();
        if (order.get_id() != null) {
            fetchOrderDetail(order.get_id());
        }
        loadSuggestions();
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

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        adapter = new HorizontalProductAdapter(this, HorizontalProductMode.READ_ONLY, null);
        binding.rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrderItems.setAdapter(adapter);

        // Action Buttons
        binding.btnCancelOrder.setOnClickListener(v -> {
            showCancelOrderBottomSheet();
        });

        binding.btnContact.setOnClickListener(v -> {
            Toast.makeText(this, "Đang kết nối với Muse để hỗ trợ cho đơn hàng " + (order != null ? order.getId() : ""), Toast.LENGTH_SHORT).show();
        });

        binding.btnContactShipping.setOnClickListener(v -> {
            Toast.makeText(this, "Đang kết nối với đơn vị vận chuyển...", Toast.LENGTH_SHORT).show();
        });

        binding.btnTrackOrder.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng theo dõi đơn hàng đang được cập nhật", Toast.LENGTH_SHORT).show();
        });

        binding.btnReview.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng đánh giá đơn hàng " + (order != null ? order.getId() : "") + " đang được phát triển", Toast.LENGTH_SHORT).show();
        });

        binding.btnRequestRefund.setOnClickListener(v -> {
            Toast.makeText(this, "Yêu cầu trả hàng cho đơn " + (order != null ? order.getId() : "") + " đã được ghi nhận", Toast.LENGTH_SHORT).show();
        });

        binding.btnViewOrderDetail.setOnClickListener(v -> {
            // Re-populate with normal data if needed, or just stay here
            Toast.makeText(this, "Hiển thị chi tiết đơn hàng đầy đủ", Toast.LENGTH_SHORT).show();
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

    private void updateStatusUI(String statusArg) {
        String status = statusArg != null ? statusArg : "PENDING";
        
        // Reset all visibility
        binding.txtHeaderTitle.setText("Thông tin đơn hàng");
        binding.txtStatusBanner.setVisibility(View.VISIBLE);
        binding.layoutCancelledBanner.setVisibility(View.GONE);
        binding.layoutShippingInfo.setVisibility(View.VISIBLE);
        binding.lblAddress.setVisibility(View.VISIBLE);
        binding.ivLocationIcon.setVisibility(View.VISIBLE);
        binding.txtCustomerName.setVisibility(View.VISIBLE);
        binding.txtCustomerPhone.setVisibility(View.VISIBLE);
        binding.txtCustomerAddress.setVisibility(View.VISIBLE);
        binding.layoutCancelledSummary.setVisibility(View.GONE);
        binding.layoutOrderInfo.setVisibility(View.VISIBLE);
        
        binding.layoutPendingActions.setVisibility(View.GONE);
        binding.layoutShippingActions.setVisibility(View.GONE);
        binding.layoutCompletedActions.setVisibility(View.GONE);
        binding.layoutCancelledActions.setVisibility(View.GONE);
        
        binding.btnReturnRefund.setVisibility(View.GONE);
        binding.lineReturn.setVisibility(View.GONE);

        String statusUpper = status.toUpperCase();
        binding.txtStatusBanner.setBackgroundColor(0xFF008B86); // Teal color for all

        if (statusUpper.contains("CANCELLED") || statusUpper.contains("HỦY")) {
            binding.txtHeaderTitle.setText("Chi tiết đơn hủy");
            binding.txtStatusBanner.setVisibility(View.GONE);
            binding.layoutCancelledBanner.setVisibility(View.VISIBLE);
            binding.layoutShippingInfo.setVisibility(View.GONE);
            
            // Hide Address details in cancelled view to match image
            binding.lblAddress.setVisibility(View.GONE);
            binding.ivLocationIcon.setVisibility(View.GONE);
            binding.txtCustomerName.setVisibility(View.GONE);
            binding.txtCustomerPhone.setVisibility(View.GONE);
            binding.txtCustomerAddress.setVisibility(View.GONE);
            
            binding.layoutCancelledSummary.setVisibility(View.VISIBLE);
            binding.layoutOrderInfo.setVisibility(View.GONE);
            
            binding.layoutCancelledActions.setVisibility(View.VISIBLE);
            
            binding.txtCancelledTime.setText("vào " + formatDate(order.getCancelledAt() != null ? order.getCancelledAt() : order.getUpdatedAt()));
            binding.txtCancelledBy.setText(order.getCancelledBy() != null ? order.getCancelledBy() : "Người mua");
            binding.txtCancelledRequestTime.setText(formatDate(order.getCancelledAt() != null ? order.getCancelledAt() : order.getUpdatedAt()));
            binding.txtCancellationReason.setText(order.getCancellationReason() != null ? order.getCancellationReason() : "Lý do khác");
            binding.txtCancelledPaymentMethod.setText(order.getPaymentMethod() != null ? order.getPaymentMethod() : "COD");
            
        } else if (statusUpper.contains("DELIVERED") || statusUpper.contains("COMPLETED") 
                || statusUpper.contains("ĐÃ GIAO") || statusUpper.contains("HOÀN THÀNH")) {
            binding.txtStatusBanner.setText("Đơn hàng đã hoàn thành");
            binding.layoutCompletedActions.setVisibility(View.VISIBLE);
            
            // Check if > 5 days
            boolean isOld = isOlderThan5Days(order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt());
            
            if (isOld) {
                binding.btnReturnRefund.setVisibility(View.GONE);
                binding.lineReturn.setVisibility(View.GONE);
                
                binding.btnReview.setText("Mua lại");
                binding.btnRequestRefund.setText("Đánh giá");
                
                binding.btnReview.setOnClickListener(v -> {
                    Toast.makeText(this, "Đã thêm các sản phẩm vào giỏ hàng để mua lại", Toast.LENGTH_SHORT).show();
                });
                binding.btnRequestRefund.setOnClickListener(v -> {
                    Toast.makeText(this, "Mở chức năng đánh giá đơn hàng", Toast.LENGTH_SHORT).show();
                });
            } else {
                binding.btnReturnRefund.setVisibility(View.VISIBLE);
                binding.lineReturn.setVisibility(View.VISIBLE);

                binding.btnReview.setText("Đánh giá");
                binding.btnRequestRefund.setText("Yêu cầu Trả hàng/Hoàn tiền");
                
                binding.btnReview.setOnClickListener(v -> {
                    Toast.makeText(this, "Mở chức năng đánh giá đơn hàng", Toast.LENGTH_SHORT).show();
                });
                binding.btnRequestRefund.setOnClickListener(v -> {
                    Toast.makeText(this, "Yêu cầu trả hàng cho đơn " + (order != null ? order.getId() : "") + " đã được ghi nhận", Toast.LENGTH_SHORT).show();
                });
            }
            
        } else if (statusUpper.contains("SHIPPING") || statusUpper.contains("GIAO")) {
            binding.txtStatusBanner.setText("Đang giao hàng");
            binding.layoutShippingActions.setVisibility(View.VISIBLE);

        } else if (statusUpper.equals("PENDING") || statusUpper.contains("XÁC NHẬN") || statusUpper.contains("LẤY HÀNG") 
                || statusUpper.contains("PROCESSING") || statusUpper.contains("XỬ LÝ") || statusUpper.contains("WAITING")
                || statusUpper.contains("CHỜ")) {
            
            if (statusUpper.contains("LẤY HÀNG") || statusUpper.contains("PROCESSING") || statusUpper.contains("XỬ LÝ")) {
                binding.txtStatusBanner.setText("Chờ lấy hàng");
            } else {
                binding.txtStatusBanner.setText("Chờ xác nhận");
            }
            binding.layoutPendingActions.setVisibility(View.VISIBLE);

        } else {
            binding.txtStatusBanner.setText(status);
        }
    }

    private boolean isOlderThan5Days(String dateStr) {
        if (dateStr == null) return false;
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            if (date == null) return false;
            
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -5);
            return date.before(cal.getTime());
        } catch (Exception e) {
            return false;
        }
    }

    private void showCancelOrderBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_cancel_order_reason, null);
        bottomSheetDialog.setContentView(view);

        RadioGroup rgReasons = view.findViewById(R.id.rgReasons);
        view.findViewById(R.id.btnClose).setOnClickListener(v -> bottomSheetDialog.dismiss());
        
        view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            int selectedId = rgReasons.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Vui lòng chọn lý do hủy", Toast.LENGTH_SHORT).show();
                return;
            }
            
            RadioButton rb = view.findViewById(selectedId);
            String reason = rb.getText().toString();
            bottomSheetDialog.dismiss();
            cancelOrderOnServer(reason);
        });

        bottomSheetDialog.show();
    }

    private void cancelOrderOnServer(String reason) {
        if (order == null || order.get_id() == null) return;

        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("status", "Đã hủy");
        body.put("cancellationReason", reason);
        body.put("cancelledBy", "Người mua");
        body.put("cancelledAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date()));

        ApiClient.INSTANCE.getInstance().updateOrderStatus(order.get_id(), body).enqueue(new Callback<Order>() {
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
