package com.project.muse_android.ai;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.muse_android.R;
import com.project.muse_android.product.ProductDetailActivity;

import java.util.List;

public class ArchivedOutfitAdapter extends RecyclerView.Adapter<ArchivedOutfitAdapter.ViewHolder> {

    private final List<SavedOutfit> savedOutfits;
    private final Context context;
    private final OnOutfitDeleteListener listener;
    private static final String BASE_URL = "https://server-testing-ymn9.onrender.com";

    public interface OnOutfitDeleteListener {
        void onDeleteClick(SavedOutfit outfit, int position);
    }

    public ArchivedOutfitAdapter(Context context, List<SavedOutfit> savedOutfits, OnOutfitDeleteListener listener) {
        this.context = context;
        this.savedOutfits = savedOutfits;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_archived_saved_outfit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedOutfit outfit = savedOutfits.get(position);
        holder.tvSetName.setText(outfit.getSetName());
        holder.tvSetDescription.setText("Đã lưu vào " + outfit.getSavedDate());

        // Top Item
        holder.tvTopName.setText(outfit.getTopName());
        loadImage(holder.ivTopImage, outfit.getTopImageUrl());

        // Bottom Item
        holder.tvBottomName.setText(outfit.getBottomName());
        loadImage(holder.ivBottomImage, outfit.getBottomImageUrl());

        boolean isTryOn = outfit.getBottomId() != null && outfit.getBottomId().startsWith("tryon_");
        View.OnClickListener clickListener = v -> {
            if (isTryOn) {
                String color = "Mặc định";
                String size = "Mặc định";
                try {
                    String[] parts = outfit.getBottomId().split("_");
                    if (parts.length >= 3) {
                        color = parts[1];
                        size = parts[2];
                    }
                } catch (Exception ignored) {}

                Intent intent = new Intent(context, VirtualFittingResultActivity.class);
                intent.putExtra("image_path", outfit.getBottomImageUrl());
                intent.putExtra("product_id", outfit.getTopId());
                intent.putExtra("size", size);
                intent.putExtra("color", color);
                intent.putExtra("from_cart", true); // Hide Add to Cart when viewing from archival
                context.startActivity(intent);
            } else {
                openProductDetail(outfit.getTopId());
            }
        };

        holder.cardTop.setOnClickListener(clickListener);
        holder.cardBottom.setOnClickListener(clickListener);
        holder.itemView.setOnClickListener(clickListener);

        // Delete button
        holder.btnDeleteSavedSet.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(outfit, holder.getBindingAdapterPosition());
        });
    }

    private void openProductDetail(String id) {
        if (id == null || id.isEmpty()) return;
        Intent intent = new Intent(context, ProductDetailActivity.class);
        intent.putExtra("product_id", id);
        context.startActivity(intent);
    }

    private void loadImage(ImageView imageView, String url) {
        if (url == null || url.isEmpty()) return;
        String fullUrl = url;
        if (!fullUrl.startsWith("http") && !fullUrl.contains("data/user") && !fullUrl.contains("cache") && !fullUrl.contains("files") && !fullUrl.contains("Pictures")) {
            fullUrl = BASE_URL + (fullUrl.startsWith("/") ? "" : "/") + fullUrl;
        }
        Glide.with(imageView.getContext())
                .load(fullUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(imageView);
    }

    @Override
    public int getItemCount() {
        return savedOutfits.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSetName;
        TextView tvSetDescription;
        View cardTop;
        ImageView ivTopImage;
        TextView tvTopName;
        View cardBottom;
        ImageView ivBottomImage;
        TextView tvBottomName;
        ImageView btnDeleteSavedSet;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSetName = itemView.findViewById(R.id.tvSetName);
            tvSetDescription = itemView.findViewById(R.id.tvSetDescription);
            cardTop = itemView.findViewById(R.id.cardTop);
            ivTopImage = itemView.findViewById(R.id.ivTopImage);
            tvTopName = itemView.findViewById(R.id.tvTopName);
            cardBottom = itemView.findViewById(R.id.cardBottom);
            ivBottomImage = itemView.findViewById(R.id.ivBottomImage);
            tvBottomName = itemView.findViewById(R.id.tvBottomName);
            btnDeleteSavedSet = itemView.findViewById(R.id.btnDeleteSavedSet);
        }
    }
}
