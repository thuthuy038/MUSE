package com.project.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.project.models.Voucher;
import com.project.muse_android.R;

import java.util.ArrayList;
import java.util.List;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

    private final Context context;
    private List<Voucher> voucherList = new ArrayList<>();
    private final OnVoucherClickListener listener;

    public interface OnVoucherClickListener {
        void onVoucherClick(Voucher voucher, int position);
    }

    public VoucherAdapter(Context context, OnVoucherClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<Voucher> list) {
        this.voucherList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        Voucher voucher = voucherList.get(position);
        if (voucher == null) return;

        holder.tvVoucherCode.setText(voucher.getCode());
        holder.tvVoucherName.setText(voucher.getName());
        holder.tvMinOrder.setText("Đơn tối thiểu " + formatAmount(voucher.getMinOrderValue()));
        holder.tvExpiryDate.setText("HSD: " + formatDate(voucher.getExpiryDate()));

        if (voucher.isSelected()) {
            holder.ivSelect.setImageResource(R.drawable.ic_check_circle);
            holder.ivSelect.setColorFilter(ContextCompat.getColor(context, R.color.primary_500));
        } else {
            holder.ivSelect.setImageResource(R.drawable.ic_check_circle); // Or a circle outline
            holder.ivSelect.setColorFilter(ContextCompat.getColor(context, R.color.neutral_300));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVoucherClick(voucher, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return voucherList.size();
    }

    private String formatAmount(double amount) {
        if (amount >= 1000) {
            return (int) (amount / 1000) + "k";
        }
        return String.valueOf((int) amount);
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = inputFormat.parse(dateStr);
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.US);
            return outputFormat.format(date);
        } catch (Exception e) {
            try {
                java.text.SimpleDateFormat altFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                java.util.Date date = altFormat.parse(dateStr);
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.US);
                return outputFormat.format(date);
            } catch (Exception ex) {
                return dateStr;
            }
        }
    }

    public static class VoucherViewHolder extends RecyclerView.ViewHolder {
        TextView tvVoucherName, tvMinOrder, tvExpiryDate, tvVoucherCode;
        ImageView ivSelect;

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVoucherName = itemView.findViewById(R.id.tvVoucherName);
            tvMinOrder = itemView.findViewById(R.id.tvMinOrder);
            tvExpiryDate = itemView.findViewById(R.id.tvExpiryDate);
            tvVoucherCode = itemView.findViewById(R.id.tvVoucherCode);
            ivSelect = itemView.findViewById(R.id.ivSelect);
        }
    }
}