package com.project.muse_android.cart;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.project.adapters.HorizontalProductAdapter;
import com.project.muse_android.voucher.VoucherBottomSheetFragment;
import com.project.adapters.VerticalProductAdapter;
import com.project.models.Product;
import com.project.models.enums.HorizontalProductMode;
import com.project.muse_android.product.ProductDetailActivity;
import com.project.muse_android.dialog.ConfirmDeleteDialog;
import com.project.network.ApiClient;
import com.project.network.ApiService;
import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentCartBinding;
import com.project.utils.ViewUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.project.utils.CartManager;

public class CartFragment extends Fragment {

    private static final String TAG = "CartFragment_API";
    private FragmentCartBinding binding;

    // Adapter cho giỏ hàng (ngang - item_product_horizontal)
    private HorizontalProductAdapter cartAdapter;
    private final List<Product> cartProducts = new ArrayList<>();

    // Adapter cho gợi ý (dọc/grid)
    private VerticalProductAdapter productAdapter;

    private boolean isEditMode = false;
    private double selectedVoucherDiscount = 0;
    private double selectedShippingDiscount = 23000; // Default always 23k
    private String selectedVoucherCode = "";
    private double defaultShippingFee = 23000; // Default Standard shipping fee

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateVoucherUI(); // Ensure default shipping badge is shown immediately

        // Nút quay lại
        binding.ivBack.setOnClickListener(v -> {
            Navigation.findNavController(view).navigateUp();
        });

        // Nút Mua sắm ngay (khi giỏ hàng trống)
        binding.btnShopNow.setOnClickListener(v -> {
            Navigation.findNavController(view).popBackStack();
        });

        // Nút liên kết tới trang Yêu thích (Wishlist)
        binding.ivFavorite.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_wishlist);
        });

        // Cấu hình RecyclerView cho giỏ hàng
        setupCartRecyclerView();

        // Cấu hình RecyclerView gợi ý sản phẩm
        setupSuggestedProductsRecyclerView();

        // Gọi API lấy dữ liệu sản phẩm
        fetchDataFromServer();

        // Sử dụng Helper để tự động đẩy Header xuống dưới Status Bar
        ViewUtils.applySystemBarsPadding(binding.header, true, false);

        // Listen for Window Insets to get navigation bar height
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.bottomContainer, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            int bottomInset = systemBars.bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);
            setupBottomSectionAdjustment(bottomInset);
            return insets;
        });

        // Xử lý Checkbox Chọn tất cả (Buy Bar)
        binding.cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selectAllItems(isChecked);
        });

        // Xử lý Checkbox Chọn tất cả (Edit Bar)
        binding.cbSelectAllEdit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selectAllItems(isChecked);
        });

        binding.btnDeleteSelected.setOnClickListener(v -> deleteSelectedItems());
        binding.btnSaveToFavorites.setOnClickListener(v -> saveSelectedToFavorites());

        // Mở BottomSheet chọn Voucher
        binding.layoutVoucher.setOnClickListener(v -> {
            double selectedOriginalTotal = 0;
            for (Product p : cartProducts) {
                if (p.isSelected()) {
                    double price = (p.getDiscountPrice() != null && p.getDiscountPrice() > 0) ? p.getDiscountPrice() : p.getPrice();
                    selectedOriginalTotal += price * (p.getQuantity() > 0 ? p.getQuantity() : 1);
                }
            }
            VoucherBottomSheetFragment voucherSheet = VoucherBottomSheetFragment.newInstance(selectedOriginalTotal);
            voucherSheet.setOnVoucherSelectedListener((discount, shipping, code) -> {
                this.selectedVoucherDiscount = discount;
                // If user selected a new shipping discount, use it, otherwise keep default 23k
                if (shipping > 0) {
                    this.selectedShippingDiscount = shipping;
                } else {
                    this.selectedShippingDiscount = 23000;
                }
                this.selectedVoucherCode = code;
                updateCheckoutButtonState();
                updateVoucherUI();
            });
            voucherSheet.show(getParentFragmentManager(), "VoucherBottomSheet");
        });

        // Mở BottomSheet chi tiết khuyến mãi
        binding.layoutPriceSummary.setOnClickListener(v -> {
            showPromotionDetails();
        });

        binding.btnCheckout.setOnClickListener(v -> {
            com.project.utils.SessionManager sessionManager = new com.project.utils.SessionManager(requireContext());
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập hoặc đăng ký để tiếp tục mua hàng", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getContext(), com.project.muse_android.auth.AuthActivity.class);
                intent.putExtra("from_checkout", true);
                startActivity(intent);
                return;
            }

            ArrayList<Product> selectedProducts = new ArrayList<>();
            for (Product p : cartProducts) {
                if (p.isSelected()) {
                    selectedProducts.add(p);
                }
            }
            if (!selectedProducts.isEmpty()) {
                Intent intent = new Intent(getContext(), com.project.muse_android.checkout.CheckoutActivity.class);
                intent.putExtra("products", selectedProducts);
                intent.putExtra("voucher_discount", selectedVoucherDiscount);
                intent.putExtra("voucher_code", selectedVoucherCode);
                startActivity(intent);
            }
        });

        // Xử lý nút Sửa
        binding.tvEdit.setOnClickListener(v -> toggleEditMode());
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;

        if (isEditMode) {
            binding.tvEdit.setText("Xong");
            binding.tvEdit.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_500));
            binding.bottomSection.setVisibility(View.GONE);
            binding.layoutEditBar.setVisibility(View.VISIBLE);
        } else {
            binding.tvEdit.setText(getString(R.string.btn_edit));
            binding.tvEdit.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_300));
            binding.bottomSection.setVisibility(View.VISIBLE);
            binding.layoutEditBar.setVisibility(View.GONE);
        }
    }

    private void selectAllItems(boolean isChecked) {
        for (Product product : cartProducts) {
            product.setSelected(isChecked);
        }
        cartAdapter.notifyDataSetChanged();
        updateCheckoutButtonState();
    }

    private void deleteSelectedItems() {
        List<Product> toDelete = new ArrayList<>();
        for (Product p : cartProducts) {
            if (p.isSelected()) {
                toDelete.add(p);
            }
        }

        if (toDelete.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng chọn sản phẩm muốn xóa", Toast.LENGTH_SHORT).show();
            return;
        }

        ConfirmDeleteDialog dialog = new ConfirmDeleteDialog(toDelete.size(), () -> {
            if (!isAdded()) return;
            for (Product p : toDelete) {
                String color = "";
                String size = "";
                if (p.getVariants() != null && !p.getVariants().isEmpty()) {
                    color = p.getVariants().get(0).getColor();
                    size = p.getVariants().get(0).getSize();
                }
                CartManager.getInstance(requireContext()).removeFromCart(p.getId(), size, color, new CartManager.CartCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (!isAdded() || binding == null) return;
                        cartProducts.remove(p);
                        cartAdapter.notifyDataSetChanged();
                        updateCartUI();
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Lỗi xóa: " + message);
                    }
                });
            }
            if (isAdded()) {
                Toast.makeText(getContext(), "Đã xóa " + toDelete.size() + " sản phẩm", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show(getParentFragmentManager(), "ConfirmDeleteDialog");
    }

    private void saveSelectedToFavorites() {
        List<Product> selectedList = new ArrayList<>();
        for (Product p : cartProducts) {
            if (p.isSelected()) {
                selectedList.add(p);
            }
        }
        
        if (selectedList.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng chọn sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalToSave = selectedList.size();
        final int[] savedCount = {0};
        final int[] errorCount = {0};

        for (Product p : selectedList) {
            String productId = p.getId();
            com.project.utils.WishlistManager.getInstance(getContext()).addToWishlist(productId, new com.project.utils.WishlistManager.WishlistCallback<com.project.models.WishlistResponse>() {
                @Override
                public void onSuccess(com.project.models.WishlistResponse result) {
                    savedCount[0]++;
                    checkCompletion();
                }

                @Override
                public void onError(String message) {
                    errorCount[0]++;
                    checkCompletion();
                }

                private void checkCompletion() {
                    if (savedCount[0] + errorCount[0] == totalToSave) {
                        if (isAdded()) {
                            if (savedCount[0] > 0) {
                                Toast.makeText(getContext(), "Đã lưu " + savedCount[0] + " sản phẩm vào Yêu thích", Toast.LENGTH_SHORT).show();
                            }
                            if (errorCount[0] > 0) {
                                Toast.makeText(getContext(), "Lỗi lưu " + errorCount[0] + " sản phẩm", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            });
        }
    }

    private int currentBottomInset = 0;

    private void setupBottomSectionAdjustment(int bottomInset) {
        this.currentBottomInset = bottomInset;
        setupBottomSectionAdjustment();
    }

    private void setupBottomSectionAdjustment() {
        if (getActivity() == null) return;

        BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigationView);
        if (bottomNav == null) {
            binding.bottomContainer.setTranslationY(0);
            return;
        }

        // Đợi view được vẽ xong để lấy chiều cao chính xác
        bottomNav.post(() -> {
            int navHeight = bottomNav.getHeight();
            if (navHeight <= 0) return;

            // Calculate translation for showing bottom container above bottom navigation view
            // accounting for the bottom navigation bar inset padding in bottomContainer.
            int translationY = -(navHeight - currentBottomInset);

            // Mặc định Buy Bar nằm trên Bottom Nav
            binding.bottomContainer.setTranslationY(translationY);

            // Lắng nghe sự kiện cuộn để đồng bộ ẩn/hiện
            binding.nestedScrollView.setOnScrollChangeListener((View.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY > oldScrollY + 10) {
                    // Cuộn xuống: Ẩn Bottom Nav -> Buy Bar trượt xuống đáy màn hình
                    binding.bottomContainer.animate()
                            .translationY(0)
                            .setDuration(200)
                            .start();
                } else if (scrollY < oldScrollY - 10) {
                    // Cuộn lên: Hiện Bottom Nav -> Buy Bar trượt lên trên Bottom Nav
                    binding.bottomContainer.animate()
                            .translationY(translationY)
                            .setDuration(200)
                            .start();
                }
            });
        });
    }

    private void setupCartRecyclerView() {
        binding.rvCartProducts.setLayoutManager(new LinearLayoutManager(getContext()));

        cartAdapter = new HorizontalProductAdapter(
                getContext(),
                HorizontalProductMode.CART,
                new HorizontalProductAdapter.OnProductActionListener() {
                    @Override
                    public void onProductClick(Product product) {
                        Intent intent = new Intent(getContext(), ProductDetailActivity.class);
                        intent.putExtra("product_id", product.getId());
                        startActivity(intent);
                    }

                    @Override

                    public void onDelete(Product product, int position) {
                        String color = "";
                        String size = "";
                        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                            color = product.getVariants().get(0).getColor();
                            size = product.getVariants().get(0).getSize();
                        }
                        CartManager.getInstance(requireContext()).removeFromCart(product.getId(), size, color, new CartManager.CartCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                if (!isAdded() || binding == null) return;
                                cartProducts.remove(position);
                                cartAdapter.notifyItemRemoved(position);
                                cartAdapter.notifyItemRangeChanged(position, cartProducts.size());
                                updateCartUI();
                                Toast.makeText(getContext(), "Đã xóa khỏi giỏ hàng", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String message) {
                                if (!isAdded()) return;
                                Toast.makeText(getContext(), "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onSimilar(Product product, int position) {
                        Toast.makeText(getContext(), "Tìm sản phẩm tương tự", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCheckedChanged(Product product, int position, boolean checked) {
                        updateCheckoutButtonState();
                    }

                    @Override
                    public void onQuantityChanged(Product product, int position, int quantity) {
                        double price = product.getDiscountPrice() != null && product.getDiscountPrice() > 0 
                                ? product.getDiscountPrice() : product.getPrice();
                        String color = "";
                        String size = "";
                        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                            color = product.getVariants().get(0).getColor();
                            size = product.getVariants().get(0).getSize();
                        }
                        CartManager.getInstance(requireContext()).updateQuantity(product.getId(), quantity, price, color, size, new CartManager.CartCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                if (!isAdded() || binding == null) return;
                                updateCheckoutButtonState();
                            }

                            @Override
                            public void onError(String message) {
                                if (!isAdded()) return;
                                Toast.makeText(getContext(), "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onVariantClick(Product product, int position) {
                        // Fetch full product details to show all colors/sizes
                        ApiService service = ApiClient.INSTANCE.getInstance();
                        service.getProductDetail(product.getId()).enqueue(new Callback<Product>() {
                            @Override
                            public void onResponse(Call<Product> call, Response<Product> response) {
                                if (!isAdded() || binding == null) return;
                                if (response.isSuccessful() && response.body() != null) {
                                    Product fullProduct = response.body();
                                    
                                    // Current selected from existing product
                                    String curColor = "";
                                    String curSize = "";
                                    if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                                        curColor = product.getVariants().get(0).getColor();
                                        curSize = product.getVariants().get(0).getSize();
                                    }

                                    ProductVariantBottomSheetFragment variantSheet = new ProductVariantBottomSheetFragment(
                                            fullProduct, curColor, curSize, product.getQuantity());
                                    
                                    variantSheet.setOnVariantSelectedListener((color, size, quantity) -> {
                                        // Update local list
                                        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                                            product.getVariants().get(0).setColor(color);
                                            product.getVariants().get(0).setSize(size);
                                        }
                                        product.setQuantity(quantity);
                                        cartAdapter.notifyItemChanged(position);
                                        
                                        double price = product.getDiscountPrice() != null && product.getDiscountPrice() > 0 
                                                ? product.getDiscountPrice() : product.getPrice();

                                        // Update in Manager (Room/API)
                                        CartManager.getInstance(requireContext()).updateQuantity(product.getId(), quantity, price, color, size, new CartManager.CartCallback<Void>() {
                                            @Override
                                            public void onSuccess(Void result) {
                                                if (!isAdded() || binding == null) return;
                                                updateCheckoutButtonState();
                                            }
                                            @Override
                                            public void onError(String message) {
                                                if (!isAdded()) return;
                                                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    });
                                    variantSheet.show(getParentFragmentManager(), "ProductVariantBottomSheet");
                                }
                            }

                            @Override
                            public void onFailure(Call<Product> call, Throwable t) {
                                if (!isAdded()) return;
                                Toast.makeText(getContext(), "Lỗi tải thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
        );

        binding.rvCartProducts.setAdapter(cartAdapter);
    }

    private void setupSuggestedProductsRecyclerView() {
        binding.rvSuggestedProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));
        productAdapter = new VerticalProductAdapter(getContext(), new VerticalProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(getContext(), ProductDetailActivity.class);
                intent.putExtra("product_id", product.getId());
                startActivity(intent);
            }

            @Override
            public void onFavoriteClick(Product product, int position) {
                Toast.makeText(getContext(), "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvSuggestedProducts.setAdapter(productAdapter);
    }

    private void fetchDataFromServer() {
        if (!isAdded()) return;
        Toast.makeText(getContext(), "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show();

        // Get Cart Items
        CartManager.getInstance(requireContext()).getCartItems(new CartManager.CartCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> result) {
                if (!isAdded() || binding == null) return;
                cartProducts.clear();
                if (result != null) {
                    cartProducts.addAll(result);
                }
                cartAdapter.setData(cartProducts);
                
                com.project.utils.SessionManager sessionManager = new com.project.utils.SessionManager(requireContext());
                if (sessionManager.isLoggedIn() && !cartProducts.isEmpty()) {
                    for (Product p : cartProducts) {
                        p.setSelected(true);
                    }
                    binding.cbSelectAll.setChecked(true);
                    binding.cbSelectAllEdit.setChecked(true);
                }
                
                updateCartUI();
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Cart error: " + message);
            }
        });

        // Get Suggestions
        ApiClient.INSTANCE.getInstance().getProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<Product> products = response.body();
                    List<Product> activeProducts = new ArrayList<>();
                    for (Product p : products) {
                        if (p.getStatus() == null || "active".equalsIgnoreCase(p.getStatus())) {
                            activeProducts.add(p);
                        }
                    }
                    productAdapter.setData(activeProducts);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                Log.e(TAG, "Lỗi fetch suggestions", t);
            }
        });
    }

    private void updateVoucherUI() {
        if (binding == null) return;

        int selectedCount = 0;
        for (Product p : cartProducts) {
            if (p.isSelected()) {
                selectedCount++;
            }
        }

        // Reset visibility
        binding.tvVoucherHint.setVisibility(View.VISIBLE);
        binding.tvVoucherDiscountAmount.setVisibility(View.GONE);
        binding.tvVoucherFreeShipping.setVisibility(View.GONE);

        if (selectedCount > 0 && (selectedVoucherDiscount > 0 || selectedShippingDiscount > 0)) {
            binding.tvVoucherHint.setVisibility(View.GONE);

            if (selectedVoucherDiscount > 0) {
                binding.tvVoucherDiscountAmount.setVisibility(View.VISIBLE);
                binding.tvVoucherDiscountAmount.setText("-" + formatPriceTag(selectedVoucherDiscount));
            }

            if (selectedShippingDiscount > 0) {
                binding.tvVoucherFreeShipping.setVisibility(View.VISIBLE);
            }
        }
    }

    private String formatPriceTag(double price) {
        if (price >= 1000) {
            DecimalFormat df = new DecimalFormat("#.#");
            return df.format(price / 1000) + "kđ";
        }
        return formatPrice(price);
    }

    private void updateCartUI() {
        if (binding == null) return;

        boolean isEmpty = cartProducts.isEmpty();

        // Chuyển đổi trạng thái Trống / Có đồ
        binding.layoutEmptyCart.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvCartProducts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        // Cập nhật số lượng trên Header
        String countText = "(" + cartProducts.size() + ")";
        binding.tvCartCount.setText(countText);

        // Hiện/ẩn summary giá
        binding.layoutPriceSummary.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        // Cập nhật nút Thanh toán (và ẩn/hiện banner Free Shipping)
        updateCheckoutButtonState();
    }

    private void updateCheckoutButtonState() {
        if (binding == null || getContext() == null) return;

        double originalTotal = 0;
        double productDiscount = 0;
        int selectedCount = 0;

        for (Product p : cartProducts) {
            if (p.isSelected()) {
                selectedCount++;
                int qty = p.getQuantity() > 0 ? p.getQuantity() : 1;
                originalTotal += (p.getPrice() * qty);
                if (p.getDiscountPrice() != null && p.getDiscountPrice() > 0) {
                    productDiscount += (p.getPrice() - p.getDiscountPrice()) * qty;
                }
            }
        }

        // totalSavings = voucherDiscount + productDiscount
        // shipping is always free (discount = fee) so it doesn't affect finalTotal here if we only consider product prices
        // unless we add shippingFee to originalTotal first.
        double totalSavings = productDiscount + selectedVoucherDiscount;
        // finalTotal = originalTotal - totalSavings
        double finalTotal = originalTotal - totalSavings;

        // Update Summary UI
        binding.tvTotalPrice.setText(formatPrice(finalTotal));
        binding.tvSavings.setText("Tiết kiệm: " + formatPrice(totalSavings));

        if (selectedCount == 0) {
            binding.btnCheckout.setEnabled(false);
            binding.btnCheckout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.neutral_300)));
            binding.btnCheckout.setText("Mua ngay (0)");
            binding.layoutFreeShipping.setVisibility(View.GONE);
        } else {
            binding.btnCheckout.setEnabled(true);
            binding.btnCheckout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.primary_500)));

            String checkoutText = "Mua ngay (" + selectedCount + ")";
            binding.btnCheckout.setText(checkoutText);
            binding.layoutFreeShipping.setVisibility(View.VISIBLE);
        }
        updateVoucherUI();
    }

    private void showPromotionDetails() {
        double originalTotal = 0;
        double productDiscount = 0;
        int selectedCount = 0;

        for (Product p : cartProducts) {
            if (p.isSelected()) {
                selectedCount++;
                int qty = p.getQuantity() > 0 ? p.getQuantity() : 1;
                originalTotal += (p.getPrice() * qty);
                if (p.getDiscountPrice() != null && p.getDiscountPrice() > 0) {
                    productDiscount += (p.getPrice() - p.getDiscountPrice()) * qty;
                }
            }
        }

        if (selectedCount == 0) {
            Toast.makeText(getContext(), "Vui lòng chọn sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        PromotionDetailsBottomSheetFragment sheet = new PromotionDetailsBottomSheetFragment(
                originalTotal,
                selectedVoucherDiscount,
                productDiscount,
                defaultShippingFee,
                selectedShippingDiscount
        );
        sheet.show(getParentFragmentManager(), "PromotionDetails");
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + " VNĐ";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}