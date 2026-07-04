package com.project.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.muse_android.databinding.ItemSearchHistoryBinding;

import java.util.List;

public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder> {

    private final List<String> historyItems;
    private final OnHistoryClickListener listener;

    public interface OnHistoryClickListener {
        void onHistoryItemClick(String query);
        void onRemoveItemClick(String query, int position);
    }

    public SearchHistoryAdapter(List<String> historyItems, OnHistoryClickListener listener) {
        this.historyItems = historyItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSearchHistoryBinding binding = ItemSearchHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String query = historyItems.get(position);
        holder.binding.txtHistoryQuery.setText(query);
        
        holder.itemView.setOnClickListener(v -> listener.onHistoryItemClick(query));
        holder.binding.btnRemoveItem.setOnClickListener(v -> listener.onRemoveItemClick(query, position));
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ItemSearchHistoryBinding binding;

        public ViewHolder(@NonNull ItemSearchHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
