package com.project.adapters;

import android.graphics.Color;
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
        holder.binding.txtCategoryName.setText(category.getName());
        
        boolean isSelected = isSelectionEnabled && (position == selectedPosition);
        if (isSelected && selectedPosition != -1) {
            holder.binding.cardCategory.setCardElevation(15f);
            holder.binding.cardCategory.setAlpha(1.0f);
            holder.binding.cardCategory.setScaleX(1.25f);
            holder.binding.cardCategory.setScaleY(1.25f);
            holder.binding.txtCategoryName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_500));
            holder.binding.txtCategoryName.setAlpha(1.0f);
            holder.binding.txtCategoryName.setTextSize(14);
            
            GradientDrawable border = new GradientDrawable();
            border.setCornerRadius(12 * holder.itemView.getResources().getDisplayMetrics().density);
            border.setStroke(6, ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_500));
            border.setColor(Color.parseColor("#F5F5F5"));
            holder.binding.cardCategory.setBackground(border);
        } else {
            holder.binding.cardCategory.setCardElevation(3f);
            holder.binding.cardCategory.setAlpha(0.9f);
            holder.binding.cardCategory.setScaleX(1.0f);
            holder.binding.cardCategory.setScaleY(1.0f);
            holder.binding.txtCategoryName.setTextColor(Color.parseColor("#333333"));
            holder.binding.txtCategoryName.setAlpha(0.8f);
            holder.binding.txtCategoryName.setTextSize(12);
            holder.binding.cardCategory.setBackground(null);
            holder.binding.cardCategory.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
        }

        String imageUrl = category.getImageUrl();
        if (imageUrl != null) {
            if (!imageUrl.startsWith("http")) {
                imageUrl = BASE_URL + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
            }
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.binding.imgCategory);
        } else {
            holder.binding.imgCategory.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionEnabled) {
                int oldPos = selectedPosition;
                selectedPosition = holder.getBindingAdapterPosition();
                notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);
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
