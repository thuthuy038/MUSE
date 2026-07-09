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
        void onImageClick(List<String> images, int position, ProductReview review);
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
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("HH:mm , dd/MM/yyyy", java.util.Locale.getDefault());
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
        holder.txtVariantInfo.setText("PHÂN LOẠI: " + variant.toUpperCase());

        updateHelpfulUI(holder, review);

        holder.btnHelpful.setOnClickListener(v -> {
            review.setLiked(!review.isLiked());
            if (review.isLiked()) {
                review.setHelpfulCount(review.getHelpfulCount() + 1);
            } else {
                review.setHelpfulCount(Math.max(0, review.getHelpfulCount() - 1));
            }
            updateHelpfulUI(holder, review);
        });

        String avatarUrl = review.getUserAvatar();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            holder.imgUserAvatar.setVisibility(View.VISIBLE);
            holder.txtUserInitial.setVisibility(View.GONE);
            
            // Fix localhost or relative URLs from backend
            if (avatarUrl.contains("localhost:3000")) {
                avatarUrl = avatarUrl.replace("http://localhost:3000", "https://server-testing-ymn9.onrender.com");
            } else if (!avatarUrl.startsWith("http")) {
                // Ensure correct path formatting: BASE_URL + /uploads/... or BASE_URL + path
                avatarUrl = "https://server-testing-ymn9.onrender.com" + (avatarUrl.startsWith("/") ? "" : "/") + avatarUrl;
            }

            android.util.Log.d("ReviewAvatar", "Final avatar URL: " + avatarUrl);

            Glide.with(holder.itemView.getContext())
                    .load(avatarUrl)
                    .placeholder(R.drawable.account_circle)
                    .error(R.drawable.account_circle)
                    .circleCrop()
                    .into(holder.imgUserAvatar);

            holder.imgUserAvatar.setColorFilter(null);
        } else {
            holder.imgUserAvatar.setVisibility(View.GONE);
            holder.txtUserInitial.setVisibility(View.VISIBLE);
            String name = review.getCustomerName();
            if (name != null && !name.isEmpty()) {
                holder.txtUserInitial.setText(name.substring(0, 1).toUpperCase());
            } else {
                holder.txtUserInitial.setText("M");
            }
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
                    onImageClickListener.onImageClick(review.getImages(), imgPos, review);
                }
            }));
        } else {
            holder.rvReviewImages.setVisibility(View.GONE);
        }

        // Handle Admin Reply
        String adminReply = review.getAdminReply();
        if (adminReply != null && !adminReply.trim().isEmpty()) {
            holder.layoutAdminReply.setVisibility(View.VISIBLE);
            holder.txtAdminReply.setText(adminReply);
        } else {
            holder.layoutAdminReply.setVisibility(View.GONE);
        }
    }

    private void updateHelpfulUI(ReviewViewHolder holder, ProductReview review) {
        holder.txtHelpful.setText(String.format(java.util.Locale.getDefault(), "Hữu ích (%d)", review.getHelpfulCount()));

        if (review.isLiked()) {
            holder.btnHelpful.setBackgroundResource(R.drawable.bg_helpful_button_selected);
            holder.txtHelpful.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_500));
            // Tint thumb icon pink
            ImageView thumbIcon = (ImageView) ((ViewGroup) holder.btnHelpful).getChildAt(0);
            thumbIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_500));
        } else {
            holder.btnHelpful.setBackgroundResource(R.drawable.bg_helpful_button);
            holder.txtHelpful.setTextColor(android.graphics.Color.parseColor("#333333"));
            // Reset thumb icon to gray
            ImageView thumbIcon = (ImageView) ((ViewGroup) holder.btnHelpful).getChildAt(0);
            thumbIcon.setColorFilter(android.graphics.Color.parseColor("#666666"));
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
        TextView txtUserName, txtComment, txtReviewDate, txtVariantInfo, txtHelpful, txtUserInitial, txtAdminReply;
        RecyclerView rvReviewImages;
        View btnHelpful, layoutAdminReply;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            imgUserAvatar = itemView.findViewById(R.id.imgUserAvatar);
            txtUserName = itemView.findViewById(R.id.txtUserName);
            txtComment = itemView.findViewById(R.id.txtComment);
            txtReviewDate = itemView.findViewById(R.id.txtReviewDate);
            txtVariantInfo = itemView.findViewById(R.id.txtVariantInfo);
            txtHelpful = itemView.findViewById(R.id.txtHelpful);
            txtUserInitial = itemView.findViewById(R.id.txtUserInitial);
            rvReviewImages = itemView.findViewById(R.id.rvReviewImages);
            btnHelpful = itemView.findViewById(R.id.btnHelpful);
            layoutAdminReply = itemView.findViewById(R.id.layoutAdminReply);
            txtAdminReply = itemView.findViewById(R.id.txtAdminReply);
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
            if (url != null) {
                if (url.contains("localhost:3000")) {
                    url = url.replace("http://localhost:3000", BASE_URL);
                } else if (!url.startsWith("http")) {
                    url = BASE_URL + (url.startsWith("/") ? "" : "/") + url;
                }
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
                holder.layoutMoreImages.setVisibility(View.VISIBLE);
                holder.txtMoreImages.setText("+" + (totalCount - 3));
            } else {
                holder.viewOverlay.setVisibility(View.GONE);
                holder.layoutMoreImages.setVisibility(View.GONE);
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
            View layoutMoreImages;

            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imgReview = itemView.findViewById(R.id.imgReview);
                viewOverlay = itemView.findViewById(R.id.viewOverlay);
                txtMoreImages = itemView.findViewById(R.id.txtMoreImages);
                layoutMoreImages = itemView.findViewById(R.id.layoutMoreImages);
            }
        }
    }
}
