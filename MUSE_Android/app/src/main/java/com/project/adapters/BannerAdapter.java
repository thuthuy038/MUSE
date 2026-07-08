package com.project.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.models.Banner;
import com.project.muse_android.R;

import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.ViewHolder> {

    public interface OnBannerClickListener {
        void onBannerClick(Banner banner);
    }

    private List<Banner> banners;
    private OnBannerClickListener listener;
    private static final String BASE_URL = "https://server-testing-ymn9.onrender.com";

    public BannerAdapter(List<Banner> banners) {
        this.banners = banners;
    }

    public BannerAdapter(List<Banner> banners, OnBannerClickListener listener) {
        this.banners = banners;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Banner banner = banners.get(position);
        String imageUrl = getFullImageUrl(banner.getImage());

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.imgBanner);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBannerClick(banner);
            }
        });
    }

    private String getFullImageUrl(String path) {
        if (path == null || path.isEmpty()) return null;
        if (path.startsWith("http")) return path;
        if (path.length() == 24) return BASE_URL + "/api/images/" + path;
        if (path.startsWith("/")) return BASE_URL + path;
        return BASE_URL + "/" + path;
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBanner;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBanner = itemView.findViewById(R.id.imgBannerItem);
        }
    }
}
