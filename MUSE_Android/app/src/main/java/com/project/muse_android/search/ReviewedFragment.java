package com.project.muse_android.search;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.models.ProductReview;
import com.project.models.ReviewResponse;
import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentReviewedBinding;
import com.project.muse_android.product.ProductDetailActivity;
import com.project.network.ApiClient;
import com.project.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewedFragment extends Fragment {

    private FragmentReviewedBinding binding;
    private ReviewedAdapter adapter;
    private SessionManager sessionManager;
    private final List<ProductReview> reviews = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReviewedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        setupRecyclerView();
        fetchUserReviews();
    }

    private void setupRecyclerView() {
        adapter = new ReviewedAdapter(reviews, new ReviewedAdapter.OnReviewItemClickListener() {
            @Override
            public void onProductClick(ProductReview review) {
                Intent intent = new Intent(getActivity(), ProductDetailActivity.class);
                intent.putExtra("product_id", review.getProductId());
                startActivity(intent);
            }

            @Override
            public void onEditClick(ProductReview review) {
                // Pass as JSON string to avoid Serialization issues with JsonElement fields
                String reviewJson = new com.google.gson.Gson().toJson(review);
                Intent intent = new Intent(getActivity(), com.project.muse_android.product.WriteReviewActivity.class);
                intent.putExtra("is_edit", true);
                intent.putExtra("review_json", reviewJson);
                startActivity(intent);
            }
        });
        binding.rvReviewed.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvReviewed.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchUserReviews();
    }

    private void fetchUserReviews() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        ApiClient.INSTANCE.getInstance().getUserReviews(userId).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReviewResponse> call, @NonNull Response<ReviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    reviews.clear();
                    if (response.body().getData() != null) {
                        reviews.addAll(response.body().getData());
                    }
                    adapter.notifyDataSetChanged();
                    binding.emptyState.setVisibility(reviews.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReviewResponse> call, @NonNull Throwable t) {
                if (isAdded()) binding.emptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private static class ReviewedAdapter extends RecyclerView.Adapter<ReviewedAdapter.ViewHolder> {
        private final List<ProductReview> items;
        private final OnReviewItemClickListener listener;

        interface OnReviewItemClickListener {
            void onProductClick(ProductReview review);
            void onEditClick(ProductReview review);
        }

        ReviewedAdapter(List<ProductReview> items, OnReviewItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reviewed, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductReview review = items.get(position);
            holder.tvName.setText(review.getProductName());
            holder.tvContent.setText(review.getContent());
            holder.ratingBar.setRating((float) review.getRating());

            // Format date
            String rawDate = review.getCreatedAt();
            if (rawDate != null && rawDate.length() >= 10) {
                holder.tvDate.setText(rawDate.substring(0, 10).replace("-", "/"));
            }

            String url = review.getProductImage();
            if (url != null) {
                if (!url.startsWith("http")) url = "https://server-testing-ymn9.onrender.com" + (url.startsWith("/") ? "" : "/") + url;
                Glide.with(holder.itemView.getContext()).load(url).placeholder(R.drawable.demo_product).into(holder.ivProduct);
            }

            // Check if review is within 7 days
            if (isWithin7Days(rawDate)) {
                holder.btnEdit.setVisibility(View.VISIBLE);
                holder.btnEdit.setOnClickListener(v -> listener.onEditClick(review));
            } else {
                holder.btnEdit.setVisibility(View.GONE);
            }

            holder.productArea.setOnClickListener(v -> listener.onProductClick(review));
        }

        private boolean isWithin7Days(String dateStr) {
            if (dateStr == null) return false;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date reviewDate = sdf.parse(dateStr);
                if (reviewDate == null) return false;

                long diff = System.currentTimeMillis() - reviewDate.getTime();
                long days = diff / (24 * 60 * 60 * 1000);
                return days <= 7;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivProduct;
            TextView tvName, tvContent, tvDate;
            RatingBar ratingBar;
            View btnEdit, productArea;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivProduct = itemView.findViewById(R.id.ivProductImage);
                tvName = itemView.findViewById(R.id.tvProductName);
                tvContent = itemView.findViewById(R.id.tvReviewContent);
                tvDate = itemView.findViewById(R.id.tvReviewDate);
                ratingBar = itemView.findViewById(R.id.ratingBar);
                btnEdit = itemView.findViewById(R.id.btnEditReview);
                // The horizontal linear layout containing image and text
                productArea = itemView.findViewById(R.id.productInfoLayout);
            }
        }
    }
}
