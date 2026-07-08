package com.project.adapters;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.models.Category;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ItemCategoryBinding;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private List<Category> categories;
    private OnCategoryClickListener listener;
    private int selectedPosition = -1;
    private boolean isSelectionEnabled = true;
    private static final String BASE_URL = "https://server-testing-ymn9.onrender.com";

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    public void setSelectionEnabled(boolean selectionEnabled) {
        this.isSelectionEnabled = selectionEnabled;
    }

    public void setSelectedCategoryId(String categoryId) {
        int oldPos = selectedPosition;
        selectedPosition = -1;
        
        if (categoryId != null && !categoryId.isEmpty()) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getId().equals(categoryId)) {
                    selectedPosition = i;
                    break;
                }
            }
        }
        
        if (oldPos != -1 && oldPos < getItemCount()) notifyItemChanged(oldPos);
        if (selectedPosition != -1) notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.binding.txtCategoryName.setText(category.getName().toUpperCase());
        
        boolean isSelected = isSelectionEnabled && (position == selectedPosition);

        if (isSelected) {
            // Selected style: Larger size, PINK text, Pink shadow
            holder.itemView.setScaleX(1.15f);
            holder.itemView.setScaleY(1.15f);

            holder.binding.txtCategoryName.setTypeface(null, Typeface.BOLD);
            holder.binding.txtCategoryName.setTextColor(Color.parseColor("#FB6F92")); // Pink color

            // Pink Shadow effect for the card
            holder.binding.cardCategory.setCardElevation(12f);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                holder.binding.cardCategory.setOutlineSpotShadowColor(Color.parseColor("#FB6F92"));
                holder.binding.cardCategory.setOutlineAmbientShadowColor(Color.parseColor("#FB6F92"));
            }
        } else {
            // Normal style
            holder.itemView.setScaleX(1.0f);
            holder.itemView.setScaleY(1.0f);
            holder.binding.layoutContainer.setBackground(null);
            holder.binding.txtCategoryName.setTypeface(null, Typeface.NORMAL);
            holder.binding.txtCategoryName.setTextColor(Color.parseColor("#666666"));

            // Remove shadow
            holder.binding.cardCategory.setCardElevation(2f);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                holder.binding.cardCategory.setOutlineSpotShadowColor(Color.BLACK);
                holder.binding.cardCategory.setOutlineAmbientShadowColor(Color.BLACK);
            }
        }

        String imageUrl = category.getImageUrl();
        if (imageUrl != null) {
            // Reset to default scale type for normal categories
            holder.binding.imgCategory.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            holder.binding.imgCategory.setPadding(0, 0, 0, 0);

            if (!imageUrl.startsWith("http")) {
                imageUrl = BASE_URL + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
            }
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.binding.imgCategory);
        } else {
            // Handle "All" category which might not have an image - Use brand logo
            if ("all".equals(category.getId())) {
                holder.binding.imgCategory.setImageResource(R.drawable.logo); // Logo thương hiệu

                // Adjust scale type and padding to prevent cropping for logo
                holder.binding.imgCategory.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                int padding = (int) (12 * holder.itemView.getResources().getDisplayMetrics().density);
                holder.binding.imgCategory.setPadding(padding, padding, padding, padding);
            } else {
                holder.binding.imgCategory.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                holder.binding.imgCategory.setPadding(0, 0, 0, 0);
                holder.binding.imgCategory.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionEnabled) {
                int oldPos = selectedPosition;
                selectedPosition = holder.getBindingAdapterPosition();
                if (oldPos != selectedPosition) {
                    notifyItemChanged(oldPos);
                    notifyItemChanged(selectedPosition);
                }
            }

            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ItemCategoryBinding binding;

        public ViewHolder(@NonNull ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
