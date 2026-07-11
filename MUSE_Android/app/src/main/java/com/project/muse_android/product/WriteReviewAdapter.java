package com.project.muse_android.product;

import android.content.Context;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.project.models.Order;
import com.project.muse_android.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WriteReviewAdapter extends RecyclerView.Adapter<WriteReviewAdapter.ViewHolder> {

    public interface OnUploadClickListener {
        void onUploadClick(int position);
    }

    public interface OnMediaClickListener {
        void onMediaClick(Uri uri, boolean isVideo);
        default void onUrlClick(String url, boolean isVideo) {}
    }

    public interface OnProductClickListener {
        void onProductClick(Order.OrderItem item);
    }

    private final Context context;
    private final List<Order.OrderItem> orderItems;
    private final OnUploadClickListener uploadListener;
    private OnMediaClickListener mediaClickListener;
    private OnProductClickListener productClickListener;
    
    private final Map<Integer, ReviewState> reviewData = new HashMap<>();

    public WriteReviewAdapter(Context context, List<Order.OrderItem> items, OnUploadClickListener uploadListener) {
        this.context = context;
        this.orderItems = items;
        this.uploadListener = uploadListener;
        for (int i = 0; i < items.size(); i++) {
            reviewData.put(i, new ReviewState());
        }
    }

    public void setOnMediaClickListener(OnMediaClickListener listener) {
        this.mediaClickListener = listener;
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.productClickListener = listener;
    }

    public void setInitialState(int position, int rating, String comment, List<String> imageUrls, List<String> videoUrls) {
        ReviewState state = reviewData.get(position);
        if (state != null) {
            state.rating = rating;
            state.comment = comment;
            if (imageUrls != null) state.existingImageUrls.addAll(imageUrls);
            if (videoUrls != null) state.existingVideoUrls.addAll(videoUrls);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_write_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final int adapterPos = holder.getBindingAdapterPosition();
        Order.OrderItem item = orderItems.get(adapterPos);
        ReviewState state = reviewData.get(adapterPos);

        holder.txtProductName.setText(item.getName());
        holder.txtProductVariant.setText(String.format("Kích cỡ: %s | Màu: %s", item.getSize(), item.getColor()));
        
        String imageUrl = item.getImage();
        if (imageUrl != null) {
            if (imageUrl.startsWith("/")) imageUrl = "https://server-testing-ymn9.onrender.com" + imageUrl;
            Glide.with(context).load(imageUrl).placeholder(R.drawable.demo_product).into(holder.imgProduct);
        }

        holder.ratingBar.setRating(state.rating);
        
        if (holder.textWatcher != null) {
            holder.etComment.removeTextChangedListener(holder.textWatcher);
        }
        
        holder.etComment.setText(state.comment);
        holder.txtCharCount.setText(String.format(Locale.getDefault(), "%d / 500", state.comment.length()));

        holder.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                state.comment = s.toString();
                holder.txtCharCount.setText(String.format(Locale.getDefault(), "%d / 500", s.length()));
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        holder.etComment.addTextChangedListener(holder.textWatcher);

        holder.ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) state.rating = (int) rating;
        });

        // Chips logic
        holder.chipGroupFeedback.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            int id = checkedIds.get(0);
            Chip chip = group.findViewById(id);
            if (chip == null) return;
            
            String text = chip.getText().toString();
            
            int newRating = 0;
            if (id == R.id.chip5Star) newRating = 5;
            else if (id == R.id.chip4Star) newRating = 4;
            else if (id == R.id.chip3Star) newRating = 3;
            else if (id == R.id.chip2Star) newRating = 2;
            else if (id == R.id.chip1Star) newRating = 1;
            
            if (newRating > 0) {
                state.rating = newRating;
                holder.ratingBar.setRating(newRating);
            }
            
            state.comment = text;
            holder.etComment.post(() -> {
                if (holder.textWatcher != null) holder.etComment.removeTextChangedListener(holder.textWatcher);
                holder.etComment.setText(text);
                holder.etComment.setSelection(text.length());
                holder.txtCharCount.setText(text.length() + " / 500");
                if (holder.textWatcher != null) holder.etComment.addTextChangedListener(holder.textWatcher);
            });
        });

        holder.btnUpload.setOnClickListener(v -> uploadListener.onUploadClick(adapterPos));

        holder.productInfoLayout.setOnClickListener(v -> {
            if (productClickListener != null) productClickListener.onProductClick(item);
        });

        // Media RecyclerView (Combined existing URLs and new Uris)
        List<Object> allMedia = new ArrayList<>();
        allMedia.addAll(state.existingImageUrls);
        allMedia.addAll(state.existingVideoUrls);
        allMedia.addAll(state.selectedMedia);

        if (allMedia.isEmpty()) {
            holder.rvSelectedMedia.setVisibility(View.GONE);
        } else {
            holder.rvSelectedMedia.setVisibility(View.VISIBLE);
            holder.rvSelectedMedia.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            holder.rvSelectedMedia.setAdapter(new MediaPreviewAdapter(allMedia, pos -> {
                // Determine which list to remove from
                int currentPos = pos;
                if (currentPos < state.existingImageUrls.size()) {
                    state.existingImageUrls.remove(currentPos);
                } else {
                    currentPos -= state.existingImageUrls.size();
                    if (currentPos < state.existingVideoUrls.size()) {
                        state.existingVideoUrls.remove(currentPos);
                    } else {
                        currentPos -= state.existingVideoUrls.size();
                        state.selectedMedia.remove(currentPos);
                    }
                }
                notifyItemChanged(adapterPos);
            }, (obj, isVideo) -> {
                if (mediaClickListener != null) {
                    if (obj instanceof Uri) mediaClickListener.onMediaClick((Uri) obj, isVideo);
                    else mediaClickListener.onUrlClick((String) obj, isVideo);
                }
            }, state.existingImageUrls.size(), state.existingVideoUrls.size()));
        }
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    public void updateMedia(int position, Uri uri) {
        ReviewState state = reviewData.get(position);
        if (state != null) {
            if (state.selectedMedia.size() + state.existingImageUrls.size() + state.existingVideoUrls.size() < 10) {
                state.selectedMedia.add(uri);
                notifyItemChanged(position);
            }
        }
    }

    public Map<Integer, ReviewState> getReviewResults() {
        return reviewData;
    }

    public static class ReviewState {
        public int rating = 0;
        public String comment = "";
        public List<Uri> selectedMedia = new ArrayList<>();
        public List<String> existingImageUrls = new ArrayList<>();
        public List<String> existingVideoUrls = new ArrayList<>();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtProductName, txtProductVariant, txtCharCount;
        RatingBar ratingBar;
        EditText etComment;
        View btnUpload, productInfoLayout;
        RecyclerView rvSelectedMedia;
        ChipGroup chipGroupFeedback;
        TextWatcher textWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtProductVariant = itemView.findViewById(R.id.txtProductVariant);
            txtCharCount = itemView.findViewById(R.id.txtCharCount);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            etComment = itemView.findViewById(R.id.etComment);
            btnUpload = itemView.findViewById(R.id.btnUpload);
            productInfoLayout = itemView.findViewById(R.id.productInfoLayout);
            rvSelectedMedia = itemView.findViewById(R.id.rvSelectedMedia);
            chipGroupFeedback = itemView.findViewById(R.id.chipGroupFeedback);
        }
    }

    private static class MediaPreviewAdapter extends RecyclerView.Adapter<MediaPreviewAdapter.MediaViewHolder> {
        private final List<Object> items;
        private final OnDeleteListener deleteListener;
        private final OnMediaItemClickListener clickListener;
        private final int existingImgCount;
        private final int existingVidCount;

        interface OnDeleteListener { void onDelete(int pos); }
        interface OnMediaItemClickListener { void onMediaClick(Object item, boolean isVideo); }

        MediaPreviewAdapter(List<Object> items, OnDeleteListener deleteListener, OnMediaItemClickListener clickListener, int existingImgCount, int existingVidCount) {
            this.items = items;
            this.deleteListener = deleteListener;
            this.clickListener = clickListener;
            this.existingImgCount = existingImgCount;
            this.existingVidCount = existingVidCount;
        }

        @NonNull
        @Override
        public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_preview, parent, false);
            return new MediaViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
            Object item = items.get(position);
            
            // Determine if it's a video
            boolean isVideo = false;
            if (item instanceof Uri) {
                String type = holder.itemView.getContext().getContentResolver().getType((Uri) item);
                isVideo = type != null && type.startsWith("video");
            } else {
                // If it's a URL, check position
                if (position >= existingImgCount && position < (existingImgCount + existingVidCount)) {
                    isVideo = true;
                }
            }

            String url = null;
            if (item instanceof String) {
                url = (String) item;
                if (!url.startsWith("http")) url = "https://server-testing-ymn9.onrender.com" + (url.startsWith("/") ? "" : "/") + url;
            }

            Glide.with(holder.itemView.getContext())
                    .load(url != null ? url : item)
                    .into(holder.imgPreview);
            
            holder.ivPlay.setVisibility(isVideo ? View.VISIBLE : View.GONE);
            holder.btnRemove.setOnClickListener(v -> deleteListener.onDelete(holder.getBindingAdapterPosition()));
            final boolean itemIsVideo = isVideo;
            holder.itemView.setOnClickListener(v -> clickListener.onMediaClick(item, itemIsVideo));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class MediaViewHolder extends RecyclerView.ViewHolder {
            ImageView imgPreview, btnRemove, ivPlay;
            public MediaViewHolder(@NonNull View itemView) {
                super(itemView);
                imgPreview = itemView.findViewById(R.id.imgPreview);
                btnRemove = itemView.findViewById(R.id.btnRemove);
                ivPlay = itemView.findViewById(R.id.ivPlay);
            }
        }
    }
}
