package com.project.muse_android.checkout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.project.adapters.HorizontalProductAdapter;
import com.project.models.Product;
import com.project.models.ProductVariant;
import com.project.models.User;
import com.project.models.enums.HorizontalProductMode;
import com.project.muse_android.R;
import com.project.muse_android.cart.PromotionDetailsBottomSheetFragment;
import com.project.muse_android.databinding.ActivityCheckoutBinding;
import com.project.muse_android.voucher.VoucherBottomSheetFragment;
import com.project.network.ApiClient;
import com.project.network.ApiResponse;
import com.project.models.Order;
import com.project.utils.SessionManager;
import com.project.utils.ViewUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutActivity extends AppCompatActivity {

    private ActivityCheckoutBinding binding;
    private List<Product> checkoutProducts = new ArrayList<>();
    private HorizontalProductAdapter adapter;
    private SessionManager sessionManager;
    
    private double originalTotal = 0;
    private double productDiscount = 0;
    private double voucherDiscount = 0; 
    private String selectedVoucherCode = "";
    private double shippingFee = 23000; // Default to standard fee
    private double shippingDiscount = 0; 

    private int selectedShippingMethod = 1; // 1: Standard, 2: Fast, 3: Express
    private int selectedPaymentMethod = 1; // 1: COD, 2: Bank, 3: Momo, 4: VNPay
    private boolean isOnlinePaymentAuthorized = false;

    private User currentUser;
    private String selectedName;
    private String selectedPhone;
    private String selectedStreet;
    private String selectedWard;
    private String selectedDistrict;
    private String selectedProvince;
    private String orderNote = "";

    private final ActivityResultLauncher<Intent> addressLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    selectedStreet = data.getStringExtra("selected_address_street");
                    selectedWard = data.getStringExtra("selected_address_ward");
                    selectedDistrict = data.getStringExtra("selected_address_district");
                    selectedProvince = data.getStringExtra("selected_address_province");
                    selectedName = data.getStringExtra("selected_user_name");
                    selectedPhone = data.getStringExtra("selected_user_phone");

                    binding.txtUserName.setText(selectedName);
                    binding.txtUserPhone.setText(selectedPhone);

                    StringBuilder sb = new StringBuilder();
                    if (selectedStreet != null && !selectedStreet.isEmpty()) sb.append(selectedStreet);
                    if (selectedWard != null && !selectedWard.isEmpty()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(selectedWard);
                    }
                    if (selectedDistrict != null && !selectedDistrict.isEmpty()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(selectedDistrict);
                    }
                    if (selectedProvince != null && !selectedProvince.isEmpty()) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(selectedProvince);
                    }
                    binding.txtAddress.setText(sb.toString());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewUtils.applySystemBarsPadding(binding.header, true, false);
        ViewUtils.applySystemBarsPadding(binding.bottomContainer, false, true);

        sessionManager = new SessionManager(this);
        loadData();
        setupUI();
        updateShippingUI();
        updatePaymentUI();
        calculatePrices();
        fetchUserProfile();
    }

    private void fetchUserProfile() {
        String token = sessionManager.getToken();
        if (token == null) return;

        ApiClient.INSTANCE.getInstance().getProfile("Bearer " + token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    updateAddressUI();
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                // Ignore failure for now
            }
        });
    }

    private void updateAddressUI() {
        if (currentUser != null) {
            if (currentUser.getAddresses() != null && !currentUser.getAddresses().isEmpty()) {
                User.Address def = null;
                for (User.Address a : currentUser.getAddresses()) {
                    if (a.isDefault()) {
                        def = a;
                        break;
                    }
                }
                if (def == null) def = currentUser.getAddresses().get(0);
                
                selectedStreet = def.getStreet();
                selectedWard = def.getWard();
                selectedDistrict = def.getDistrict();
                selectedProvince = def.getProvince();

                // Use address contact info if available, otherwise fallback to account info
                selectedName = (def.getFullName() != null && !def.getFullName().isEmpty()) ? def.getFullName() : currentUser.getName();
                selectedPhone = (def.getPhone() != null && !def.getPhone().isEmpty()) ? def.getPhone() : 
                                (currentUser.getPhone() != null ? currentUser.getPhone() : "Chưa có số điện thoại");

                binding.txtUserName.setText(selectedName);
                binding.txtUserPhone.setText(selectedPhone);

                StringBuilder sb = new StringBuilder();
                if (selectedStreet != null && !selectedStreet.isEmpty()) sb.append(selectedStreet);
                if (selectedWard != null && !selectedWard.isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(selectedWard);
                }
                if (selectedDistrict != null && !selectedDistrict.isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(selectedDistrict);
                }
                if (selectedProvince != null && !selectedProvince.isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(selectedProvince);
                }
                binding.txtAddress.setText(sb.toString());
            } else {
                selectedName = currentUser.getName();
                selectedPhone = currentUser.getPhone() != null ? currentUser.getPhone() : "Chưa có số điện thoại";
                
                binding.txtUserName.setText(selectedName);
                binding.txtUserPhone.setText(selectedPhone);
                binding.txtAddress.setText("Vui lòng thiết lập địa chỉ giao hàng");
            }
        }
    }

    private void loadData() {
        // Retrieve products passed from CartFragment
        List<Product> list = getIntent().getParcelableArrayListExtra("products");
        if (list != null) {
            checkoutProducts.addAll(list);
        }
        
        // Retrieve voucher discount passed from CartFragment
        voucherDiscount = getIntent().getDoubleExtra("voucher_discount", 0);
        selectedVoucherCode = getIntent().getStringExtra("voucher_code");
        if (selectedVoucherCode == null) selectedVoucherCode = "";
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.cardAddress.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.project.muse_android.address.ShippingAddressActivity.class);
            addressLauncher.launch(intent);
        });

        // Setup RecyclerView with Adapter in READ_ONLY mode
        adapter = new HorizontalProductAdapter(this, HorizontalProductMode.READ_ONLY, new HorizontalProductAdapter.OnProductActionListener() {
            @Override
            public void onDelete(Product product, int position) {}
            @Override
            public void onSimilar(Product product, int position) {}
            @Override
            public void onCheckedChanged(Product product, int position, boolean checked) {}
            @Override
            public void onQuantityChanged(Product product, int position, int quantity) {}
            @Override
            public void onVariantClick(Product product, int position) {}
            @Override
            public void onProductClick(Product product) {}
        });

        binding.rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrderItems.setAdapter(adapter);
        adapter.setData(checkoutProducts);

        binding.btnSelectVoucher.setOnClickListener(v -> {
            VoucherBottomSheetFragment voucherSheet = new VoucherBottomSheetFragment();
            voucherSheet.setOnVoucherSelectedListener((discount, shipping, code) -> {
                this.voucherDiscount = discount;
                this.shippingDiscount = shipping;
                this.selectedVoucherCode = code;
                calculatePrices();
            });
            voucherSheet.show(getSupportFragmentManager(), "VoucherBottomSheet");
        });

        binding.layoutPriceSummary.setOnClickListener(v -> {
            PromotionDetailsBottomSheetFragment sheet = new PromotionDetailsBottomSheetFragment(
                    originalTotal,
                    voucherDiscount,
                    productDiscount,
                    shippingFee,
                    shippingDiscount
            );
            sheet.show(getSupportFragmentManager(), "PromotionDetails");
        });

        binding.cardShippingStandard.setOnClickListener(v -> {
            selectedShippingMethod = 1;
            shippingFee = 23000;
            updateShippingUI();
            calculatePrices();
        });

        binding.cardShippingFast.setOnClickListener(v -> {
            selectedShippingMethod = 2;
            shippingFee = 38000;
            updateShippingUI();
            calculatePrices();
        });

        binding.cardShippingExpress.setOnClickListener(v -> {
            selectedShippingMethod = 3;
            shippingFee = 55000;
            updateShippingUI();
            calculatePrices();
        });

        binding.cardPaymentCOD.setOnClickListener(v -> {
            selectedPaymentMethod = 1;
            isOnlinePaymentAuthorized = false;
            updatePaymentUI();
        });

        binding.cardPaymentBank.setOnClickListener(v -> {
            showPaymentAuthorizationDialog(2);
        });

        binding.cardPaymentMomo.setOnClickListener(v -> {
            showPaymentAuthorizationDialog(3);
        });

        binding.cardPaymentVNPay.setOnClickListener(v -> {
            showPaymentAuthorizationDialog(4);
        });

        binding.btnNote.setOnClickListener(v -> {
            MessageBottomSheetFragment messageSheet = MessageBottomSheetFragment.newInstance(orderNote);
            messageSheet.setOnMessageSubmittedListener(message -> {
                orderNote = message;
                if (message.trim().isEmpty()) {
                    binding.txtNoteValue.setText("Để lại lời nhắn");
                    binding.txtNoteValue.setTextColor(android.graphics.Color.parseColor("#B4B4B4"));
                } else {
                    binding.txtNoteValue.setText(message);
                    binding.txtNoteValue.setTextColor(android.graphics.Color.parseColor("#1F1C1C"));
                }
            });
            messageSheet.show(getSupportFragmentManager(), "MessageBottomSheet");
        });

        binding.btnOrder.setOnClickListener(v -> {
            placeOrder();
        });
    }

    private void showPaymentAuthorizationDialog(int paymentMethod) {
        String methodName = "";
        switch (paymentMethod) {
            case 2: methodName = "Ngân hàng"; break;
            case 3: methodName = "MoMo"; break;
            case 4: methodName = "VNPay"; break;
        }

        final String finalMethodName = methodName;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Yêu cầu truy cập")
                .setMessage("Cho phép ứng dụng MUSE truy cập vào ứng dụng " + finalMethodName + " để thực hiện thanh toán?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    isOnlinePaymentAuthorized = true;
                    selectedPaymentMethod = paymentMethod;
                    updatePaymentUI();
                    Toast.makeText(CheckoutActivity.this, "Đã cấp quyền truy cập ứng dụng " + finalMethodName + " thành công. Bạn có thể tiến hành thanh toán.", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Không đồng ý", (dialog, which) -> {
                    isOnlinePaymentAuthorized = false;
                    selectedPaymentMethod = paymentMethod;
                    updatePaymentUI();
                    Toast.makeText(CheckoutActivity.this, "Bạn đã từ chối quyền truy cập. Không thể thanh toán qua phương thức này.", Toast.LENGTH_LONG).show();
                })
                .setCancelable(false)
                .show();
    }

    private void placeOrder() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Đang tải thông tin người dùng, vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare Order data
        Order order = new Order();
        order.setUserId(currentUser.get_id());
        order.setCustomerName(selectedName != null ? selectedName : currentUser.getName());
        order.setEmail(currentUser.getEmail());
        order.setPhone(selectedPhone != null && !selectedPhone.equals("Chưa có số điện thoại") ? selectedPhone : (currentUser.getPhone() != null ? currentUser.getPhone() : "0000000000"));
        
        String address = binding.txtAddress.getText().toString();
        if (address.contains("Vui lòng thiết lập")) {
            Toast.makeText(this, "Vui lòng thêm địa chỉ giao hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        order.setAddress(address);
        
        // Build nested ShippingAddress matching MongoDB
        Order.ShippingAddress shippingAddress = new Order.ShippingAddress(
            order.getCustomerName(),
            order.getEmail(),
            order.getPhone(),
            selectedStreet != null ? selectedStreet : address,
            selectedProvince != null ? selectedProvince : "",
            selectedDistrict != null ? selectedDistrict : "",
            selectedWard != null ? selectedWard : ""
        );
        order.setShippingAddress(shippingAddress);
        
        List<Order.OrderItem> items = new ArrayList<>();
        double itemTotalDiscounted = 0;
        for (Product p : checkoutProducts) {
            items.add(createOrderItem(p));
            
            double discPrice = (p.getDiscountPrice() != null && p.getDiscountPrice() > 0) 
                    ? p.getDiscountPrice() : p.getPrice();
            itemTotalDiscounted += discPrice * (p.getQuantity() > 0 ? p.getQuantity() : 1);
        }
        order.setItems(items);

        // Build nested ShippingMethod
        String shippingMethodName = "Giao hàng tiêu chuẩn";
        if (selectedShippingMethod == 2) {
            shippingMethodName = "Giao hàng nhanh";
        } else if (selectedShippingMethod == 3) {
            shippingMethodName = "Hỏa tốc";
        }
        order.setShippingMethod(new Order.ShippingMethod(shippingMethodName, shippingFee));

        // Build nested Promotion
        order.setPromotion(new Order.Promotion(voucherDiscount, selectedVoucherCode));

        // Set prices
        order.setSubTotal(itemTotalDiscounted);
        double finalPrice = itemTotalDiscounted + shippingFee - shippingDiscount - voucherDiscount;
        order.setTotalPrice(finalPrice);
        order.setDiscount(voucherDiscount);
        order.setFinalPrice(finalPrice);
        
        order.setStatus("Đang xử lý");
        order.setNote(orderNote.trim().isEmpty() ? "Đặt hàng từ Android App" : orderNote.trim());
        
        // Map payment method
        String pMethod = "COD";
        switch (selectedPaymentMethod) {
            case 2: pMethod = "BANK"; break;
            case 3: pMethod = "MOMO"; break;
            case 4: pMethod = "VNPAY"; break;
        }

        if (selectedPaymentMethod != 1 && !isOnlinePaymentAuthorized) {
            Toast.makeText(this, "Vui lòng cho phép liên kết ứng dụng thanh toán hoặc chọn phương thức thanh toán khác.", Toast.LENGTH_LONG).show();
            return;
        }

        order.setPaymentMethod(pMethod);
        if (selectedPaymentMethod != 1 && isOnlinePaymentAuthorized) {
            order.setPaymentStatus("Đã thanh toán");
            order.setStatus("Đang xử lý"); // Processing since it's already paid
        } else {
            order.setPaymentStatus("Chưa thanh toán");
            order.setStatus("Đang xử lý");
        }
        order.setVoucherCode(selectedVoucherCode);

        binding.btnOrder.setEnabled(false);
        binding.btnOrder.setText("ĐANG XỬ LÝ...");

        ApiClient.INSTANCE.getInstance().createOrder(order).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                if (response.isSuccessful()) {
                    Order createdOrder = response.body();
                    if (createdOrder != null) {
                        String orderIdStr = createdOrder.getId() != null ? createdOrder.getId() : "";
                        String displayId = orderIdStr.length() > 8 ? orderIdStr.substring(orderIdStr.length() - 8) : orderIdStr;
                        
                        com.project.models.Notification localNotif = new com.project.models.Notification();
                        localNotif.setTitle("Đặt hàng thành công");
                        localNotif.setMessage("Đơn hàng #" + displayId + " của bạn đã được đặt thành công!");
                        localNotif.setType("order");
                        localNotif.setStatus("unread");
                        localNotif.setCreatedAt(new java.util.Date());
                        
                        sessionManager.addLocalNotification(localNotif);
                    }
                    
                    Intent intent = new Intent(CheckoutActivity.this, com.project.muse_android.order.OrderSuccessActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    binding.btnOrder.setEnabled(true);
                    binding.btnOrder.setText("ĐẶT HÀNG");
                    Toast.makeText(CheckoutActivity.this, "Đặt hàng thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                binding.btnOrder.setEnabled(true);
                binding.btnOrder.setText("ĐẶT HÀNG");
                android.util.Log.e("CheckoutActivity", "Lỗi kết nối khi đặt hàng", t);
                t.printStackTrace();
                Toast.makeText(CheckoutActivity.this, "Lỗi kết nối: " + t.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private Order.OrderItem createOrderItem(Product p) {
        String image = "";
        if (p.getImages() != null && !p.getImages().isEmpty()) {
            image = p.getImages().get(0).getUrl();
        }
        
        String color = "";
        String size = "";
        if (p.getVariants() != null && !p.getVariants().isEmpty()) {
            color = p.getVariants().get(0).getColor();
            size = p.getVariants().get(0).getSize();
        }

        double price = (p.getDiscountPrice() != null && p.getDiscountPrice() > 0) 
                ? p.getDiscountPrice() : p.getPrice();
        int quantity = p.getQuantity() > 0 ? p.getQuantity() : 1;

        return new Order.OrderItem(
                p.getId(),
                p.getName(),
                image,
                size,
                color,
                quantity,
                price
        );
    }

    private void updateShippingUI() {
        // Reset all to unselected
        binding.cardShippingStandard.setCardBackgroundColor(getResources().getColor(R.color.white));
        binding.cardShippingStandard.setStrokeColor(getResources().getColor(R.color.neutral_300));
        binding.ivCheckStandard.setImageResource(R.drawable.ic_unchecked_circle);
        binding.ivCheckStandard.setColorFilter(getResources().getColor(R.color.neutral_300));

        binding.cardShippingFast.setCardBackgroundColor(getResources().getColor(R.color.white));
        binding.cardShippingFast.setStrokeColor(getResources().getColor(R.color.neutral_300));
        binding.ivCheckFast.setImageResource(R.drawable.ic_unchecked_circle);
        binding.ivCheckFast.setColorFilter(getResources().getColor(R.color.neutral_300));

        binding.cardShippingExpress.setCardBackgroundColor(getResources().getColor(R.color.white));
        binding.cardShippingExpress.setStrokeColor(getResources().getColor(R.color.neutral_300));
        binding.ivCheckExpress.setImageResource(R.drawable.ic_unchecked_circle);
        binding.ivCheckExpress.setColorFilter(getResources().getColor(R.color.neutral_300));

        // Set selected
        switch (selectedShippingMethod) {
            case 1:
                binding.cardShippingStandard.setCardBackgroundColor(android.graphics.Color.parseColor("#F0FEFF"));
                binding.cardShippingStandard.setStrokeColor(android.graphics.Color.parseColor("#008B86"));
                binding.ivCheckStandard.setImageResource(R.drawable.ic_check_circle);
                binding.ivCheckStandard.setColorFilter(android.graphics.Color.parseColor("#008B86"));
                break;
            case 2:
                binding.cardShippingFast.setCardBackgroundColor(android.graphics.Color.parseColor("#F0FEFF"));
                binding.cardShippingFast.setStrokeColor(android.graphics.Color.parseColor("#008B86"));
                binding.ivCheckFast.setImageResource(R.drawable.ic_check_circle);
                binding.ivCheckFast.setColorFilter(android.graphics.Color.parseColor("#008B86"));
                break;
            case 3:
                binding.cardShippingExpress.setCardBackgroundColor(android.graphics.Color.parseColor("#F0FEFF"));
                binding.cardShippingExpress.setStrokeColor(android.graphics.Color.parseColor("#008B86"));
                binding.ivCheckExpress.setImageResource(R.drawable.ic_check_circle);
                binding.ivCheckExpress.setColorFilter(android.graphics.Color.parseColor("#008B86"));
                break;
        }
    }

    private void updatePaymentUI() {
        // Reset all to unselected
        binding.cardPaymentCOD.setCardBackgroundColor(getResources().getColor(R.color.white));
        binding.cardPaymentCOD.setStrokeColor(getResources().getColor(R.color.neutral_300));
        binding.ivCheckCOD.setImageResource(R.drawable.ic_unchecked_circle);
        binding.ivCheckCOD.setColorFilter(getResources().getColor(R.color.neutral_300));

        binding.cardPaymentBank.setCardBackgroundColor(getResources().getColor(R.color.white));
        binding.cardPaymentBank.setStrokeColor(getResources().getColor(R.color.neutral_300));
        binding.ivCheckBank.setImageResource(R.drawable.ic_unchecked_circle);
        binding.ivCheckBank.setColorFilter(getResources().getColor(R.color.neutral_300));

        binding.cardPaymentMomo.setCardBackgroundColor(getResources().getColor(R.color.white));
        binding.cardPaymentMomo.setStrokeColor(getResources().getColor(R.color.neutral_300));
        binding.ivCheckMomo.setImageResource(R.drawable.ic_unchecked_circle);
        binding.ivCheckMomo.setColorFilter(getResources().getColor(R.color.neutral_300));

        binding.cardPaymentVNPay.setCardBackgroundColor(getResources().getColor(R.color.white));
        binding.cardPaymentVNPay.setStrokeColor(getResources().getColor(R.color.neutral_300));
        binding.ivCheckVNPay.setImageResource(R.drawable.ic_unchecked_circle);
        binding.ivCheckVNPay.setColorFilter(getResources().getColor(R.color.neutral_300));

        // Set selected
        switch (selectedPaymentMethod) {
            case 1:
                binding.cardPaymentCOD.setCardBackgroundColor(android.graphics.Color.parseColor("#FFE5EC"));
                binding.cardPaymentCOD.setStrokeColor(android.graphics.Color.parseColor("#FB6F92"));
                binding.ivCheckCOD.setImageResource(R.drawable.ic_check_circle);
                binding.ivCheckCOD.setColorFilter(android.graphics.Color.parseColor("#FB6F92"));
                break;
            case 2:
                binding.cardPaymentBank.setCardBackgroundColor(android.graphics.Color.parseColor("#FFE5EC"));
                binding.cardPaymentBank.setStrokeColor(android.graphics.Color.parseColor("#FB6F92"));
                binding.ivCheckBank.setImageResource(R.drawable.ic_check_circle);
                binding.ivCheckBank.setColorFilter(android.graphics.Color.parseColor("#FB6F92"));
                break;
            case 3:
                binding.cardPaymentMomo.setCardBackgroundColor(android.graphics.Color.parseColor("#FFE5EC"));
                binding.cardPaymentMomo.setStrokeColor(android.graphics.Color.parseColor("#FB6F92"));
                binding.ivCheckMomo.setImageResource(R.drawable.ic_check_circle);
                binding.ivCheckMomo.setColorFilter(android.graphics.Color.parseColor("#FB6F92"));
                break;
            case 4:
                binding.cardPaymentVNPay.setCardBackgroundColor(android.graphics.Color.parseColor("#FFE5EC"));
                binding.cardPaymentVNPay.setStrokeColor(android.graphics.Color.parseColor("#FB6F92"));
                binding.ivCheckVNPay.setImageResource(R.drawable.ic_check_circle);
                binding.ivCheckVNPay.setColorFilter(android.graphics.Color.parseColor("#FB6F92"));
                break;
        }
    }

    private void calculatePrices() {
        originalTotal = 0;
        productDiscount = 0;
        double itemTotalDiscounted = 0;
        int totalQuantity = 0;

        for (Product p : checkoutProducts) {
            int q = p.getQuantity() > 0 ? p.getQuantity() : 1;
            totalQuantity += q;
            
            originalTotal += p.getPrice() * q;
            double discPrice = (p.getDiscountPrice() != null && p.getDiscountPrice() > 0) 
                    ? p.getDiscountPrice() : p.getPrice();
            
            itemTotalDiscounted += discPrice * q;
            productDiscount += (p.getPrice() - discPrice) * q;
        }

        double finalTotal = itemTotalDiscounted + shippingFee - shippingDiscount - voucherDiscount;

        binding.txtTotalCountLabel.setText("Tổng số tiền (" + totalQuantity + " sản phẩm)");
        binding.txtTotalItemsPrice.setText(formatPrice(itemTotalDiscounted));

        binding.txtSubtotal.setText(formatPrice(itemTotalDiscounted));
        binding.txtShippingFee.setText(formatPrice(shippingFee));
        binding.txtShippingDiscount.setText("-" + formatPrice(shippingDiscount));
        binding.txtVoucherDiscount.setText("-" + formatPrice(voucherDiscount));
        binding.txtTotalAmount.setText(formatPrice(finalTotal));
        
        binding.txtBottomTotal.setText(formatPrice(finalTotal));
        binding.txtBottomSavings.setText("Tiết kiệm: " + formatPrice(productDiscount + voucherDiscount));
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + "đ";
    }
}
