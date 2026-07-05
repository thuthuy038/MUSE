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

        holder.tvVoucherName.setText(voucher.getName());
        holder.tvMinOrder.setText("Đơn tối thiểu " + formatAmount(voucher.getMinOrderValue()));
        holder.tvExpiryDate.setText("HSD: " + voucher.getExpiryDate());

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

    public static class VoucherViewHolder extends RecyclerView.ViewHolder {
        TextView tvVoucherName, tvMinOrder, tvExpiryDate;
        ImageView ivSelect;

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVoucherName = itemView.findViewById(R.id.tvVoucherName);
            tvMinOrder = itemView.findViewById(R.id.tvMinOrder);
            tvExpiryDate = itemView.findViewById(R.id.tvExpiryDate);
            ivSelect = itemView.findViewById(R.id.ivSelect);
        }
    }
}