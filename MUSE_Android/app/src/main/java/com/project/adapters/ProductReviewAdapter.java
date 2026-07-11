package com.project.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;
import android.net.Uri;

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

        // Display variant info
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
            
            if (avatarUrl.contains("localhost:3000")) {
                avatarUrl = avatarUrl.replace("http://localhost:3000", "https://server-testing-ymn9.onrender.com");
            } else if (!avatarUrl.startsWith("http")) {
                avatarUrl = "https://server-testing-ymn9.onrender.com" + (avatarUrl.startsWith("/") ? "" : "/") + avatarUrl;
            }

            Glide.with(holder.itemView.getContext())
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_profile_vector)
                    .error(R.drawable.ic_profile_vector)
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

        // Handle stars
        int rating = review.getRating();
        ViewGroup starLayout = (ViewGroup) holder.itemView.findViewById(R.id.layoutStars);
        for (int i = 0; i < starLayout.getChildCount(); i++) {
            View star = starLayout.getChildAt(i);
            star.setVisibility(i < rating ? View.VISIBLE : View.GONE);
        }

        // Handle Media (Combined Images & Videos)
        List<MediaItem> allMedia = new ArrayList<>();
        List<String> images = review.getImages();
        List<String> videos = review.getVideos();
        
        if (images != null) {
            for (String img : images) allMedia.add(new MediaItem(img, false));
        }
        if (videos != null) {
            for (String vid : videos) allMedia.add(new MediaItem(vid, true));
        }

        if (!allMedia.isEmpty()) {
            holder.rvReviewImages.setVisibility(View.VISIBLE);
            holder.rvReviewImages.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(holder.itemView.getContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
            
            int actualCount = allMedia.size();
            List<MediaItem> limitedMedia = actualCount > 3 ? allMedia.subList(0, 3) : allMedia;
            
            holder.rvReviewImages.setAdapter(new ReviewMediaAdapter(limitedMedia, actualCount, (pos) -> {
                MediaItem selected = allMedia.get(pos);
                if (selected.isVideo) {
                    try {
                        String fullUrl = selected.url;
                        if (!fullUrl.startsWith("http")) {
                            fullUrl = "https://server-testing-ymn9.onrender.com" + (fullUrl.startsWith("/") ? "" : "/") + fullUrl;
                        }
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(fullUrl), "video/*");
                        holder.itemView.getContext().startActivity(intent);
                    } catch (Exception e) {
                        android.widget.Toast.makeText(holder.itemView.getContext(), "Không thể phát video", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (onImageClickListener != null) {
                        onImageClickListener.onImageClick(images, pos, review);
                    }
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
            ImageView thumbIcon = (ImageView) ((ViewGroup) holder.btnHelpful).getChildAt(0);
            thumbIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_500));
        } else {
            holder.btnHelpful.setBackgroundResource(R.drawable.bg_helpful_button);
            holder.txtHelpful.setTextColor(android.graphics.Color.parseColor("#333333"));
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

    private static class MediaItem {
        String url;
        boolean isVideo;
        MediaItem(String url, boolean isVideo) { this.url = url; this.isVideo = isVideo; }
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

    private static class ReviewMediaAdapter extends RecyclerView.Adapter<ReviewMediaAdapter.ImageViewHolder> {
        private List<MediaItem> mediaItems;
        private int totalCount;
        private OnItemClickListener listener;
        private static final String BASE_URL = "https://server-testing-ymn9.onrender.com";

        public interface OnItemClickListener {
            void onItemClick(int position);
        }

        public ReviewMediaAdapter(List<MediaItem> items, int totalCount, OnItemClickListener listener) {
            this.mediaItems = items;
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
            MediaItem item = mediaItems.get(position);
            String url = item.url;
            if (url != null) {
                if (url.contains("localhost:3000")) {
                    url = url.replace("http://localhost:3000", BASE_URL);
                } else if (!url.startsWith("http")) {
                    url = BASE_URL + (url.startsWith("/") ? "" : "/") + url;
                }
            }
            
            Glide.with(holder.itemView.getContext())
                    .load(url)
                    .placeholder(R.drawable.image)
                    .error(R.drawable.image)
                    .centerCrop()
                    .into(holder.imgReview);

            holder.ivPlay.setVisibility(item.isVideo ? View.VISIBLE : View.GONE);

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
            return mediaItems != null ? mediaItems.size() : 0;
        }

        static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imgReview, ivPlay;
            View viewOverlay;
            TextView txtMoreImages;
            View layoutMoreImages;

            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imgReview = itemView.findViewById(R.id.imgReview);
                ivPlay = itemView.findViewById(R.id.ivPlay);
                viewOverlay = itemView.findViewById(R.id.viewOverlay);
                txtMoreImages = itemView.findViewById(R.id.txtMoreImages);
                layoutMoreImages = itemView.findViewById(R.id.layoutMoreImages);
            }
        }
    }
}
