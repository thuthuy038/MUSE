package com.project.muse_android.product;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.models.Order;
import com.project.models.ProductReview;
import com.project.muse_android.databinding.ActivityWriteReviewBinding;
import com.project.network.ApiClient;
import com.project.network.ApiResponse;
import com.project.utils.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WriteReviewActivity extends AppCompatActivity {

    private ActivityWriteReviewBinding binding;
    private WriteReviewAdapter adapter;
    private Order order;
    private int currentUploadPosition = -1;
    private SessionManager sessionManager;

    private boolean isEditMode = false;
    private ProductReview existingReview;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(10), uris -> {
                if (uris != null && !uris.isEmpty() && currentUploadPosition != -1) {
                    for (Uri uri : uris) {
                        adapter.updateMedia(currentUploadPosition, uri);
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWriteReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        isEditMode = getIntent().getBooleanExtra("is_edit", false);
        String reviewJson = getIntent().getStringExtra("review_json");
        if (reviewJson != null) {
            existingReview = new com.google.gson.Gson().fromJson(reviewJson, ProductReview.class);
        }
        order = (Order) getIntent().getSerializableExtra("order");

        if (isEditMode && existingReview != null) {
            setupEditMode();
        } else if (order == null || order.getItems() == null) {
            Toast.makeText(this, "Không tìm thấy dữ liệu đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
    }

    private void setupEditMode() {
        binding.txtTitle.setText("Sửa đánh giá");
        binding.btnSubmit.setText("CẬP NHẬT ĐÁNH GIÁ");

        // If we don't have an order object but have review data, create a minimal order/item structure
        if (order == null) {
            order = new Order();
            order.set_id(existingReview.getOrderId());
            List<Order.OrderItem> items = new ArrayList<>();
            Order.OrderItem item = new Order.OrderItem();
            item.setProductId(existingReview.getProductId());
            item.setName(existingReview.getProductName());
            item.setImage(existingReview.getProductImage());
            item.setSize(existingReview.getSize());
            item.setColor(existingReview.getColor());
            items.add(item);
            order.setItems(items);
        }
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new WriteReviewAdapter(this, order.getItems(), position -> {
            currentUploadPosition = position;
            pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                    .build());
        });

        if (isEditMode && existingReview != null) {
            adapter.setInitialState(0, existingReview.getRating(), existingReview.getContent(),
                    existingReview.getImages(), existingReview.getVideos());
        }

        adapter.setOnMediaClickListener(new WriteReviewAdapter.OnMediaClickListener() {
            @Override
            public void onMediaClick(Uri uri, boolean isVideo) {
                openMedia(uri.toString(), isVideo);
            }
            @Override
            public void onUrlClick(String url, boolean isVideo) {
                openMedia(url, isVideo);
            }
        });

        adapter.setOnProductClickListener(item -> {
            Intent intent = new Intent(this, com.project.muse_android.product.ProductDetailActivity.class);
            intent.putExtra("product_id", item.getProductId());
            startActivity(intent);
        });

        binding.rvReviewForms.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReviewForms.setAdapter(adapter);

        binding.btnSubmit.setOnClickListener(v -> submitAllReviews());
    }

    private void openMedia(String path, boolean isVideo) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String fullPath = path;
        if (!fullPath.startsWith("http") && !fullPath.startsWith("content")) {
            fullPath = "https://server-testing-ymn9.onrender.com" + (fullPath.startsWith("/") ? "" : "/") + fullPath;
        }
        intent.setDataAndType(Uri.parse(fullPath), isVideo ? "video/*" : "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Không tìm thấy trình phát phù hợp", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitAllReviews() {
        binding.btnSubmit.setEnabled(false);
        binding.btnSubmit.setText("ĐANG GỬI...");

        final List<Order.OrderItem> items = order.getItems();
        final Map<Integer, WriteReviewAdapter.ReviewState> results = adapter.getReviewResults();
        final String token = sessionManager.getToken();
        final String userId = sessionManager.getUserId();

        if (token == null || userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            resetSubmitButton();
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            if (results.get(i).rating == 0) {
                Toast.makeText(this, "Vui lòng chọn số sao cho: " + items.get(i).getName(), Toast.LENGTH_SHORT).show();
                resetSubmitButton();
                return;
            }
        }

        processItemRecursively(0, items, results, token, userId);
    }

    private void processItemRecursively(final int index, final List<Order.OrderItem> items, 
                                       final Map<Integer, WriteReviewAdapter.ReviewState> results, 
                                       final String token, final String userId) {
        if (index >= items.size()) {
            onAllDone();
            return;
        }

        final Order.OrderItem item = items.get(index);
        final WriteReviewAdapter.ReviewState state = results.get(index);

        uploadMediaBatch(state.selectedMedia, new MediaUploadCallback() {
            @Override
            public void onComplete(List<Map<String, String>> images, List<Map<String, String>> videos) {
                // Combine existing media with newly uploaded ones
                List<Map<String, String>> finalImages = new ArrayList<>();
                for (String url : state.existingImageUrls) {
                    Map<String, String> m = new HashMap<>();
                    m.put("url", url);
                    finalImages.add(m);
                }
                finalImages.addAll(images);

                List<Map<String, String>> finalVideos = new ArrayList<>();
                for (String url : state.existingVideoUrls) {
                    Map<String, String> m = new HashMap<>();
                    m.put("url", url);
                    finalVideos.add(m);
                }
                finalVideos.addAll(videos);

                Map<String, Object> body = new HashMap<>();
                body.put("userId", userId);
                body.put("productId", item.getProductId());
                body.put("orderId", order.get_id());
                body.put("rating", state.rating);
                body.put("content", state.comment);
                body.put("size", item.getSize());
                body.put("color", item.getColor());
                body.put("images", finalImages);
                body.put("videos", finalVideos);

                Callback<ApiResponse<ProductReview>> callback = new Callback<ApiResponse<ProductReview>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ProductReview>> call, @NonNull Response<ApiResponse<ProductReview>> response) {
                        processItemRecursively(index + 1, items, results, token, userId);
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ProductReview>> call, @NonNull Throwable t) {
                        processItemRecursively(index + 1, items, results, token, userId);
                    }
                };

                if (isEditMode && existingReview != null) {
                    ApiClient.INSTANCE.getInstance().updateReview(existingReview.get_id(), "Bearer " + token, body).enqueue(callback);
                } else {
                    ApiClient.INSTANCE.getInstance().postReview("Bearer " + token, body).enqueue(callback);
                }
            }
        });
    }

    private void onAllDone() {
        getSharedPreferences("MUSE_PREFS", MODE_PRIVATE)
                .edit().putBoolean("reviewed_" + order.get_id(), true).apply();

        Toast.makeText(this, "Gửi đánh giá thành công!", Toast.LENGTH_LONG).show();
        Intent intent = new Intent();
        intent.putExtra("order_id", order.get_id());
        intent.putExtra("is_reviewed", true);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void resetSubmitButton() {
        binding.btnSubmit.setEnabled(true);
        binding.btnSubmit.setText(isEditMode ? "CẬP NHẬT ĐÁNH GIÁ" : "GỬI");
    }

    private interface MediaUploadCallback {
        void onComplete(List<Map<String, String>> images, List<Map<String, String>> videos);
    }

    private void uploadMediaBatch(List<Uri> uris, final MediaUploadCallback callback) {
        final List<Map<String, String>> uploadedImages = new ArrayList<>();
        final List<Map<String, String>> uploadedVideos = new ArrayList<>();

        if (uris == null || uris.isEmpty()) {
            callback.onComplete(uploadedImages, uploadedVideos);
            return;
        }

        final int[] count = {0};
        final int total = uris.size();

        for (final Uri uri : uris) {
            String mimeType = getContentResolver().getType(uri);
            final boolean isVideo = mimeType != null && mimeType.startsWith("video");

            File file = uriToFile(uri);
            if (file == null) {
                checkCount(count, total, uploadedImages, uploadedVideos, callback);
                continue;
            }

            RequestBody requestFile = RequestBody.create(file, MediaType.parse(mimeType != null ? mimeType : "image/jpeg"));
            MultipartBody.Part bodyPart = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

            ApiClient.INSTANCE.getInstance().uploadMedia(bodyPart).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String fileId = response.body().get("fileId");
                        Map<String, String> mediaObj = new HashMap<>();
                        mediaObj.put("fileId", fileId);
                        mediaObj.put("url", "/api/images/" + fileId);
                        
                        synchronized (uploadedImages) {
                            if (isVideo) uploadedVideos.add(mediaObj);
                            else uploadedImages.add(mediaObj);
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(WriteReviewActivity.this, "Lỗi tải tệp lên server", Toast.LENGTH_SHORT).show());
                    }
                    checkCount(count, total, uploadedImages, uploadedVideos, callback);
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                    runOnUiThread(() -> Toast.makeText(WriteReviewActivity.this, "Lỗi kết nối khi tải tệp", Toast.LENGTH_SHORT).show());
                    checkCount(count, total, uploadedImages, uploadedVideos, callback);
                }
            });
        }
    }

    private void checkCount(int[] count, int total, List<Map<String, String>> imgs, List<Map<String, String>> vids, MediaUploadCallback callback) {
        int current;
        synchronized (count) {
            count[0]++;
            current = count[0];
        }
        if (current == total) {
            callback.onComplete(imgs, vids);
        }
    }

    private File uriToFile(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;

            String fileName = "upload_" + System.currentTimeMillis();
            String mimeType = getContentResolver().getType(uri);
            String extension = mimeType != null && mimeType.contains("/") ? "." + mimeType.split("/")[1] : ".jpg";

            File tempFile = File.createTempFile(fileName, extension, getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.flush();
            fos.close();
            is.close();
            return tempFile;
        } catch (Exception e) {
            Log.e("WriteReview", "Uri to File error: " + e.getMessage(), e);
            return null;
        }
    }
}
