package com.project.muse_android.profile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.database.ShopMessage;
import com.project.muse_android.R;

import java.util.List;

public class ShopChatAdapter extends RecyclerView.Adapter<ShopChatAdapter.ShopChatViewHolder> {

    private final List<ShopMessage> messages;
    private final Context context;

    public ShopChatAdapter(Context context, List<ShopMessage> messages) {
        this.context = context;
        this.messages = messages;
    }

    @NonNull
    @Override
    public ShopChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop_chat_message, parent, false);
        return new ShopChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopChatViewHolder holder, int position) {
        ShopMessage msg = messages.get(position);
        if ("customer".equalsIgnoreCase(msg.getSender()) || "guest".equalsIgnoreCase(msg.getSender()) || "user".equalsIgnoreCase(msg.getSender())) {
            holder.layoutUser.setVisibility(View.VISIBLE);
            holder.layoutAdmin.setVisibility(View.GONE);
            holder.tvUserMessage.setText(msg.getContent());
        } else {
            holder.layoutUser.setVisibility(View.GONE);
            holder.layoutAdmin.setVisibility(View.VISIBLE);
            holder.tvAdminMessage.setText(msg.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ShopChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutAdmin;
        LinearLayout layoutUser;
        TextView tvAdminMessage;
        TextView tvUserMessage;

        public ShopChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutAdmin = itemView.findViewById(R.id.layoutAdmin);
            layoutUser = itemView.findViewById(R.id.layoutUser);
            tvAdminMessage = itemView.findViewById(R.id.tvAdminMessage);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);
        }
    }
}
