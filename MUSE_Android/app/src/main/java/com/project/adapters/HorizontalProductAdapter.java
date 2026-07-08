package com.project.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

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

            int quantity = product.getQuantity() > 0 ? product.getQuantity() : 1;
            quantityMap.put(product.getId(), quantity);

            if (mode == HorizontalProductMode.CART) {
                binding.cbSelect.setVisibility(View.VISIBLE);
                binding.layoutVariant.setVisibility(View.VISIBLE);
                binding.layoutQuantityEditor.setVisibility(View.VISIBLE);
                binding.txtCartPrice.setVisibility(View.VISIBLE);
                binding.layoutReadOnly.setVisibility(View.GONE);
                binding.layoutActions.setVisibility(View.VISIBLE);

                binding.txtProductSizes.setText(color + " | " + size);
                double price = product.getDiscountPrice() != null && product.getDiscountPrice() > 0 ? product.getDiscountPrice() : product.getPrice();
                binding.txtPrice.setText(formatPrice(price));
                binding.txtQuantity.setText(String.valueOf(quantity));

                binding.imgSimilarIcon.setVisibility(View.GONE);
                binding.txtSimilar.setVisibility(View.VISIBLE);
                binding.txtSimilar.setText("Sản phẩm\ntương tự");

                // Snap behavior for CART mode
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
            } else if (mode == com.project.models.enums.HorizontalProductMode.SUGGEST) {
                binding.cbSelect.setVisibility(View.GONE);
                binding.layoutVariant.setVisibility(View.GONE);
                binding.layoutQuantityEditor.setVisibility(View.GONE);
                binding.txtPrice.setVisibility(View.GONE);
                binding.layoutReadOnly.setVisibility(View.VISIBLE);
                binding.layoutActions.setVisibility(View.GONE);

                binding.txtVariantReadOnly.setText(color + ", " + size);
                binding.txtDiscountPriceReadOnly.setText(formatPrice(product.getDiscountPrice() != null ? product.getDiscountPrice() : 0));
                binding.txtOriginalPrice.setText(formatPrice(product.getPrice()));
                binding.txtOriginalPrice.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                binding.txtReadonlyQuantity.setVisibility(View.GONE);

                // Disable scroll for SUGGEST mode (static item)
                binding.horizontalScrollView.setOnTouchListener((v, event) -> true);
            } else {
                binding.cbSelect.setVisibility(View.GONE);
                binding.layoutVariant.setVisibility(View.GONE);
                binding.layoutQuantityEditor.setVisibility(View.GONE);
                binding.txtPrice.setVisibility(View.GONE);
                binding.layoutReadOnly.setVisibility(View.VISIBLE);
                binding.layoutActions.setVisibility(View.GONE);

                binding.txtVariantReadOnly.setText(color + ", " + size);
                double discPrice = (product.getDiscountPrice() != null && product.getDiscountPrice() > 0) ? product.getDiscountPrice() : product.getPrice();
                binding.txtDiscountPriceReadOnly.setText(formatPrice(discPrice));
                if (product.getDiscountPrice() != null && product.getDiscountPrice() > 0 && product.getDiscountPrice() < product.getPrice()) {
                    binding.txtOriginalPrice.setVisibility(View.VISIBLE);
                    binding.txtOriginalPrice.setText(formatPrice(product.getPrice()));
                    binding.txtOriginalPrice.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    binding.txtOriginalPrice.setVisibility(View.GONE);
                }
                binding.txtReadonlyQuantity.setText("x" + quantity);

                // Disable scroll for READ_ONLY mode
                binding.horizontalScrollView.setOnTouchListener((v, event) -> true);
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
                int q = product.getQuantity() + 1;
                product.setQuantity(q);
                quantityMap.put(product.getId(), q);
                binding.txtQuantity.setText(String.valueOf(q));
                if (actionListener != null) actionListener.onQuantityChanged(product, getBindingAdapterPosition(), q);
            });

            binding.btnMinus.setOnClickListener(v -> {
                int q = product.getQuantity();
                if (q > 1) {
                    q--;
                    product.setQuantity(q);
                    quantityMap.put(product.getId(), q);
                    binding.txtQuantity.setText(String.valueOf(q));
                    if (actionListener != null) actionListener.onQuantityChanged(product, getBindingAdapterPosition(), q);
                }
            });
        }

        private void setupSwipeWidth() {
            // Get screen metrics
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            float density = context.getResources().getDisplayMetrics().density;

            if (mode == HorizontalProductMode.READ_ONLY) {
                // Completely disable any programmatic width/height for READ_ONLY
                // This lets the ConstraintLayout use wrap_content from XML
                binding.horizontalScrollView.setOnTouchListener(null);
                
                ViewGroup.LayoutParams params = binding.layoutMainContent.getLayoutParams();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                //params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.height = (int) (110 * context.getResources().getDisplayMetrics().density);
                binding.layoutMainContent.setLayoutParams(params);

                View parentLinear = (View) binding.layoutMainContent.getParent();
                if (parentLinear != null) {
                    parentLinear.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
                    parentLinear.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                return;
            }

            // For CART & SUGGEST modes, set width to screen - margin
            int margin = (mode == com.project.models.enums.HorizontalProductMode.SUGGEST) ? 80 : 24;
            int totalMarginPx = (int) (margin * density); 
            int contentWidth = screenWidth - totalMarginPx;

            // Adjust layoutActions width dynamically: 90dp for SUGGEST (delete is hidden), 160dp for CART (both shown)
            int actionsWidthDp = (mode == com.project.models.enums.HorizontalProductMode.SUGGEST) ? 90 : 160;
            binding.layoutActions.getLayoutParams().width = (int) (actionsWidthDp * density);

            binding.layoutMainContent.getLayoutParams().width = contentWidth;
            binding.layoutMainContent.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;

            View parentLinear = (View) binding.layoutMainContent.getParent();
            if (parentLinear != null) {
                parentLinear.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
                parentLinear.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            }
            
            binding.layoutMainContent.requestLayout();
        }
    }
}
