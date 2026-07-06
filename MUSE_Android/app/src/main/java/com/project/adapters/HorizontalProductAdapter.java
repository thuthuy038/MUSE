package com.project.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.models.Product;
import com.project.models.ProductVariant;
import com.project.models.enums.HorizontalProductMode;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ItemProductHorizontalBinding;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class HorizontalProductAdapter extends ProductAdapter {

    public interface OnProductActionListener extends OnProductClickListener {
        void onDelete(Product product, int position);
        void onSimilar(Product product, int position);
        void onCheckedChanged(Product product, int position, boolean checked);
        void onQuantityChanged(Product product, int position, int quantity);
        void onVariantClick(Product product, int position);
    }

    private final Context context;
    private final HorizontalProductMode mode;
    private final OnProductActionListener actionListener;
    private final HashMap<String, Integer> quantityMap = new HashMap<>();

    public HorizontalProductAdapter(
            Context context,
            HorizontalProductMode mode,
            OnProductActionListener listener
    ) {
        super(new ArrayList<>(), TYPE_HORIZONTAL, listener);
        this.context = context;
        this.mode = mode;
        this.actionListener = listener;
    }

    @Override
    public void setData(List<Product> data) {
        if (data != null) {
            for (Product product : data) {
                if (!quantityMap.containsKey(product.getId())) {
                    quantityMap.put(product.getId(), 1);
                }
            }
        }
        super.setData(data);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductHorizontalBinding binding = ItemProductHorizontalBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CustomHorizontalViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CustomHorizontalViewHolder) {
            ((CustomHorizontalViewHolder) holder).bindCustom(products.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + " VNĐ";
    }

    public class CustomHorizontalViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductHorizontalBinding binding;

        public CustomHorizontalViewHolder(ItemProductHorizontalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bindCustom(Product product) {
            // Setup Swipe width
            setupSwipeWidth();

            // Reset scroll position
            binding.horizontalScrollView.scrollTo(0, 0);

            // Snap behavior
            binding.horizontalScrollView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    int scrollX = binding.horizontalScrollView.getScrollX();
                    int actionsWidth = binding.layoutActions.getWidth();
                    if (scrollX > actionsWidth / 3) {
                        binding.horizontalScrollView.smoothScrollTo(actionsWidth, 0);
                    } else {
                        binding.horizontalScrollView.smoothScrollTo(0, 0);
                    }
                    v.performClick();
                    return true;
                }
                return false;
            });

            binding.txtProductName.setText(product.getName());

            // Load Image
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                String imageUrl = product.getImages().get(0).getUrl();
                if (imageUrl != null && !imageUrl.startsWith("http")) {
                    imageUrl = "https://server-testing-ymn9.onrender.com" + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
                }
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.demo_product)
                        .error(R.drawable.demo_product)
                        .into(binding.imgProduct);
            }

            // Variants
            String color = "";
            String size = "";
            if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                ProductVariant variant = product.getVariants().get(0);
                color = variant.getColor();
                size = variant.getSize();
            }

            int quantity = product.getQuantity() > 0 ? product.getQuantity() : quantityMap.getOrDefault(product.getId(), 1);

            if (mode == HorizontalProductMode.CART) {
                binding.cbSelect.setVisibility(View.VISIBLE);
                binding.layoutVariant.setVisibility(View.VISIBLE);
                binding.layoutQuantityEditor.setVisibility(View.VISIBLE);
                binding.txtCartPrice.setVisibility(View.VISIBLE);
                binding.layoutReadOnly.setVisibility(View.GONE);

                binding.txtProductSizes.setText(color + " | " + size);
                double price = product.getDiscountPrice() != null && product.getDiscountPrice() > 0 ? product.getDiscountPrice() : product.getPrice();
                binding.txtPrice.setText(formatPrice(price));
                binding.txtQuantity.setText(String.valueOf(quantity));
            } else {
                binding.cbSelect.setVisibility(View.GONE);
                binding.layoutVariant.setVisibility(View.GONE);
                binding.layoutQuantityEditor.setVisibility(View.GONE);
                binding.txtPrice.setVisibility(View.GONE);
                binding.layoutReadOnly.setVisibility(View.VISIBLE);

                binding.txtVariantReadOnly.setText(color + ", " + size);
                binding.txtDiscountPriceReadOnly.setText(formatPrice(product.getDiscountPrice() != null ? product.getDiscountPrice() : 0));
                binding.txtOriginalPrice.setText(formatPrice(product.getPrice()));
                binding.txtOriginalPrice.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                binding.txtReadonlyQuantity.setText("x" + quantity);
            }

            // Click Listeners
            binding.layoutMainContent.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onProductClick(product);
            });

            binding.layoutVariant.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onVariantClick(product, getBindingAdapterPosition());
            });

            binding.layoutDelete.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDelete(product, getBindingAdapterPosition());
            });

            binding.layoutSimilar.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onSimilar(product, getBindingAdapterPosition());
            });

            // Checkbox
            binding.cbSelect.setOnCheckedChangeListener(null);
            binding.cbSelect.setChecked(product.isSelected());
            binding.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                product.setSelected(isChecked);
                if (actionListener != null) actionListener.onCheckedChanged(product, getBindingAdapterPosition(), isChecked);
            });

            // Quantity
            binding.btnAdd.setOnClickListener(v -> {
                int q = quantityMap.getOrDefault(product.getId(), 1) + 1;
                quantityMap.put(product.getId(), q);
                binding.txtQuantity.setText(String.valueOf(q));
                if (actionListener != null) actionListener.onQuantityChanged(product, getBindingAdapterPosition(), q);
            });

            binding.btnMinus.setOnClickListener(v -> {
                int q = quantityMap.getOrDefault(product.getId(), 1);
                if (q > 1) {
                    q--;
                    quantityMap.put(product.getId(), q);
                    binding.txtQuantity.setText(String.valueOf(q));
                    if (actionListener != null) actionListener.onQuantityChanged(product, getBindingAdapterPosition(), q);
                }
            });
        }

        private void setupSwipeWidth() {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            float density = context.getResources().getDisplayMetrics().density;
            int totalMarginPx = (int) (24 * density);
            int contentWidth = screenWidth - totalMarginPx;

            ViewGroup.LayoutParams params = binding.layoutMainContent.getLayoutParams();
            params.width = contentWidth;
            binding.layoutMainContent.setLayoutParams(params);
        }
    }
}
