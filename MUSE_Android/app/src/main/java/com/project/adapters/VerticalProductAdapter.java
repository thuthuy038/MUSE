package com.project.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.models.Product;
import com.project.muse_android.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VerticalProductAdapter extends RecyclerView.Adapter<VerticalProductAdapter.ProductViewHolder> {

    private final Context context;
    private List<Product> productList = new ArrayList<>();
    private OnProductClickListener listener;

    // INTERFACE LINH HOẠT: Giúp định nghĩa hành vi tùy ý ở từng màn hình khác nhau
    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onFavoriteClick(Product product, int position);
    }

    // Constructor tối giản: Chỉ cần Context (Có thể set Listener sau hoặc truyền vào luôn)
    public VerticalProductAdapter(Context context) {
        this.context = context;
    }

    public VerticalProductAdapter(Context context, OnProductClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    // HÀM TÁI SỬ DỤNG: Dùng để cập nhật danh sách sản phẩm từ bất kỳ nguồn/màn hình nào
    public void setData(List<Product> newList) {
        this.productList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged(); // Cập nhật lại giao diện hiển thị
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_vertical, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        if (product == null) return;

        // 1. Đổ dữ liệu text cơ bản
        holder.txtName.setText(product.getName());
        holder.txtRating.setText(String.valueOf(product.getRating()));
        holder.txtSold.setText("| Đã bán " + product.getSold());

        // 2. Định dạng tiền tệ Việt Nam Đồng
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        // 3. Logic hiển thị giá (Có giảm giá hay không)
        if (product.getDiscountPercent() > 0) {
            holder.txtDiscount.setVisibility(View.VISIBLE);
            holder.txtDiscount.setText("-" + product.getDiscountPercent() + "%");
            holder.txtDiscountPrice.setText(currencyFormat.format(product.getDiscountPrice()));
            holder.txtPrice.setText(currencyFormat.format(product.getPrice()));
            holder.txtPrice.setVisibility(View.VISIBLE);
        } else {
            holder.txtDiscount.setVisibility(View.GONE);
            holder.txtPrice.setVisibility(View.GONE);
            holder.txtDiscountPrice.setText(currencyFormat.format(product.getPrice()));
        }

        // 4. Xử lý hình ảnh (Khuyên dùng Glide để tối ưu bộ nhớ khi cuộn danh sách)
        // Nếu chưa cài Glide, nhóm có thể tạm thời bỏ comment khi tích hợp:

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            String imageUrl = product.getImages().get(0).getUrl();
            // Nếu URL là đường dẫn tương đối (bắt đầu bằng /), thêm BASE_URL vào
            if (imageUrl != null && imageUrl.startsWith("/")) {
                imageUrl = "https://server-testing-ymn9.onrender.com" + imageUrl;
            }

            Glide.with(context)
                 .load(imageUrl)
                 .placeholder(R.drawable.demo_product)
                 .error(R.drawable.demo_product)
                 .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageResource(R.drawable.demo_product);
        }

        // 5. CHUYỂN GIAO SỰ KIỆN CLICK VỀ CHO MÀN HÌNH MẸ XỬ LÝ
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(product);
        });

        holder.imgFavorite.setOnClickListener(v -> {
            if (listener != null) listener.onFavoriteClick(product, position);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct, imgFavorite;
        TextView txtDiscount, txtName, txtDiscountPrice, txtPrice, txtRating, txtSold;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            imgFavorite = itemView.findViewById(R.id.imgFavorite);
            txtDiscount = itemView.findViewById(R.id.txtDiscount);
            txtName = itemView.findViewById(R.id.txtName);
            txtDiscountPrice = itemView.findViewById(R.id.txtDiscountPrice);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtRating = itemView.findViewById(R.id.txtRating);
            txtSold = itemView.findViewById(R.id.txtSold);

            // Gạch ngang giá gốc
            txtPrice.setPaintFlags(txtPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }
}