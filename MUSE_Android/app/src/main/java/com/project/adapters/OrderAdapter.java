package com.project.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.models.Order;
import com.project.models.Product;
import com.project.models.enums.HorizontalProductMode;
import com.project.muse_android.databinding.ItemOrderBinding;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private final Context context;
    private List<Order> orders;
    private final Set<String> expandedOrderIds = new HashSet<>();

    public OrderAdapter(Context context, List<Order> orders) {
        this.context = context;
        this.orders = orders;
    }

    public void setData(List<Order> orders) {
        this.orders = orders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderBinding binding = ItemOrderBinding.inflate(LayoutInflater.from(context), parent, false);
        return new OrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(orders.get(position));
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private final ItemOrderBinding binding;
        private final HorizontalProductAdapter productAdapter;

        public OrderViewHolder(ItemOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            
            // Initialize nested adapter once
            this.productAdapter = new HorizontalProductAdapter(context, HorizontalProductMode.READ_ONLY, null);
            binding.rvOrderProducts.setLayoutManager(new LinearLayoutManager(context));
            binding.rvOrderProducts.setAdapter(productAdapter);
        }

        public void bind(Order order) {
            // 1. Status Header
            String statusText = order.getStatus();
            binding.txtStatus.setText(mapStatusToText(statusText));

            // 2. Update nested adapter data
            List<Product> products = order.getProducts();
            boolean isExpanded = expandedOrderIds.contains(order.getId());

            if (products != null && products.size() > 1) {
                binding.layoutViewMore.setVisibility(View.VISIBLE);
                
                if (isExpanded) {
                    productAdapter.setData(products);
                    binding.ivExpandArrow.setRotation(180f);
                } else {
                    productAdapter.setData(products.subList(0, 1));
                    binding.ivExpandArrow.setRotation(0f);
                }

                binding.layoutViewMore.setOnClickListener(v -> {
                    if (isExpanded) {
                        expandedOrderIds.remove(order.getId());
                    } else {
                        expandedOrderIds.add(order.getId());
                    }
                    notifyItemChanged(getBindingAdapterPosition());
                });
            } else {
                binding.layoutViewMore.setVisibility(View.GONE);
                productAdapter.setData(products);
            }

            // 3. Total Price Footer
            int totalQty = 0;
            if (products != null) {
                for (Product p : products) {
                    totalQty += (p.getQuantity() > 0 ? p.getQuantity() : 1);
                }
            }
            String totalInfo = String.format(Locale.getDefault(), "Tổng số tiền(%d sản phẩm): %s", totalQty, formatPrice(order.getTotalPrice()));
            binding.txtTotalInfo.setText(totalInfo);

            // 4. Action Buttons based on Status
            setupButtons(order.getStatus());
        }

        private void setupButtons(String status) {
            // Hide all buttons first
            binding.btnContact.setVisibility(View.GONE);
            binding.btnTrack.setVisibility(View.GONE);
            binding.btnReturn.setVisibility(View.GONE);
            binding.btnReview.setVisibility(View.GONE);
            binding.btnCancelDetail.setVisibility(View.GONE);
            binding.btnReorder.setVisibility(View.GONE);

            if (status == null) return;

            switch (status.toUpperCase()) {
                case "PENDING":
                case "PROCESSING":
                    binding.btnContact.setVisibility(View.VISIBLE);
                    break;
                case "SHIPPING":
                    binding.btnTrack.setVisibility(View.VISIBLE);
                    break;
                case "DELIVERED":
                case "COMPLETED":
                    binding.btnReturn.setVisibility(View.VISIBLE);
                    binding.btnReview.setVisibility(View.VISIBLE);
                    break;
                case "CANCELLED":
                    binding.btnCancelDetail.setVisibility(View.VISIBLE);
                    binding.btnReorder.setVisibility(View.VISIBLE);
                    break;
            }
        }

        private String mapStatusToText(String status) {
            if (status == null) return "Chờ xác nhận";
            switch (status.toUpperCase()) {
                case "PENDING": return "Chờ xác nhận";
                case "PROCESSING": return "Chờ lấy hàng";
                case "SHIPPING": return "Chờ giao hàng";
                case "DELIVERED":
                case "COMPLETED": return "Đã giao";
                case "RETURNED": return "Trả hàng";
                case "CANCELLED": return "Đã hủy";
                default: return status;
            }
        }

        private String formatPrice(double price) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
            symbols.setGroupingSeparator('.');
            DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
            return decimalFormat.format(price) + "đ";
        }
    }
}
