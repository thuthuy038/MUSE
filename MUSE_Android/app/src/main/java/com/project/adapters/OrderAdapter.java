package com.project.adapters;

import android.content.Context;
import android.widget.Toast;
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
            setupButtons(order);

            // 5. Navigate to Detail
            binding.getRoot().setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(context, com.project.muse_android.order.OrderDetailActivity.class);
                intent.putExtra("order", order);
                context.startActivity(intent);
            });
        }

        private void setupButtons(Order order) {
            // Hide all buttons first
            binding.btnContact.setVisibility(View.GONE);
            binding.btnTrack.setVisibility(View.GONE);
            binding.btnReturn.setVisibility(View.GONE);
            binding.btnReview.setVisibility(View.GONE);
            binding.btnCancelDetail.setVisibility(View.GONE);
            binding.btnReorder.setVisibility(View.GONE);

            String status = order.getStatus();
            if (status == null) return;

            String statusUpper = status.toUpperCase();

            // PENDING, PROCESSING, ĐANG XỬ LÝ, CHỜ LẤY HÀNG
            if (statusUpper.equals("PENDING") || statusUpper.equals("PROCESSING") 
                    || statusUpper.contains("XỬ LÝ") || statusUpper.contains("LẤY HÀNG")
                    || statusUpper.contains("XÁC NHẬN")) {
                binding.btnContact.setVisibility(View.VISIBLE);
                binding.btnContact.setOnClickListener(v -> {
                    Toast.makeText(context, "Đang kết nối liên hệ hỗ trợ đơn hàng " + order.getId(), Toast.LENGTH_SHORT).show();
                });
            }
            // SHIPPING, ĐANG GIAO HÀNG
            else if (statusUpper.equals("SHIPPING") || statusUpper.contains("GIAO")) {
                binding.btnTrack.setVisibility(View.VISIBLE);
                binding.btnTrack.setOnClickListener(v -> {
                    Toast.makeText(context, "Đang định vị theo dõi hành trình đơn hàng " + order.getId(), Toast.LENGTH_SHORT).show();
                });
            }
            // DELIVERED, COMPLETED, ĐÃ GIAO, HOÀN THÀNH
            else if (statusUpper.equals("DELIVERED") || statusUpper.equals("COMPLETED") 
                    || statusUpper.contains("ĐÃ GIAO") || statusUpper.contains("HOÀN THÀNH")) {
                binding.btnReturn.setVisibility(View.VISIBLE);
                binding.btnReview.setVisibility(View.VISIBLE);

                binding.btnReturn.setOnClickListener(v -> {
                    Toast.makeText(context, "Yêu cầu Trả hàng/Hoàn tiền đã gửi cho đơn " + order.getId(), Toast.LENGTH_SHORT).show();
                });
                binding.btnReview.setOnClickListener(v -> {
                    Toast.makeText(context, "Mở form đánh giá cho đơn hàng " + order.getId(), Toast.LENGTH_SHORT).show();
                });
            }
            // CANCELLED, ĐÃ HỦY
            else if (statusUpper.equals("CANCELLED") || statusUpper.contains("HỦY")) {
                binding.btnCancelDetail.setVisibility(View.VISIBLE);
                binding.btnReorder.setVisibility(View.VISIBLE);

                binding.btnCancelDetail.setOnClickListener(v -> {
                    Toast.makeText(context, "Đơn hàng " + order.getId() + " đã bị hủy thành công.", Toast.LENGTH_SHORT).show();
                });
                binding.btnReorder.setOnClickListener(v -> {
                    Toast.makeText(context, "Đã thêm lại các sản phẩm của đơn " + order.getId() + " vào giỏ hàng để mua lại", Toast.LENGTH_SHORT).show();
                });
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
