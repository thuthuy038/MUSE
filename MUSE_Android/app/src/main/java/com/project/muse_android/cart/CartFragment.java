package com.project.muse_android.cart;

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
import com.project.network.ApiClient;
import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentCartBinding;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
                        // Xem chi tiết
                    }

                    @Override
                    public void onDelete(Product product, int position) {
                        cartProducts.remove(position);
                        cartAdapter.notifyItemRemoved(position);
                        cartAdapter.notifyItemRangeChanged(position, cartProducts.size());
                        updateCartUI();
                        Toast.makeText(getContext(), "Đã xóa khỏi giỏ hàng", Toast.LENGTH_SHORT).show();
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
                        updateCheckoutButtonState();
                    }

                    @Override
                    public void onVariantClick(Product product, int position) {
                        ProductVariantBottomSheetFragment variantSheet = new ProductVariantBottomSheetFragment(product);
                        variantSheet.show(getParentFragmentManager(), "ProductVariantBottomSheet");
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
                Toast.makeText(getContext(), "Chi tiết: " + product.getName(), Toast.LENGTH_SHORT).show();
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

        ApiClient.INSTANCE.getInstance().getProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Product> products = response.body();
                    
                    // Giả lập: Lấy 3 sản phẩm đầu tiên vào giỏ hàng
                    if (products.size() >= 3) {
                        cartProducts.clear();
                        cartProducts.add(products.get(0));
                        cartProducts.add(products.get(1));
                        cartProducts.add(products.get(2));
                        cartAdapter.setData(cartProducts);
                    }

                    // Toàn bộ danh sách gợi ý
                    productAdapter.setData(products);
                    
                    updateCartUI();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                Log.e(TAG, "Lỗi fetch data", t);
                updateCartUI();
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
                if (p.getDiscountPrice() > 0) {
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
                if (p.getDiscountPrice() > 0) {
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
