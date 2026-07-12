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

    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> returnRefundLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Order updated = (Order) result.getData().getSerializableExtra("updated_order");
                    if (updated != null) {
                        this.order = updated;
                        populateData();
                    } else if (order.get_id() != null) {
                        fetchOrderDetail(order.get_id());
                    }
                }
            });

    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> reviewLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (order.get_id() != null) {
                        fetchOrderDetail(order.get_id());
                    }
                }
            });

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
        String orderId = getIntent().getStringExtra("order_id");

        if (order == null && orderId == null) {
            Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        setupSuggestionRecyclerView();

        if (order != null) {
            populateData();
            if (order.get_id() != null) {
                fetchOrderDetail(order.get_id());
            }
        } else {
            // Only orderId is provided, fetch details immediately
            fetchOrderDetail(orderId);
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
            com.project.utils.SessionManager sm = new com.project.utils.SessionManager(this);
            if (!sm.isLoggedIn()) {
                android.content.Intent intent = new android.content.Intent(this, com.project.muse_android.auth.AuthActivity.class);
                startActivity(intent);
            } else {
                android.content.Intent intent = new android.content.Intent(this, com.project.muse_android.profile.ShopChatActivity.class);
                startActivity(intent);
            }
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
            com.project.utils.SessionManager sm = new com.project.utils.SessionManager(this);
            if (!sm.isLoggedIn()) {
                android.content.Intent intent = new android.content.Intent(this, com.project.muse_android.auth.AuthActivity.class);
                startActivity(intent);
            } else {
                android.content.Intent intent = new android.content.Intent(this, com.project.muse_android.profile.ShopChatActivity.class);
                startActivity(intent);
            }
        });
        
        binding.btnHelp.setOnClickListener(v -> {
            Toast.makeText(this, "Mở Trung tâm hỗ trợ", Toast.LENGTH_SHORT).show();
        });
    }

    private void populateData() {
        // 1. Status Banner
        updateStatusUI(order.getStatus());

        // 2. Shipping Info
        String shippingName = "Giao hàng tiêu chuẩn";
        if (order.getShippingMethod() != null && order.getShippingMethod().getName() != null) {
            shippingName = order.getShippingMethod().getName();
        }
        String shippingCodeSuffix = "";
        if (order.getId() != null) {
            if (order.getId().length() > 8) {
                shippingCodeSuffix = order.getId().substring(order.getId().length() - 8).toUpperCase();
            } else {
                shippingCodeSuffix = order.getId().toUpperCase();
            }
        }
        binding.txtShippingCode.setText("Phương thức: " + shippingName + " (Mã vận đơn: ORD-" + shippingCodeSuffix + ")");

        // 3. Address Section
        binding.txtCustomerName.setText(order.getCustomerName());
        binding.txtCustomerPhone.setText(order.getPhone());
        binding.txtCustomerAddress.setText("Địa chỉ: " + getFullAddress(order));

        // 4. Order Items
        String statusUpper = order.getStatus() != null ? order.getStatus().toUpperCase() : "";
        if ((statusUpper.contains("TRẢ HÀNG") || statusUpper.contains("RETURN")) 
                && order.getReturnItems() != null && !order.getReturnItems().isEmpty()) {
            adapter.setData(order.getReturnProducts());
        } else {
            adapter.setData(order.getProducts());
        }

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
        
        String paymentMethod = null;
        if (order.getPaymentId() != null) {
            paymentMethod = order.getPaymentId().getPaymentMethod();
        }
        if (paymentMethod == null) {
            paymentMethod = order.getPaymentMethod();
        }
        
        String paymentMethodFriendly = "Thanh toán khi nhận hàng (COD)";
        if ("MOMO".equalsIgnoreCase(paymentMethod)) {
            paymentMethodFriendly = "Ví điện tử MoMo";
        } else if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            paymentMethodFriendly = "Thẻ ATM/Ứng dụng Ngân hàng (VNPay)";
        }
        binding.txtPaymentMethod.setText(paymentMethodFriendly);
        
        binding.txtOrderTime.setText(formatDate(order.getCreatedAt()));

        View relativeLayoutPaymentTime = (View) binding.txtPaymentTime.getParent();
        if ("COD".equalsIgnoreCase(paymentMethod) || paymentMethod == null) {
            String status = order.getStatus() != null ? order.getStatus().toUpperCase() : "";
            if (status.contains("DELIVERED") || status.contains("COMPLETED") 
                    || status.contains("ĐÃ GIAO") || status.contains("HOÀN THÀNH")) {
                if (relativeLayoutPaymentTime != null) {
                    relativeLayoutPaymentTime.setVisibility(View.VISIBLE);
                }
                binding.txtPaymentTime.setText(formatDate(order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt()));
            } else {
                if (relativeLayoutPaymentTime != null) {
                    relativeLayoutPaymentTime.setVisibility(View.GONE);
                }
            }
        } else {
            if (relativeLayoutPaymentTime != null) {
                relativeLayoutPaymentTime.setVisibility(View.VISIBLE);
            }
            binding.txtPaymentTime.setText(formatDate(order.getCreatedAt()));
        }

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
            binding.btnCancelledLeft.setVisibility(View.GONE);
            binding.spaceCancelled.setVisibility(View.GONE);
            binding.btnViewOrderDetail.setVisibility(View.VISIBLE);
            binding.btnViewOrderDetail.setText("Mua lại");
            binding.btnViewOrderDetail.setOnClickListener(v -> {
                ArrayList<Product> reorderProducts = new ArrayList<>(order.getProducts());
                if (!reorderProducts.isEmpty()) {
                    android.content.Intent intent = new android.content.Intent(this, com.project.muse_android.checkout.CheckoutActivity.class);
                    intent.putExtra("products", reorderProducts);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Không có sản phẩm nào để mua lại", Toast.LENGTH_SHORT).show();
                }
            });
            
            binding.layoutCancelledSummary.setVisibility(View.VISIBLE);
            binding.txtCancelledByLabel.setText("Yêu cầu bởi");
            binding.txtCancelledBy.setText(order.getCancelledBy() != null ? order.getCancelledBy() : "Người mua");
            binding.txtCancelledRequestTimeLabel.setText("Yêu cầu vào");
            binding.txtCancelledRequestTime.setText(formatDate(order.getCancelledAt() != null ? order.getCancelledAt() : order.getUpdatedAt()));
            binding.txtCancellationReasonLabel.setText("Lý do");
            binding.txtCancellationReason.setText(order.getCancellationReason() != null ? order.getCancellationReason() : "Lý do khác");
            binding.txtCancelledPaymentMethodLabel.setText("Phương thức thanh toán");
            binding.txtCancelledPaymentMethod.setText(order.getPaymentMethod() != null ? order.getPaymentMethod() : "COD");
            
        } else if (statusUpper.contains("TRẢ HÀNG") || statusUpper.contains("RETURN")) {
            binding.txtHeaderTitle.setText("Chi tiết trả hàng");
            binding.txtStatusBanner.setText(mapReturnStatusToText(status));
            binding.txtStatusBanner.setBackgroundColor(0xFFFB6F92); // Pinkish red for returns
            
            binding.layoutCancelledSummary.setVisibility(View.VISIBLE);
            binding.txtCancelledByLabel.setText("Yêu cầu bởi");
            binding.txtCancelledBy.setText(order.getReturnMethod() != null ? order.getReturnMethod() : "Người mua");
            binding.txtCancelledRequestTimeLabel.setText("Yêu cầu vào");
            binding.txtCancelledRequestTime.setText(formatDate(order.getReturnRequestedAt() != null ? order.getReturnRequestedAt() : order.getUpdatedAt()));
            binding.txtCancellationReasonLabel.setText("Lý do");
            binding.txtCancellationReason.setText(order.getReturnReason() != null ? (order.getReturnReason() + (order.getReturnNote() != null && !order.getReturnNote().isEmpty() ? "\nGhi chú: " + order.getReturnNote() : "")) : "Không có lý do");
            
            binding.txtCancelledPaymentMethodLabel.setText("Phương thức hoàn tiền");
            binding.txtCancelledPaymentMethod.setText("Tài khoản ngân hàng");
            
            binding.layoutCancelledActions.setVisibility(View.VISIBLE);
            binding.btnCancelledLeft.setVisibility(View.VISIBLE);
            binding.btnCancelledLeft.setText("Liên hệ");
            binding.btnCancelledLeft.setOnClickListener(v -> {
                android.content.Intent chatIntent = new android.content.Intent(this, com.project.muse_android.profile.ShopChatActivity.class);
                startActivity(chatIntent);
            });
            binding.spaceCancelled.setVisibility(View.VISIBLE);
            binding.btnViewOrderDetail.setVisibility(View.VISIBLE);
            binding.btnViewOrderDetail.setText("Mua lại");
            binding.btnViewOrderDetail.setOnClickListener(v -> {
                ArrayList<Product> reorderProducts = new ArrayList<>();
                if (order.getReturnItems() != null && !order.getReturnItems().isEmpty()) {
                    reorderProducts.addAll(order.getReturnProducts());
                } else {
                    reorderProducts.addAll(order.getProducts());
                }
                
                if (!reorderProducts.isEmpty()) {
                    android.content.Intent intent = new android.content.Intent(this, com.project.muse_android.checkout.CheckoutActivity.class);
                    intent.putExtra("products", reorderProducts);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Không có sản phẩm nào để mua lại", Toast.LENGTH_SHORT).show();
                }
            });

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
                    openWriteReview();
                });
            } else {
                binding.btnReturnRefund.setVisibility(View.VISIBLE);
                binding.lineReturn.setVisibility(View.VISIBLE);

                binding.btnReview.setText("Đánh giá");
                binding.btnRequestRefund.setText("Yêu cầu Trả hàng");
                
                binding.btnReview.setOnClickListener(v -> {
                    openWriteReview();
                });
                binding.btnRequestRefund.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(this, com.project.muse_android.order.ReturnRefundActivity.class);
                    intent.putExtra("order", order);
                    returnRefundLauncher.launch(intent);
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
                    
                    String orderIdStr = order.getId() != null ? order.getId() : (order.get_id() != null ? order.get_id() : "");
                    String displayId = (orderIdStr.startsWith("ORD") || orderIdStr.length() <= 8)
                            ? orderIdStr : orderIdStr.substring(orderIdStr.length() - 8);
                    
                    com.project.models.Notification localNotif = new com.project.models.Notification();
                    localNotif.setTitle("Đơn hàng đã hủy");
                    localNotif.setMessage("Đơn hàng #" + displayId + " của bạn đã được hủy thành công.");
                    localNotif.setType("order");
                    localNotif.setStatus("unread");
                    localNotif.setTargetId(order.get_id()); // Fixed: Add targetId for navigation
                    localNotif.setCreatedAt(new java.util.Date());
                    
                    new com.project.utils.SessionManager(OrderDetailActivity.this).addLocalNotification(localNotif);
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

    private String getFullAddress(Order order) {
        if (order == null) return "";
        Order.ShippingAddress sa = order.getShippingAddress();
        if (sa != null) {
            StringBuilder sb = new StringBuilder();
            if (sa.getAddress() != null && !sa.getAddress().isEmpty()) {
                sb.append(sa.getAddress());
            }
            if (sa.getWard() != null && !sa.getWard().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(sa.getWard());
            }
            if (sa.getDistrict() != null && !sa.getDistrict().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(sa.getDistrict());
            }
            if (sa.getCity() != null && !sa.getCity().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(sa.getCity());
            }
            return sb.toString();
        }
        return order.getAddress() != null ? order.getAddress() : "";
    }

    private void fetchOrderDetail(String orderId) {
        fetchOrderDetail(orderId, true);
    }

    private void fetchOrderDetail(String orderId, boolean allowFallback) {
        ApiClient.INSTANCE.getInstance().getOrderDetail(orderId).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    order = response.body();
                    populateData();
                } else {
                    if (allowFallback) {
                        fetchOrderByUserFacingId(orderId);
                    } else {
                        Toast.makeText(OrderDetailActivity.this, "Không thể tải chi tiết đơn hàng", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                if (allowFallback) {
                    fetchOrderByUserFacingId(orderId);
                } else {
                    Toast.makeText(OrderDetailActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchOrderByUserFacingId(String userFacingId) {
        if (userFacingId == null || userFacingId.isEmpty()) return;
        com.project.utils.SessionManager sessionManager = new com.project.utils.SessionManager(this);
        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem chi tiết đơn hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.INSTANCE.getInstance().getMyOrders(userId).enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(@NonNull Call<List<Order>> call, @NonNull Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Order o : response.body()) {
                        String orderId = o.getId() != null ? o.getId() : "";
                        String orderDbId = o.get_id() != null ? o.get_id() : "";
                        if (userFacingId.equalsIgnoreCase(orderId) 
                                || userFacingId.equalsIgnoreCase(orderDbId)
                                || (orderId.length() >= userFacingId.length() && orderId.endsWith(userFacingId))
                                || (orderDbId.length() >= userFacingId.length() && orderDbId.endsWith(userFacingId))) {
                            order = o;
                            populateData();
                            if (order.get_id() != null) {
                                fetchOrderDetail(order.get_id(), false);
                            }
                            return;
                        }
                    }
                }
                Toast.makeText(OrderDetailActivity.this, "Không tìm thấy thông tin đơn hàng: " + userFacingId, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<List<Order>> call, @NonNull Throwable t) {
                Toast.makeText(OrderDetailActivity.this, "Không thể lấy thông tin đơn hàng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String mapReturnStatusToText(String status) {
        if (status == null) return "Trả hàng";
        switch (status.toUpperCase()) {
            case "YÊU CẦU TRẢ HÀNG": return "Chờ xác nhận trả hàng/hoàn tiền";
            case "ĐANG TRẢ HÀNG": return "Đang trong quá trình trả hàng";
            case "ĐÃ TRẢ HÀNG": return "Trả hàng & Hoàn tiền thành công";
            case "TỪ CHỐI TRẢ HÀNG": return "Từ chối yêu cầu trả hàng";
            default: return status;
        }
    }

    private void openWriteReview() {
        if (order != null) {
            android.content.Intent intent = new android.content.Intent(this, com.project.muse_android.product.WriteReviewActivity.class);
            intent.putExtra("order", order);
            reviewLauncher.launch(intent);
        }
    }
}
