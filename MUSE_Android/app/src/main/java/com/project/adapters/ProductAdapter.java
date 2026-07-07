package com.project.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import com.project.muse_android.R;
import com.project.muse_android.databinding.ItemProductHorizontalBinding;
import com.project.muse_android.databinding.ItemProductVerticalBinding;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_VERTICAL = 0;
    public static final int TYPE_HORIZONTAL = 1;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        default void onFavoriteClick(Product product, int position) {}
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Product product);
    }

    protected List<Product> products;
    private final int viewType;
    private final OnProductClickListener listener;
    private OnFavoriteClickListener favoriteListener;
    private static final String BASE_URL = "https://server-testing-ymn9.onrender.com";

    public ProductAdapter(List<Product> products) {
        this(products, TYPE_VERTICAL, null);
    }

    public ProductAdapter(List<Product> products, int viewType) {
        this(products, viewType, null);
    }

    public ProductAdapter(List<Product> products, int viewType, OnProductClickListener listener) {
        this.products = products;
        this.viewType = viewType;
        this.listener = listener;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener favoriteListener) {
        this.favoriteListener = favoriteListener;
    }

    public void setData(List<Product> products) {
        this.products = products;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HORIZONTAL) {
            ItemProductHorizontalBinding binding = ItemProductHorizontalBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new HorizontalViewHolder(binding);
        } else {
            ItemProductVerticalBinding binding = ItemProductVerticalBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new VerticalViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Product product = products.get(position);
        if (holder instanceof VerticalViewHolder) {
            ((VerticalViewHolder) holder).bind(product);
        } else if (holder instanceof HorizontalViewHolder) {
            ((HorizontalViewHolder) holder).bind(product);
        }
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    public class VerticalViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductVerticalBinding binding;

        public VerticalViewHolder(ItemProductVerticalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Product product) {
            binding.txtProductName.setText(product.getName());
            binding.txtProductSizes.setText(product.getSizeRange());

            Double dPrice = product.getDiscountPrice();
            if (dPrice != null && dPrice > 0) {
                binding.txtPrice.setText(formatPrice(dPrice));
                binding.txtOriginalPrice.setText(formatPrice(product.getPrice()));
                binding.layoutOriginalPrice.setVisibility(View.VISIBLE);
            } else {
                binding.txtPrice.setText(formatPrice(product.getPrice()));
                binding.layoutOriginalPrice.setVisibility(View.GONE);
            }

            String offer = product.getOfferDescription();
            if (offer != null && !offer.isEmpty()) {
                binding.txtOffer.setText(offer);
                binding.txtOffer.setVisibility(View.VISIBLE);
            } else {
                binding.txtOffer.setVisibility(View.GONE);
            }
            binding.txtRating.setText(String.valueOf(product.getRating()));
            binding.txtSoldCount.setText(String.format(Locale.getDefault(), "(%d lượt mua)", product.getSoldCount()));

            if (product.getStock() <= 0) {
                binding.txtSoldOut.setVisibility(View.VISIBLE);
                binding.imgProduct.setAlpha(0.6f);
            } else {
                binding.txtSoldOut.setVisibility(View.GONE);
                binding.imgProduct.setAlpha(1.0f);
            }

            loadImage(binding.imgProduct, product);
            setupColors(binding.layoutColors, product.getColors());

            updateFavoriteIcon(binding.btnFavorite, product.isFavorite());

            binding.btnFavorite.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    product.setFavorite(!product.isFavorite());
                    updateFavoriteIcon(binding.btnFavorite, product.isFavorite());

                    if (listener != null) listener.onFavoriteClick(product, position);
                    if (favoriteListener != null) favoriteListener.onFavoriteClick(product);
                }
                v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).withEndAction(() ->
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                ).start();
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product);
            });
        }
    }

    public class HorizontalViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductHorizontalBinding binding;

        public HorizontalViewHolder(ItemProductHorizontalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Product product) {
            binding.txtProductName.setText(product.getName());
            binding.txtProductSizes.setText(product.getSizeRange());

            Double dPrice = product.getDiscountPrice();
            if (dPrice != null && dPrice > 0) {
                binding.txtPrice.setText(formatPrice(dPrice));
                binding.txtOriginalPrice.setText(formatPrice(product.getPrice()));
                binding.layoutOriginalPrice.setVisibility(View.VISIBLE);
            } else {
                binding.txtPrice.setText(formatPrice(product.getPrice()));
                binding.layoutOriginalPrice.setVisibility(View.GONE);
            }

            String offer = product.getOfferDescription();
            if (offer != null && !offer.isEmpty()) {
                binding.txtOffer.setText(offer);
                binding.txtOffer.setVisibility(View.VISIBLE);
            } else {
                binding.txtOffer.setVisibility(View.GONE);
            }
            binding.txtRating.setText(String.valueOf(product.getRating()));
            binding.txtSoldCount.setText(String.format(Locale.getDefault(), "(%d lượt mua)", product.getSoldCount()));

            if (product.getStock() <= 0) {
                binding.txtSoldOut.setVisibility(View.VISIBLE);
                binding.imgProduct.setAlpha(0.6f);
            } else {
                binding.txtSoldOut.setVisibility(View.GONE);
                binding.imgProduct.setAlpha(1.0f);
            }

            loadImage(binding.imgProduct, product);
            setupColors(binding.layoutColors, product.getColors());

            updateFavoriteIcon(binding.btnFavorite, product.isFavorite());

            binding.btnFavorite.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    product.setFavorite(!product.isFavorite());
                    updateFavoriteIcon(binding.btnFavorite, product.isFavorite());

                    if (listener != null) listener.onFavoriteClick(product, position);
                    if (favoriteListener != null) favoriteListener.onFavoriteClick(product);
                }
                v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).withEndAction(() ->
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                ).start();
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(product);
            });
        }
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + " VNĐ";
    }

    private void updateFavoriteIcon(ImageView imageView, boolean isFavorite) {
        if (isFavorite) {
            imageView.setImageResource(R.drawable.ic_favorite_filled);
            imageView.setImageTintList(null);
        } else {
            imageView.setImageResource(R.drawable.ic_favorite);
            imageView.setImageTintList(ColorStateList.valueOf(Color.parseColor("#333333")));
        }
    }

    private void loadImage(ImageView imageView, Product product) {
        String imageUrl = null;
        if (product.getImages() != null && !product.getImages().isEmpty() && product.getImages().get(0) != null) {
            imageUrl = product.getImages().get(0).getUrl();
        }

        if (imageUrl != null) {
            if (!imageUrl.startsWith("http")) {
                imageUrl = BASE_URL + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
            }
            Glide.with(imageView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(imageView);
        }
    }

    private void setupColors(LinearLayout layout, List<String> colors) {
        layout.removeAllViews();
        if (colors == null || colors.isEmpty()) {
            return;
        }

        float density = layout.getContext().getResources().getDisplayMetrics().density;
        int dotSize = (int) (18 * density);
        int margin = (int) (0 * density);
        int addedCount = 0;

        for (String colorStr : colors) {
            if (addedCount >= 4) break;

            try {
                int color;
                if (colorStr.startsWith("#")) {
                    color = Color.parseColor(colorStr);
                } else {
                    color = mapColorName(colorStr);
                    if (color == Color.TRANSPARENT) color = Color.parseColor("#E0E0E0");
                }

                LinearLayout colorItem = new LinearLayout(layout.getContext());
                colorItem.setOrientation(LinearLayout.VERTICAL);
                colorItem.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
                LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                        (int)(30 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
                itemParams.setMargins(0, 0, margin, 0);
                colorItem.setLayoutParams(itemParams);

                View dot = new View(layout.getContext());
                LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
                dot.setLayoutParams(dotParams);

                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.OVAL);
                shape.setColor(color);
                shape.setStroke(2, Color.BLACK);
                dot.setBackground(shape);

                TextView label = new TextView(layout.getContext());
                label.setText(colorStr);
                label.setTextSize(7);
                label.setTextColor(Color.parseColor("#333333"));
                label.setGravity(android.view.Gravity.CENTER);
                label.setMaxLines(1);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                label.setPadding((int)(1*density), 0, (int)(1*density), 0);

                label.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                colorItem.addView(dot);
                colorItem.addView(label);

                layout.addView(colorItem);
                addedCount++;
            } catch (Exception e) {
            }
        }
    }

    private int mapColorName(String name) {
        if (name == null) return Color.TRANSPARENT;
        String colorName = name.toLowerCase().trim();

        // Basic Colors
        if (colorName.contains("trắng") || colorName.contains("white")) return Color.WHITE;
        if (colorName.contains("đen") || colorName.contains("black")) return Color.BLACK;
        if (colorName.contains("xám") || colorName.contains("gray") || colorName.contains("grey") || colorName.contains("ghi")) return Color.GRAY;
        
        // Metallics
        if (colorName.contains("bạc") || colorName.contains("silver")) return Color.parseColor("#C0C0C0");
        if (colorName.contains("vàng đồng") || colorName.contains("gold")) return Color.parseColor("#D4AF37");
        
        // Blues
        if (colorName.contains("navy") || colorName.contains("xanh than") || colorName.contains("than")) return Color.parseColor("#000080");
        if (colorName.contains("sky") || colorName.contains("da trời")) return Color.parseColor("#87CEEB");
        if (colorName.contains("xanh dương") || colorName.contains("blue") || colorName.equals("xanh")) return Color.parseColor("#0000FF");
        if (colorName.contains("cobalt") || colorName.contains("xanh coban")) return Color.parseColor("#0047AB");
        
        // Reds & Pinks
        if (colorName.contains("đỏ đô") || colorName.contains("burgundy") || colorName.contains("đỏ rượu")) return Color.parseColor("#800000");
        if (colorName.contains("đỏ") || colorName.contains("red")) return Color.parseColor("#FF0000");
        if (colorName.contains("hồng phấn") || colorName.contains("rose")) return Color.parseColor("#FF66CC");
        if (colorName.contains("hồng") || colorName.contains("pink")) return Color.parseColor("#FFC0CB");
        if (colorName.contains("fuchsia") || colorName.contains("hồng sen")) return Color.parseColor("#FF00FF");
        
        // Purples
        if (colorName.contains("tím") || colorName.contains("purple") || colorName.contains("violet")) return Color.parseColor("#800080");
        if (colorName.contains("mận") || colorName.contains("plum")) return Color.parseColor("#8E4585");
        if (colorName.contains("lavender") || colorName.contains("oải hương")) return Color.parseColor("#E6E6FA");
        
        // Greens
        if (colorName.contains("rêu") || colorName.contains("olive")) return Color.parseColor("#808000");
        if (colorName.contains("xanh lá") || colorName.contains("green")) return Color.parseColor("#008000");
        if (colorName.contains("cốm") || colorName.contains("mint")) return Color.parseColor("#98FF98");
        if (colorName.contains("xanh ngọc") || colorName.contains("teal") || colorName.contains("turquoise")) return Color.parseColor("#008080");
        
        // Browns & Earth Tones
        if (colorName.contains("nâu") || colorName.contains("brown") || colorName.contains("bò")) return Color.parseColor("#A52A2A");
        if (colorName.contains("be") || colorName.contains("beige") || colorName.contains("kem") || colorName.contains("cream")) return Color.parseColor("#F5F5DC");
        if (colorName.contains("khaki") || colorName.contains("cát")) return Color.parseColor("#C3B091");
        if (colorName.contains("nâu đất") || colorName.contains("terracotta")) return Color.parseColor("#E2725B");
        
        // Oranges & Yellows
        if (colorName.contains("cam") || colorName.contains("orange")) return Color.parseColor("#FFA500");
        if (colorName.contains("gạch") || colorName.contains("brick")) return Color.parseColor("#B22222");
        if (colorName.contains("vàng") || colorName.contains("yellow")) return Color.parseColor("#FFFF00");
        if (colorName.contains("mù tạt") || colorName.contains("mustard")) return Color.parseColor("#FFDB58");
        
        // Patterns & Multi
        if (colorName.contains("đa sắc") || colorName.contains("nhiều màu") || colorName.contains("multi")) return Color.LTGRAY;
        if (colorName.contains("họa tiết") || colorName.contains("hoa") || colorName.contains("caro")) return Color.parseColor("#F0F0F0");

        return Color.TRANSPARENT;
    }
}
