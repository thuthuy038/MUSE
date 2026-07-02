package com.project.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.models.Product;
import com.project.models.enums.HorizontalProductMode;
import com.project.muse_android.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HorizontalProductAdapter
        extends RecyclerView.Adapter<HorizontalProductAdapter.ProductViewHolder> {

    private final Context context;
    private final HorizontalProductMode mode;

    private List<Product> productList = new ArrayList<>();

    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);

        void onIncreaseQuantity(Product product, int position);

        void onDecreaseQuantity(Product product, int position);

        void onVariantClick(Product product, int position);
    }

    public HorizontalProductAdapter(
            Context context,
            HorizontalProductMode mode
    ) {
        this.context = context;
        this.mode = mode;
    }

    public HorizontalProductAdapter(
            Context context,
            HorizontalProductMode mode,
            OnProductClickListener listener
    ) {
        this.context = context;
        this.mode = mode;
        this.listener = listener;
    }

    public void setData(List<Product> list) {
        productList = list != null
                ? list
                : new ArrayList<>();

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(context)
                .inflate(
                        R.layout.item_product_horizontal,
                        parent,
                        false
                );

        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ProductViewHolder holder,
            int position
    ) {

        Product product = productList.get(position);

        if (product == null) return;

        holder.txtName.setText(product.getName());

        NumberFormat currency =
                NumberFormat.getCurrencyInstance(
                        new Locale("vi", "VN")
                );

        // =========================
        // HÌNH ẢNH
        // =========================

        if (product.getImages() != null
                && !product.getImages().isEmpty()) {

            String imageUrl =
                    product.getImages()
                            .get(0)
                            .getUrl();

            if (imageUrl != null
                    && imageUrl.startsWith("/")) {

                imageUrl =
                        "https://server-testing-ymn9.onrender.com"
                                + imageUrl;
            }

            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.demo_product)
                    .error(R.drawable.demo_product)
                    .into(holder.imgProduct);

        } else {

            holder.imgProduct.setImageResource(
                    R.drawable.demo_product
            );
        }

        // ====================================
        // DỮ LIỆU TẠM
        // Sau này thay bằng CartItem
        // ====================================

        String color = "Hồng";
        String size = "S";
        int quantity = 1;

        // ====================================
        // CART MODE
        // ====================================

        holder.txtVariant.setText(
                color + " | " + size
        );

        holder.txtQuantity.setText(
                String.valueOf(quantity)
        );

        holder.txtCartPrice.setText(
                currency.format(
                        product.getPrice()
                )
        );

        // ====================================
        // READ ONLY MODE
        // ====================================

        holder.txtVariantReadOnly.setText(
                color + ", " + size
        );

        holder.txtReadonlyQuantity.setText(
                "x" + quantity
        );

        if (product.getDiscountPercent() > 0) {

            holder.txtDiscountPriceReadOnly.setText(
                    currency.format(
                            product.getDiscountPrice()
                    )
            );

            holder.txtOriginalPriceReadOnly.setVisibility(
                    View.VISIBLE
            );

            holder.txtOriginalPriceReadOnly.setText(
                    currency.format(
                            product.getPrice()
                    )
            );

        } else {

            holder.txtDiscountPriceReadOnly.setText(
                    currency.format(
                            product.getPrice()
                    )
            );

            holder.txtOriginalPriceReadOnly.setVisibility(
                    View.GONE
            );
        }

        // ====================================
        // HIỂN THỊ THEO MODE
        // ====================================

        if (mode == HorizontalProductMode.CART) {

            holder.layoutVariant.setVisibility(
                    View.VISIBLE
            );

            holder.layoutQuantityEditor.setVisibility(
                    View.VISIBLE
            );

            holder.layoutReadOnly.setVisibility(
                    View.GONE
            );

        } else {

            holder.layoutVariant.setVisibility(
                    View.GONE
            );

            holder.layoutQuantityEditor.setVisibility(
                    View.GONE
            );

            holder.layoutReadOnly.setVisibility(
                    View.VISIBLE
            );
        }

        // ====================================
        // EVENTS
        // ====================================

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });

        holder.btnAdd.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIncreaseQuantity(
                        product,
                        position
                );
            }
        });

        holder.btnMinus.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDecreaseQuantity(
                        product,
                        position
                );
            }
        });

        holder.layoutVariant.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVariantClick(
                        product,
                        position
                );
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder
            extends RecyclerView.ViewHolder {

        ImageView imgProduct;

        ImageView btnAdd;
        ImageView btnMinus;

        TextView txtName;

        // CART
        LinearLayout layoutVariant;
        LinearLayout layoutQuantityEditor;

        TextView txtVariant;
        TextView txtQuantity;
        TextView txtCartPrice;

        // READ ONLY
        LinearLayout layoutReadOnly;

        TextView txtVariantReadOnly;
        TextView txtDiscountPriceReadOnly;
        TextView txtOriginalPriceReadOnly;
        TextView txtReadonlyQuantity;

        public ProductViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            imgProduct =
                    itemView.findViewById(
                            R.id.imgProduct
                    );

            txtName =
                    itemView.findViewById(
                            R.id.txtName
                    );

            txtCartPrice =
                    itemView.findViewById(
                            R.id.txtCartPrice
                    );

            // CART
            layoutVariant =
                    itemView.findViewById(
                            R.id.layoutVariant
                    );

            layoutQuantityEditor =
                    itemView.findViewById(
                            R.id.layoutQuantityEditor
                    );

            txtVariant =
                    itemView.findViewById(
                            R.id.txtVariant
                    );

            txtQuantity =
                    itemView.findViewById(
                            R.id.txtQuantity
                    );

            btnAdd =
                    itemView.findViewById(
                            R.id.btnAdd
                    );

            btnMinus =
                    itemView.findViewById(
                            R.id.btnMinus
                    );

            // READ ONLY
            layoutReadOnly =
                    itemView.findViewById(
                            R.id.layoutReadOnly
                    );

            txtVariantReadOnly =
                    itemView.findViewById(
                            R.id.txtVariantReadOnly
                    );

            txtDiscountPriceReadOnly =
                    itemView.findViewById(
                            R.id.txtDiscountPriceReadOnly
                    );

            txtOriginalPriceReadOnly =
                    itemView.findViewById(
                            R.id.txtOriginalPriceReadOnly
                    );

            txtReadonlyQuantity =
                    itemView.findViewById(
                            R.id.txtReadonlyQuantity
                    );

            txtOriginalPriceReadOnly.setPaintFlags(
                    txtOriginalPriceReadOnly.getPaintFlags()
                            | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }
    }
}