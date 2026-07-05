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
import com.project.network.ApiClient;
import com.project.network.ApiService;
import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentCartBinding;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

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

        // Cấu hình RecyclerView cho giỏ hàng
        setupCartRecyclerView();

        // Cấu hình RecyclerView gợi ý sản phẩm
        setupSuggestedProductsRecyclerView();

        // Gọi API lấy dữ liệu sản phẩm
        fetchDataFromServer();

        // Điều chỉnh Buy Bar theo Bottom Navigation
        setupBottomSectionAdjustment();

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
            VoucherBottomSheetFragment voucherSheet = new VoucherBottomSheetFragment();
            voucherSheet.show(getParentFragmentManager(), "VoucherBottomSheet");
        });

        // Mở BottomSheet chi tiết khuyến mãi
        binding.layoutPriceSummary.setOnClickListener(v -> {
            showPromotionDetails();
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

        for (Product p : toDelete) {
            CartManager.getInstance(requireContext()).removeFromCart(p.getId(), new CartManager.CartCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
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
        Toast.makeText(getContext(), "Đã xóa " + toDelete.size() + " sản phẩm", Toast.LENGTH_SHORT).show();
    }

    private void saveSelectedToFavorites() {
        int count = 0;
        for (Product p : cartProducts) {
            if (p.isSelected()) {
                count++;
            }
        }
        if (count > 0) {
            Toast.makeText(getContext(), "Đã thêm " + count + " sản phẩm vào yêu thích", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Vui lòng chọn sản phẩm", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBottomSectionAdjustment() {
        if (getActivity() == null) return;

        BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigationView);
        if (bottomNav == null) return;

        // Đợi view được vẽ xong để lấy chiều cao chính xác
        bottomNav.post(() -> {
            int navHeight = bottomNav.getHeight();
            if (navHeight <= 0) return;

            // Mặc định Buy Bar nằm trên Bottom Nav
            binding.bottomContainer.setTranslationY(-navHeight);

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
                            .translationY(-navHeight)
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
                        CartManager.getInstance(requireContext()).removeFromCart(product.getId(), new CartManager.CartCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                cartProducts.remove(position);
                                cartAdapter.notifyItemRemoved(position);
                                cartAdapter.notifyItemRangeChanged(position, cartProducts.size());
                                updateCartUI();
                                Toast.makeText(getContext(), "Đã xóa khỏi giỏ hàng", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String message) {
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
                        CartManager.getInstance(requireContext()).updateQuantity(product.getId(), quantity, new CartManager.CartCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                updateCheckoutButtonState();
                            }

                            @Override
                            public void onError(String message) {
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
                                        
                                        // Update in Manager (Room/API)
                                        CartManager.getInstance(requireContext()).updateQuantity(product.getId(), quantity, new CartManager.CartCallback<Void>() {
                                            @Override
                                            public void onSuccess(Void result) {
                                                updateCheckoutButtonState();
                                            }
                                            @Override
                                            public void onError(String message) {
                                                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    });
                                    variantSheet.show(getParentFragmentManager(), "ProductVariantBottomSheet");
                                }
                            }

                            @Override
                            public void onFailure(Call<Product> call, Throwable t) {
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
        if (isAdded()) {
            Toast.makeText(getContext(), "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show();
        }

        // Get Cart Items
        CartManager.getInstance(requireContext()).getCartItems(new CartManager.CartCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> result) {
                cartProducts.clear();
                if (result != null) {
                    cartProducts.addAll(result);
                }
                cartAdapter.setData(cartProducts);
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
                if (response.isSuccessful() && response.body() != null) {
                    List<Product> products = response.body();
                    productAdapter.setData(products);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                Log.e(TAG, "Lỗi fetch suggestions", t);
            }
        });
    }

    private void updateCartUI() {
        if (binding == null) return;

        boolean isEmpty = cartProducts.isEmpty();

        // Chuyển đổi trạng thái Trống / Có đồ
        binding.layoutEmptyCart.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvCartProducts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        // Banner Free Shipping chỉ hiện khi có đồ
        binding.layoutFreeShipping.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        // Cập nhật số lượng trên Header
        String countText = "(" + cartProducts.size() + ")";
        binding.tvCartCount.setText(countText);

        // Hiện/ẩn summary giá
        binding.layoutPriceSummary.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        // Cập nhật nút Thanh toán
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
                originalTotal += p.getPrice();
                if (p.getDiscountPrice() != null && p.getDiscountPrice() > 0) {
                    productDiscount += (p.getPrice() - p.getDiscountPrice());
                }
            }
        }

        // Fake voucher and shipping for UI demo
        double voucherDiscount = selectedCount > 0 ? 10000 : 0;
        double totalSavings = productDiscount + voucherDiscount;
        double finalTotal = originalTotal - totalSavings;

        // Update Summary UI
        binding.tvTotalPrice.setText(formatPrice(finalTotal));
        binding.tvSavings.setText("Tiết kiệm: " + formatPrice(totalSavings));

        if (selectedCount == 0) {
            binding.btnCheckout.setEnabled(false);
            binding.btnCheckout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.neutral_300)));
            binding.btnCheckout.setText("Mua ngay (0)");
        } else {
            binding.btnCheckout.setEnabled(true);
            binding.btnCheckout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.primary_500)));

            String checkoutText = "Mua ngay (" + selectedCount + ")";
            binding.btnCheckout.setText(checkoutText);
        }
    }

    private void showPromotionDetails() {
        double originalTotal = 0;
        double productDiscount = 0;
        int selectedCount = 0;

        for (Product p : cartProducts) {
            if (p.isSelected()) {
                selectedCount++;
                originalTotal += p.getPrice();
                if (p.getDiscountPrice() != null && p.getDiscountPrice() > 0) {
                    productDiscount += (p.getPrice() - p.getDiscountPrice());
                }
            }
        }

        if (selectedCount == 0) {
            Toast.makeText(getContext(), "Vui lòng chọn sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        // Match the logic in updateCheckoutButtonState
        double voucherDiscount = 10000;
        double shippingFee = 50000;
        double shippingDiscount = 50000;

        PromotionDetailsBottomSheetFragment sheet = new PromotionDetailsBottomSheetFragment(
                originalTotal,
                voucherDiscount,
                productDiscount,
                shippingFee,
                shippingDiscount
        );
        sheet.show(getParentFragmentManager(), "PromotionDetails");
    }

    private String formatPrice(double price) {

        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(price).replace(",", ".") + "đ";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}