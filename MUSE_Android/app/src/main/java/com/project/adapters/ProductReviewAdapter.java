package com.project.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.models.ProductReview;
import com.project.muse_android.R;

import android.widget.Filter;
import android.widget.Filterable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductReviewAdapter extends RecyclerView.Adapter<ProductReviewAdapter.ReviewViewHolder> implements Filterable {

    private List<ProductReview> reviewList;
    private List<ProductReview> reviewListFull;
    private OnImageClickListener onImageClickListener;

    public interface OnImageClickListener {
        void onImageClick(List<String> images, int position);
    }

    public ProductReviewAdapter(List<ProductReview> reviewList) {
        this.reviewList = reviewList;
        this.reviewListFull = new ArrayList<>(reviewList);
    }

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        ProductReview review = reviewList.get(position);

        holder.txtUserName.setText(review.getCustomerName() != null && !review.getCustomerName().isEmpty() ? review.getCustomerName() : "Người dùng MUSE");
        holder.txtComment.setText(review.getContent());
        
        // Format date: 19:07, 16/6/2026
        String rawDate = review.getCreatedAt();
        if (rawDate != null && !rawDate.isEmpty()) {
            try {
                // ISO format: 2024-03-20T10:00:00.000Z or similar
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
                inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                java.util.Date date = inputFormat.parse(rawDate);
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("HH:mm, dd/MM/yyyy", java.util.Locale.getDefault());
                holder.txtReviewDate.setText(outputFormat.format(date));
            } catch (Exception e) {
                holder.txtReviewDate.setText(rawDate);
            }
        } else {
            holder.txtReviewDate.setText("-");
        }

        // Display variant info or combine color/size
        String variant = review.getVariantInfo();
        if (variant == null || variant.isEmpty() || variant.equals("-")) {
            String color = review.getColor();
            String size = review.getSize();
            if (color != null && size != null) {
                variant = color + ", " + size;
            } else if (color != null) {
                variant = color;
            } else if (size != null) {
                variant = size;
            } else {
                variant = "-";
            }
        }
        holder.txtVariantInfo.setText("Phân loại: " + variant);
        holder.txtHelpful.setText(String.format(java.util.Locale.getDefault(), "Hữu ích (%d) 👍", review.getHelpfulCount()));

        if (review.getUserAvatar() != null && !review.getUserAvatar().isEmpty()) {
            String avatarUrl = review.getUserAvatar();
            if (!avatarUrl.startsWith("http")) {
                avatarUrl = "https://server-testing-ymn9.onrender.com" + (avatarUrl.startsWith("/") ? "" : "/") + avatarUrl;
            }
            Glide.with(holder.itemView.getContext())
                    .load(avatarUrl)
                    .placeholder(R.drawable.account_circle)
                    .circleCrop()
                    .into(holder.imgUserAvatar);
        } else {
            holder.imgUserAvatar.setImageResource(R.drawable.account_circle);
        }

        // Handle stars visibility based on rating
        int rating = review.getRating();
        ViewGroup starLayout = (ViewGroup) holder.itemView.findViewById(R.id.layoutStars);
        for (int i = 0; i < starLayout.getChildCount(); i++) {
            View star = starLayout.getChildAt(i);
            star.setVisibility(i < rating ? View.VISIBLE : View.GONE);
        }

        // Handle images
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            holder.rvReviewImages.setVisibility(View.VISIBLE);
            holder.rvReviewImages.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(holder.itemView.getContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
            
            // Show max 3 images
            List<String> displayImages = review.getImages();
            int actualCount = displayImages.size();
            List<String> limitedImages = actualCount > 3 ? displayImages.subList(0, 3) : displayImages;
            
            holder.rvReviewImages.setAdapter(new ReviewImageAdapter(limitedImages, actualCount, (imgPos) -> {
                if (onImageClickListener != null) {
                    onImageClickListener.onImageClick(review.getImages(), imgPos);
                }
            }));
        } else {
            holder.rvReviewImages.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reviewList != null ? reviewList.size() : 0;
    }

    public void updateData(List<ProductReview> newList) {
        if (newList != null) {
            Collections.sort(newList, (r1, r2) -> {
                if (r1.getCreatedAt() == null || r2.getCreatedAt() == null) return 0;
                return r2.getCreatedAt().compareTo(r1.getCreatedAt());
            });
        }
        this.reviewList = newList != null ? newList : new ArrayList<>();
        this.reviewListFull = new ArrayList<>(this.reviewList);
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<ProductReview> filteredList = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(reviewListFull);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (ProductReview item : reviewListFull) {
                        boolean nameMatch = item.getCustomerName() != null && item.getCustomerName().toLowerCase().contains(filterPattern);
                        boolean commentMatch = item.getContent() != null && item.getContent().toLowerCase().contains(filterPattern);
                        if (nameMatch || commentMatch) {
                            filteredList.add(item);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                reviewList.clear();
                reviewList.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView imgUserAvatar;
        TextView txtUserName, txtComment, txtReviewDate, txtVariantInfo, txtHelpful;
        RecyclerView rvReviewImages;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            imgUserAvatar = itemView.findViewById(R.id.imgUserAvatar);
            txtUserName = itemView.findViewById(R.id.txtUserName);
            txtComment = itemView.findViewById(R.id.txtComment);
            txtReviewDate = itemView.findViewById(R.id.txtReviewDate);
            txtVariantInfo = itemView.findViewById(R.id.txtVariantInfo);
            txtHelpful = itemView.findViewById(R.id.txtHelpful);
            rvReviewImages = itemView.findViewById(R.id.rvReviewImages);
        }
    }

    private static class ReviewImageAdapter extends RecyclerView.Adapter<ReviewImageAdapter.ImageViewHolder> {
        private List<String> images;
        private int totalCount;
        private OnItemClickListener listener;
        private static final String BASE_URL = "https://server-testing-ymn9.onrender.com";

        public interface OnItemClickListener {
            void onItemClick(int position);
        }

        public ReviewImageAdapter(List<String> images, int totalCount, OnItemClickListener listener) {
            this.images = images;
            this.totalCount = totalCount;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            String url = images.get(position);
            if (url != null && !url.startsWith("http")) {
                url = BASE_URL + (url.startsWith("/") ? "" : "/") + url;
            }
            
            android.util.Log.d("ReviewImage", "Loading image: " + url);
            
            Glide.with(holder.itemView.getContext())
                    .load(url)
                    .placeholder(R.drawable.image)
                    .error(R.drawable.image)
                    .centerCrop()
                    .into(holder.imgReview);

            // Overlay for the 3rd image if there are more
            if (position == 2 && totalCount > 3) {
                holder.viewOverlay.setVisibility(View.VISIBLE);
                holder.txtMoreImages.setVisibility(View.VISIBLE);
                holder.txtMoreImages.setText("+" + (totalCount - 3));
            } else {
                holder.viewOverlay.setVisibility(View.GONE);
                holder.txtMoreImages.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(position);
            });
        }

        @Override
        public int getItemCount() {
            return images != null ? images.size() : 0;
        }

        static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imgReview;
            View viewOverlay;
            TextView txtMoreImages;

            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imgReview = itemView.findViewById(R.id.imgReview);
                viewOverlay = itemView.findViewById(R.id.viewOverlay);
                txtMoreImages = itemView.findViewById(R.id.txtMoreImages);
            }
        }
    }
}
