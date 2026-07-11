package com.project.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.project.models.Product;
import com.project.models.OutfitSet;
import com.project.models.SavedOutfit;
import com.project.utils.AiStorageManager;
import com.project.muse_android.R;
import com.project.muse_android.product.ProductDetailActivity;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class OutfitSetAdapter extends RecyclerView.Adapter<OutfitSetAdapter.OutfitViewHolder> {

    private final List<OutfitSet> outfitSets;
    private final Context context;
    private static final String BASE_URL = "https://server-testing-ymn9.onrender.com";

    public OutfitSetAdapter(Context context, List<OutfitSet> outfitSets) {
        this.context = context;
        this.outfitSets = outfitSets;
    }

    @NonNull
    @Override
    public OutfitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_outfit_set, parent, false);
        return new OutfitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OutfitViewHolder holder, int position) {
        OutfitSet set = outfitSets.get(position);
        holder.tvSetName.setText(set.getName());
        holder.tvSetDescription.setText(set.getDescription());

        // Bind Top Product
        Product top = set.getTop();
        holder.tvTopName.setText(top.getName());
        holder.tvTopPrice.setText(formatPrice(top.getPrice()));
        loadImage(holder.ivTopImage, top);
        holder.cardTop.setOnClickListener(v -> openProductDetail(top));

        // Bind Bottom Product
        Product bottom = set.getBottom();
        holder.tvBottomName.setText(bottom.getName());
        holder.tvBottomPrice.setText(formatPrice(bottom.getPrice()));
        loadImage(holder.ivBottomImage, bottom);
        holder.cardBottom.setOnClickListener(v -> openProductDetail(bottom));

        // Save Set button click listener
        holder.btnSaveSet.setOnClickListener(v -> {
            String topUrl = (top.getImages() != null && !top.getImages().isEmpty()) ? top.getImages().get(0).getUrl() : "";
            String bottomUrl = (bottom.getImages() != null && !bottom.getImages().isEmpty()) ? bottom.getImages().get(0).getUrl() : "";
            String todayDate = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());

            SavedOutfit saved = new SavedOutfit(
                set.getName(),
                top.getName(),
                top.getPrice(),
                topUrl,
                top.get_id(),
                bottom.getName(),
                bottom.getPrice(),
                bottomUrl,
                bottom.get_id(),
                todayDate
            );

            AiStorageManager.saveOutfit(context, saved);
            Toast.makeText(context, "Đã lưu bộ phối đồ \"" + set.getName() + "\" vào mục Yêu thích! ✨", Toast.LENGTH_LONG).show();
        });
    }

    private void openProductDetail(Product product) {
        Intent intent = new Intent(context, ProductDetailActivity.class);
        intent.putExtra("product_id", product.get_id());
        context.startActivity(intent);
    }

    private void loadImage(ImageView imageView, Product product) {
        String imageUrl = null;
        if (product.getImages() != null && !product.getImages().isEmpty() && product.getImages().get(0) != null) {
            imageUrl = product.getImages().get(0).getUrl();
        }

        if (imageUrl != null) {
            if (!imageUrl.startsWith("http")) {
                imageUrl = BASE_URL + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
            }
            Glide.with(imageView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(imageView);
        }
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(price) + " đ";
    }

    @Override
    public int getItemCount() {
        return outfitSets.size();
    }

    static class OutfitViewHolder extends RecyclerView.ViewHolder {
        TextView tvSetName;
        TextView tvSetDescription;
        View cardTop;
        ImageView ivTopImage;
        TextView tvTopName;
        TextView tvTopPrice;
        View cardBottom;
        ImageView ivBottomImage;
        TextView tvBottomName;
        TextView tvBottomPrice;
        MaterialButton btnSaveSet;

        public OutfitViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSetName = itemView.findViewById(R.id.tvSetName);
            tvSetDescription = itemView.findViewById(R.id.tvSetDescription);
            cardTop = itemView.findViewById(R.id.cardTop);
            ivTopImage = itemView.findViewById(R.id.ivTopImage);
            tvTopName = itemView.findViewById(R.id.tvTopName);
            tvTopPrice = itemView.findViewById(R.id.tvTopPrice);
            cardBottom = itemView.findViewById(R.id.cardBottom);
            ivBottomImage = itemView.findViewById(R.id.ivBottomImage);
            tvBottomName = itemView.findViewById(R.id.tvBottomName);
            tvBottomPrice = itemView.findViewById(R.id.tvBottomPrice);
            btnSaveSet = itemView.findViewById(R.id.btnSaveSet);
        }
    }
}
