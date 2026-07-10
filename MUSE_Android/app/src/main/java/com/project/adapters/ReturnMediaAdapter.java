package com.project.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.muse_android.R;

import java.util.List;

public class ReturnMediaAdapter extends RecyclerView.Adapter<ReturnMediaAdapter.ViewHolder> {

    public interface OnMediaActionListener {
        void onDelete(int position);
        void onMediaClick(Uri uri, boolean isVideo);
    }

    private final Context context;
    private final List<Uri> mediaUris;
    private final OnMediaActionListener listener;

    public ReturnMediaAdapter(Context context, List<Uri> mediaUris, OnMediaActionListener listener) {
        this.context = context;
        this.mediaUris = mediaUris;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_media_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri uri = mediaUris.get(position);
        
        Glide.with(context)
                .load(uri)
                .centerCrop()
                .placeholder(R.drawable.image)
                .into(holder.imgPreview);

        String type = context.getContentResolver().getType(uri);
        boolean isVideo = type != null && type.startsWith("video");
        holder.ivPlay.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(holder.getBindingAdapterPosition());
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onMediaClick(uri, isVideo);
        });
    }

    @Override
    public int getItemCount() {
        return mediaUris.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPreview, btnRemove, ivPlay;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPreview = itemView.findViewById(R.id.imgPreview);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            ivPlay = itemView.findViewById(R.id.ivPlay);
        }
    }
}
