package com.project.muse_android.product;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityFullScreenImageBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FullScreenImageActivity extends AppCompatActivity {

    private ActivityFullScreenImageBinding binding;
    private static final String BASE_URL = "https://server-testing-ymn9.onrender.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFullScreenImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        List<String> images = getIntent().getStringArrayListExtra("images");
        int position = getIntent().getIntExtra("position", 0);

        if (images == null) images = new ArrayList<>();

        binding.btnClose.setOnClickListener(v -> finish());

        ImagePagerAdapter adapter = new ImagePagerAdapter(images);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setCurrentItem(position, false);

        List<String> finalImages = images;
        binding.txtIndicator.setText(String.format(Locale.getDefault(), "%d/%d", position + 1, finalImages.size()));

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                binding.txtIndicator.setText(String.format(Locale.getDefault(), "%d/%d", position + 1, finalImages.size()));
            }
        });
    }

    private static class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ViewHolder> {
        private List<String> images;

        public ImagePagerAdapter(List<String> images) {
            this.images = images;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String url = images.get(position);
            if (url != null && !url.startsWith("http")) {
                url = BASE_URL + (url.startsWith("/") ? "" : "/") + url;
            }
            Glide.with(holder.itemView.getContext())
                    .load(url)
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            ViewHolder(ImageView itemView) {
                super(itemView);
                this.imageView = itemView;
            }
        }
    }
}
