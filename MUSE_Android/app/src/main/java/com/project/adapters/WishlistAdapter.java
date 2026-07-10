package com.project.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.models.Product;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ItemWishlistProductBinding;

import java.util.ArrayList;
import java.util.List;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder> {

    private List<Product> products;
    private boolean isEditMode = false;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onFavoriteClick(Product product, int position);
        void onAddToCartClick(Product product);
        void onFindSimilarClick(Product product);
        void onSelectionChanged();
    }

    public WishlistAdapter(List<Product> products, OnProductClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
        notifyDataSetChanged();
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public void updateList(List<Product> newList) {
        this.products = newList;
        notifyDataSetChanged();
    }

    public List<Product> getSelectedProducts() {
        List<Product> selected = new ArrayList<>();
        for (Product p : products) {
            if (p.isSelected()) {
                selected.add(p);
            }
        }
        return selected;
    }

    public void selectAll(boolean select) {
        for (Product p : products) {
            p.setSelected(select);
        }
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged();
    }

    @NonNull
    @Override
    public WishlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWishlistProductBinding binding = ItemWishlistProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new WishlistViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull WishlistViewHolder holder, int position) {
        holder.bind(products.get(position));
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    private void setupColors(LinearLayout layout, List<String> colors) {
        layout.removeAllViews();
        if (colors == null || colors.isEmpty()) {
            return;
        }

        float density = layout.getContext().getResources().getDisplayMetrics().density;
        int dotSize = (int) (18 * density);
        int margin = (int) (4 * density);

        for (String colorName : colors) {
            View dot = new View(layout.getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSize, dotSize);
            params.setMargins(0, 0, margin, 0);
            dot.setLayoutParams(params);

            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            
            String colorHex = getColorHex(colorName);
            drawable.setColor(android.graphics.Color.parseColor(colorHex));
            
            if (colorHex.equalsIgnoreCase("#FFFFFF")) {
                drawable.setStroke((int) (1 * density), android.graphics.Color.parseColor("#CCCCCC"));
            }
            
            dot.setBackground(drawable);
            layout.addView(dot);
        }
    }

    private String getColorHex(String colorName) {
        if (colorName == null) return "#E0E0E0";
        colorName = colorName.toLowerCase().trim();
        
        if (colorName.contains("trắng") || colorName.contains("white")) return "#FFFFFF";
        if (colorName.contains("đen") || colorName.contains("black")) return "#000000";
        if (colorName.contains("xám") || colorName.contains("gray") || colorName.contains("grey") || colorName.contains("ghi")) return "#808080";
        if (colorName.contains("bạc") || colorName.contains("silver")) return "#C0C0C0";
        if (colorName.contains("vàng đồng") || colorName.contains("gold")) return "#D4AF37";
        if (colorName.contains("navy") || colorName.contains("xanh than") || colorName.contains("than")) return "#000080";
        if (colorName.contains("sky") || colorName.contains("da trời")) return "#87CEEB";
        if (colorName.contains("xanh dương") || colorName.contains("blue") || colorName.equals("xanh")) return "#0000FF";
        if (colorName.contains("cobalt") || colorName.contains("xanh coban")) return "#0047AB";
        if (colorName.contains("đỏ đô") || colorName.contains("burgundy") || colorName.contains("đỏ rượu")) return "#800000";
        if (colorName.contains("đỏ") || colorName.contains("red")) return "#FF0000";
        if (colorName.contains("hồng phấn") || colorName.contains("rose")) return "#FF66CC";
        if (colorName.contains("hồng") || colorName.contains("pink")) return "#FFC0CB";
        if (colorName.contains("fuchsia") || colorName.contains("hồng sen")) return "#FF00FF";
        if (colorName.contains("tím") || colorName.contains("purple") || colorName.contains("violet")) return "#800080";
        if (colorName.contains("mận") || colorName.contains("plum")) return "#8E4585";
        if (colorName.contains("lavender") || colorName.contains("oải hương")) return "#E6E6FA";
        if (colorName.contains("rêu") || colorName.contains("olive")) return "#808000";
        if (colorName.contains("xanh lá") || colorName.contains("green")) return "#008000";
        if (colorName.contains("cốm") || colorName.contains("mint")) return "#98FF98";
        if (colorName.contains("xanh ngọc") || colorName.contains("teal") || colorName.contains("turquoise")) return "#008080";
        if (colorName.contains("nâu") || colorName.contains("brown") || colorName.contains("bò")) return "#A52A2A";
        if (colorName.contains("be") || colorName.contains("beige") || colorName.contains("kem") || colorName.contains("cream")) return "#F5F5DC";
        if (colorName.contains("khaki") || colorName.contains("cát")) return "#C3B091";
        if (colorName.contains("nâu đất") || colorName.contains("terracotta")) return "#E2725B";
        if (colorName.contains("cam") || colorName.contains("orange")) return "#FFA500";
        if (colorName.contains("gạch") || colorName.contains("brick")) return "#B22222";
        if (colorName.contains("vàng") || colorName.contains("yellow")) return "#FFFF00";
        if (colorName.contains("mù tạt") || colorName.contains("mustard")) return "#FFDB58";
        if (colorName.contains("đa sắc") || colorName.contains("nhiều màu") || colorName.contains("multi")) return "#CCCCCC";
        if (colorName.contains("họa tiết") || colorName.contains("hoa") || colorName.contains("caro")) return "#F0F0F0";

        return "#E0E0E0";
    }

    class WishlistViewHolder extends RecyclerView.ViewHolder {
        private final ItemWishlistProductBinding binding;

        public WishlistViewHolder(ItemWishlistProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private String formatPrice(double price) {
            java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(new java.util.Locale("vi", "VN"));
            symbols.setGroupingSeparator('.');
            java.text.DecimalFormat decimalFormat = new java.text.DecimalFormat("#,###", symbols);
            return decimalFormat.format(price) + " VNĐ";
        }

        public void bind(Product product) {
            binding.productCard.txtProductName.setText(product.getName());
            binding.productCard.txtPrice.setText(formatPrice(product.getFinalPrice()));
            
            if (product.getDiscountPrice() != null && product.getDiscountPrice() > 0 && product.getDiscountPrice() < product.getPrice()) {
                binding.productCard.layoutOriginalPrice.setVisibility(View.VISIBLE);
                binding.productCard.txtOriginalPrice.setText(formatPrice(product.getPrice()));
                binding.productCard.txtOriginalPrice.setPaintFlags(binding.productCard.txtOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                binding.productCard.layoutOriginalPrice.setVisibility(View.GONE);
            }

            binding.productCard.txtRating.setText(String.valueOf(product.getRating()));
            binding.productCard.txtSoldCount.setText(String.format(java.util.Locale.getDefault(), "(%d lượt mua)", product.getSoldCount()));
            binding.productCard.txtProductSizes.setText(product.getSizeRange());

            // Setup colors
            setupColors(binding.productCard.layoutColors, product.getColors());

            // Setup image loading safely to avoid double slash
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                String imageUrl = product.getImages().get(0).getUrl();
                if (imageUrl != null) {
                    if (!imageUrl.startsWith("http")) {
                        imageUrl = "https://server-testing-ymn9.onrender.com" + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
                    }
                    Glide.with(itemView.getContext())
                            .load(imageUrl)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .into(binding.productCard.imgProduct);
                }
            }

            binding.productCard.txtSoldOut.setVisibility(product.getStock() <= 0 ? View.VISIBLE : View.GONE);

            String offer = product.getOfferDescription();
            if (offer != null && !offer.isEmpty()) {
                binding.productCard.txtOffer.setText(offer);
                binding.productCard.txtOffer.setVisibility(View.VISIBLE);
            } else {
                binding.productCard.txtOffer.setVisibility(View.GONE);
            }

            // Edit Mode UI
            if (isEditMode) {
                binding.layoutActionButtons.setVisibility(View.GONE);
                binding.cbSelectEdit.setVisibility(View.VISIBLE);
                binding.cbSelectEdit.setChecked(product.isSelected());
                binding.productCard.btnFavorite.setVisibility(View.GONE);
            } else {
                binding.layoutActionButtons.setVisibility(View.VISIBLE);
                binding.cbSelectEdit.setVisibility(View.GONE);
                binding.productCard.btnFavorite.setVisibility(View.VISIBLE);
            }
            
            // Heart icon logic (using binding from ProductAdapter style update logic)
            if (product.isFavorite()) {
                binding.productCard.btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                binding.productCard.btnFavorite.setImageTintList(null);
            } else {
                binding.productCard.btnFavorite.setImageResource(R.drawable.ic_favorite);
                binding.productCard.btnFavorite.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#333333")));
            }

            binding.cbSelectEdit.setOnClickListener(v -> {
                product.setSelected(binding.cbSelectEdit.isChecked());
                if (listener != null) listener.onSelectionChanged();
            });

            binding.productCard.btnFavorite.setOnClickListener(v -> {
                if (listener != null) listener.onFavoriteClick(product, getAdapterPosition());
            });

            binding.btnAddToCart.setOnClickListener(v -> {
                if (listener != null) listener.onAddToCartClick(product);
            });

            binding.btnFindSimilar.setOnClickListener(v -> {
                if (listener != null) listener.onFindSimilarClick(product);
            });

            itemView.setOnClickListener(v -> {
                if (isEditMode) {
                    binding.cbSelectEdit.setChecked(!binding.cbSelectEdit.isChecked());
                    product.setSelected(binding.cbSelectEdit.isChecked());
                    if (listener != null) listener.onSelectionChanged();
                } else {
                    if (listener != null) listener.onProductClick(product);
                }
            });
        }
    }
}
