package com.project.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.models.Product;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ItemProductBinding;

import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private final List<Product> products;
    private static final String BASE_URL = "https://server-testing-ymn9.onrender.com";

    public ProductAdapter(List<Product> products) {
        this.products = products;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductBinding binding = ItemProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        
        holder.binding.txtProductName.setText(product.getName());
        holder.binding.txtProductSizes.setText(product.getSizeRange());
        holder.binding.txtPrice.setText(String.format(Locale.getDefault(), "%.0f VNĐ", product.getPrice()));
        
        double originalPrice = product.getOriginalPrice();
        if (originalPrice > product.getPrice()) {
            holder.binding.txtOriginalPrice.setText(String.format(Locale.getDefault(), "%.0f VNĐ", originalPrice));
            holder.binding.txtOriginalPrice.setVisibility(ViewGroup.VISIBLE);
        } else {
            holder.binding.txtOriginalPrice.setVisibility(ViewGroup.GONE);
        }

        holder.binding.txtOffer.setText(product.getOfferDescription());
        holder.binding.txtRating.setText(String.valueOf(product.getRating()));
        holder.binding.txtSoldCount.setText(String.format(Locale.getDefault(), "(%d lượt mua)", product.getSoldCount()));

        String imageUrl = null;
        if (product.getImages() != null && !product.getImages().isEmpty() && product.getImages().get(0) != null) {
            imageUrl = product.getImages().get(0).getUrl();
        }

        if (imageUrl != null) {
            if (!imageUrl.startsWith("http")) {
                imageUrl = BASE_URL + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
            }
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.binding.imgProduct);
        }

        // Logic for colors could be added here
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemProductBinding binding;

        public ViewHolder(@NonNull ItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
