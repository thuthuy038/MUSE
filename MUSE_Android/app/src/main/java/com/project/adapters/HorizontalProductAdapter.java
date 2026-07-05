package com.project.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.models.Product;
import com.project.models.ProductVariant;
import com.project.models.enums.HorizontalProductMode;
import com.project.muse_android.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HorizontalProductAdapter extends RecyclerView.Adapter<HorizontalProductAdapter.ViewHolder> {

    public interface OnProductActionListener {
        void onProductClick(Product product);

        void onDelete(Product product, int position);

        void onSimilar(Product product, int position);

        void onCheckedChanged(Product product, int position, boolean checked);

        void onQuantityChanged(Product product, int position, int quantity);

        void onVariantClick(Product product, int position);
    }

    private final Context context;
    private final HorizontalProductMode mode;
    private final OnProductActionListener listener;

    private final List<Product> products = new ArrayList<>();

    // tạm lưu số lượng khi Product chưa có quantity
    private final HashMap<String, Integer> quantityMap = new HashMap<>();

    public HorizontalProductAdapter(
            Context context,
            HorizontalProductMode mode,
            OnProductActionListener listener
    ) {
        this.context = context;
        this.mode = mode;
        this.listener = listener;
    }

    public void setData(List<Product> data) {
        products.clear();

        if (data != null) {
            products.addAll(data);

            for (Product product : data) {
                if (!quantityMap.containsKey(product.getId())) {
                    quantityMap.put(product.getId(), 1);
                }
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(context)
                .inflate(
                        R.layout.item_product_horizontal,
                        parent,
                        false
                );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        Product product = products.get(position);

        // ====================================
        // RESPONSIVE WIDTH FOR SWIPE
        // ====================================
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        
        // Trừ đi: Fragment padding (10dp * 2) + Item margin (0dp * 2) + Buffer = 24dp
        float density = context.getResources().getDisplayMetrics().density;
        int totalMarginPx = (int) (24 * density);
        int contentWidth = screenWidth - totalMarginPx;

        ViewGroup.LayoutParams params = holder.layoutMainContent.getLayoutParams();
        params.width = contentWidth;
        holder.layoutMainContent.setLayoutParams(params);

        // Reset scroll position
        holder.horizontalScrollView.scrollTo(0, 0);

        // Snap behavior for swipe reveal
        holder.horizontalScrollView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                int scrollX = holder.horizontalScrollView.getScrollX();
                int actionsWidth = holder.layoutActions.getWidth();
                
                if (scrollX > actionsWidth / 3) { // Chỉ cần vuốt 1/3 là snap để dễ mở hơn
                    holder.horizontalScrollView.smoothScrollTo(actionsWidth, 0);
                } else {
                    holder.horizontalScrollView.smoothScrollTo(0, 0);
                }
                v.performClick();
                return true;
            }
            return false;
        });

        holder.txtName.setText(product.getName());

        /*
         * IMAGE
         */

        if (product.getImages() != null
                && !product.getImages().isEmpty()) {

            Glide.with(context)
                    .load(product.getImages().get(0).getUrl())
                    .placeholder(R.drawable.demo_product)
                    .into(holder.imgProduct);
        }

        /*
         * VARIANT DEFAULT
         */

        String color = "";
        String size = "";

        if (product.getVariants() != null
                && !product.getVariants().isEmpty()) {

            ProductVariant variant =
                    product.getVariants().get(0);

            color = variant.getColor();
            size = variant.getSize();
        }

        int quantity =
                quantityMap.getOrDefault(
                        product.getId(),
                        1
                );

        /*
         * MODE
         */

        if (mode == HorizontalProductMode.CART) {

            holder.cbSelect.setVisibility(View.VISIBLE);

            holder.layoutVariant.setVisibility(View.VISIBLE);
            holder.layoutQuantityEditor.setVisibility(View.VISIBLE);
            holder.txtCartPrice.setVisibility(View.VISIBLE);

            holder.layoutReadOnly.setVisibility(View.GONE);

            holder.txtVariant.setText(
                    color + " | " + size
            );

            double price =
                    product.getDiscountPrice() > 0
                            ? product.getDiscountPrice()
                            : product.getPrice();

            holder.txtCartPrice.setText(
                    formatPrice(price)
            );

            holder.txtQuantity.setText(
                    String.valueOf(quantity)
            );

        } else {

            holder.cbSelect.setVisibility(View.GONE);

            holder.layoutVariant.setVisibility(View.GONE);
            holder.layoutQuantityEditor.setVisibility(View.GONE);
            holder.txtCartPrice.setVisibility(View.GONE);

            holder.layoutReadOnly.setVisibility(View.VISIBLE);

            holder.txtVariantReadOnly.setText(
                    color + ", " + size
            );

            holder.txtDiscountPriceReadOnly.setText(
                    formatPrice(
                            product.getDiscountPrice()
                    )
            );

            holder.txtOriginalPriceReadOnly.setText(
                    formatPrice(
                            product.getPrice()
                    )
            );

            holder.txtOriginalPriceReadOnly.setPaintFlags(
                    Paint.STRIKE_THRU_TEXT_FLAG
            );

            holder.txtReadonlyQuantity.setText(
                    "x" + quantity
            );
        }

        /*
         * CLICK ITEM
         */

        holder.layoutMainContent.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });

        holder.layoutVariant.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVariantClick(product, holder.getAdapterPosition());
            }
        });

        /*
         * DELETE
         */

        holder.layoutDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(
                        product,
                        holder.getAdapterPosition()
                );
            }
        });

        /*
         * SIMILAR
         */

        holder.layoutSimilar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSimilar(
                        product,
                        holder.getAdapterPosition()
                );
            }
        });

        /*
         * CHECKBOX
         */

        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(product.isSelected());

        holder.cbSelect.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    product.setSelected(isChecked);
                    if (listener != null) {
                        listener.onCheckedChanged(
                                product,
                                holder.getAdapterPosition(),
                                isChecked
                        );
                    }
                }
        );

        /*
         * ADD QUANTITY
         */

        holder.btnAdd.setOnClickListener(v -> {

            int currentQuantity =
                    quantityMap.getOrDefault(
                            product.getId(),
                            1
                    );

            currentQuantity++;

            quantityMap.put(
                    product.getId(),
                    currentQuantity
            );

            holder.txtQuantity.setText(
                    String.valueOf(currentQuantity)
            );

            if (listener != null) {
                listener.onQuantityChanged(
                        product,
                        holder.getAdapterPosition(),
                        currentQuantity
                );
            }
        });

        /*
         * MINUS QUANTITY
         */

        holder.btnMinus.setOnClickListener(v -> {

            int currentQuantity =
                    quantityMap.getOrDefault(
                            product.getId(),
                            1
                    );

            if (currentQuantity > 1) {
                currentQuantity--;

                quantityMap.put(
                        product.getId(),
                        currentQuantity
                );

                holder.txtQuantity.setText(
                        String.valueOf(currentQuantity)
                );

                if (listener != null) {
                    listener.onQuantityChanged(
                            product,
                            holder.getAdapterPosition(),
                            currentQuantity
                    );
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    private String formatPrice(double price) {
        DecimalFormat formatter =
                new DecimalFormat("#,###");

        return formatter
                .format(price)
                .replace(",", ".") + "đ";
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgProduct;

        TextView txtName;

        /*
         * CART
         */

        CheckBox cbSelect;

        LinearLayout layoutVariant;
        LinearLayout layoutQuantityEditor;

        TextView txtVariant;
        TextView txtCartPrice;
        TextView txtQuantity;

        ImageView btnMinus;
        ImageView btnAdd;

        /*
         * READ ONLY
         */

        LinearLayout layoutReadOnly;

        TextView txtVariantReadOnly;
        TextView txtDiscountPriceReadOnly;
        TextView txtOriginalPriceReadOnly;
        TextView txtReadonlyQuantity;

        /*
         * ACTIONS & SWIPE
         */

        HorizontalScrollView horizontalScrollView;
        View layoutMainContent;
        View layoutActions;
        LinearLayout layoutDelete;
        LinearLayout layoutSimilar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            horizontalScrollView = itemView.findViewById(R.id.horizontalScrollView);
            layoutMainContent = itemView.findViewById(R.id.layoutMainContent);
            layoutActions = itemView.findViewById(R.id.layoutActions);

            imgProduct = itemView.findViewById(R.id.imgProduct);

            txtName = itemView.findViewById(R.id.txtName);

            cbSelect = itemView.findViewById(R.id.cbSelect);

            layoutVariant = itemView.findViewById(R.id.layoutVariant);
            layoutQuantityEditor = itemView.findViewById(R.id.layoutQuantityEditor);

            txtVariant = itemView.findViewById(R.id.txtVariant);
            txtCartPrice = itemView.findViewById(R.id.txtCartPrice);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);

            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnAdd = itemView.findViewById(R.id.btnAdd);

            layoutReadOnly = itemView.findViewById(R.id.layoutReadOnly);

            txtVariantReadOnly =
                    itemView.findViewById(R.id.txtVariantReadOnly);

            txtDiscountPriceReadOnly =
                    itemView.findViewById(R.id.txtDiscountPriceReadOnly);

            txtOriginalPriceReadOnly =
                    itemView.findViewById(R.id.txtOriginalPriceReadOnly);

            txtReadonlyQuantity =
                    itemView.findViewById(R.id.txtReadonlyQuantity);

            layoutDelete =
                    itemView.findViewById(R.id.layoutDelete);

            layoutSimilar =
                    itemView.findViewById(R.id.layoutSimilar);
        }
    }
}