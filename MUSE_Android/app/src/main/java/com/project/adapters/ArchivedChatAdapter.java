package com.project.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.models.ArchivedChat;
import com.project.muse_android.R;

import java.util.List;

public class ArchivedChatAdapter extends RecyclerView.Adapter<ArchivedChatAdapter.ViewHolder> {

    private final List<ArchivedChat> archivedChats;
    private final Context context;
    private final OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(ArchivedChat chat);
        void onDeleteClick(ArchivedChat chat, int position);
    }

    public ArchivedChatAdapter(Context context, List<ArchivedChat> archivedChats, OnChatClickListener listener) {
        this.context = context;
        this.archivedChats = archivedChats;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_archived_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArchivedChat chat = archivedChats.get(position);
        holder.tvChatTitle.setText(chat.getTitle());
        holder.tvChatDate.setText(chat.getDate());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onChatClick(chat);
        });

        holder.btnDeleteChat.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(chat, holder.getBindingAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return archivedChats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvChatTitle;
        TextView tvChatDate;
        ImageView btnDeleteChat;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatTitle = itemView.findViewById(R.id.tvChatTitle);
            tvChatDate = itemView.findViewById(R.id.tvChatDate);
            btnDeleteChat = itemView.findViewById(R.id.btnDeleteChat);
        }
    }
}
