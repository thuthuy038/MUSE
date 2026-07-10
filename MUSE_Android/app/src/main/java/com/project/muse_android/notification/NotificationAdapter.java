package com.project.muse_android.notification;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import androidx.core.content.ContextCompat;
import com.project.models.Notification;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ItemNotificationBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final Context context;
    private List<Notification> notificationList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification, int position);
    }

    private OnNotificationClickListener clickListener;

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.clickListener = listener;
    }

    public NotificationAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<Notification> list) {
        this.notificationList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNotificationBinding binding;

        public ViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onNotificationClick(notificationList.get(position), position);
                }
            });
        }

        public void bind(Notification n) {
            // Hide all first
            binding.layoutPromotion.setVisibility(View.GONE);
            binding.layoutSystem.setVisibility(View.GONE);
            binding.layoutOrder.setVisibility(View.GONE);

            int bgColor = "unread".equals(n.getStatus()) 
                    ? ContextCompat.getColor(context, R.color.unread_bg) 
                    : ContextCompat.getColor(context, R.color.white);

            binding.layoutPromotion.setBackgroundColor(bgColor);
            binding.layoutSystem.setBackgroundColor(bgColor);
            binding.layoutOrder.setCardBackgroundColor(bgColor);

            String dateStr = n.getCreatedAt() != null ? dateFormat.format(n.getCreatedAt()) : "";

            switch (n.getType()) {
                case "promotion":
                case "stock":
                    binding.layoutPromotion.setVisibility(View.VISIBLE);
                    binding.txtPromoTitle.setText(n.getTitle());
                    binding.txtPromoMessage.setText(n.getMessage());
                    binding.txtPromoDate.setText(dateStr);
                    break;
                case "system":
                case "review":
                    binding.layoutSystem.setVisibility(View.VISIBLE);
                    binding.txtSystemMessage.setText(n.getMessage());
                    binding.txtSystemDate.setText(dateStr);
                    break;
                case "order":
                    binding.layoutOrder.setVisibility(View.VISIBLE);
                    binding.txtOrderTitle.setText(n.getTitle());
                    binding.txtOrderMessage.setText(n.getMessage());
                    binding.txtOrderDate.setText(dateStr);
                    break;
            }
        }
    }
}
