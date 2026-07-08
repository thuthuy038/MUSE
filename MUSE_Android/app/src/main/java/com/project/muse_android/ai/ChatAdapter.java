package com.project.muse_android.ai;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.adapters.ProductAdapter;
import com.project.models.Product;
import com.project.muse_android.R;
import com.project.muse_android.product.ProductDetailActivity;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> messages;
    private final Context context;

    public ChatAdapter(Context context, List<ChatMessage> messages) {
        this.context = context;
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (msg.isUser()) {
            holder.layoutUser.setVisibility(View.VISIBLE);
            holder.layoutBot.setVisibility(View.GONE);
            holder.tvUserMessage.setText(msg.getText());
        } else {
            holder.layoutUser.setVisibility(View.GONE);
            holder.layoutBot.setVisibility(View.VISIBLE);
            holder.tvBotMessage.setText(msg.getText());

            if (msg.getSuggestedProducts() != null && !msg.getSuggestedProducts().isEmpty()) {
                holder.rvSuggestedProducts.setVisibility(View.VISIBLE);
                holder.rvSuggestedProducts.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                
                ProductAdapter productAdapter = new ProductAdapter(msg.getSuggestedProducts(), ProductAdapter.TYPE_HORIZONTAL, product -> {
                    Intent intent = new Intent(context, ProductDetailActivity.class);
                    intent.putExtra("product_id", product.get_id());
                    context.startActivity(intent);
                });
                holder.rvSuggestedProducts.setAdapter(productAdapter);
            } else {
                holder.rvSuggestedProducts.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutBot;
        LinearLayout layoutUser;
        TextView tvBotMessage;
        TextView tvUserMessage;
        RecyclerView rvSuggestedProducts;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutBot = itemView.findViewById(R.id.layoutBot);
            layoutUser = itemView.findViewById(R.id.layoutUser);
            tvBotMessage = itemView.findViewById(R.id.tvBotMessage);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);
            rvSuggestedProducts = itemView.findViewById(R.id.rvSuggestedProducts);
        }
    }
}
