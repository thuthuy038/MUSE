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
        order = (Order) getIntent().getSerializableExtra("order");
        if (order == null || order.getItems() == null) {
            Toast.makeText(this, "Không tìm thấy dữ liệu đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new WriteReviewAdapter(this, order.getItems(), position -> {
            currentUploadPosition = position;
            pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                    .build());
        });

        adapter.setOnMediaClickListener((uri, isVideo) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, isVideo ? "video/*" : "image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Không tìm thấy trình phát phù hợp", Toast.LENGTH_SHORT).show();
            }
        });

        binding.rvReviewForms.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReviewForms.setAdapter(adapter);

        binding.btnSubmit.setOnClickListener(v -> submitAllReviews());
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
                Map<String, Object> body = new HashMap<>();
                body.put("userId", userId);
                body.put("productId", item.getProductId());
                body.put("orderId", order.get_id());
                body.put("rating", state.rating);
                body.put("content", state.comment);
                body.put("size", item.getSize());
                body.put("color", item.getColor());
                body.put("images", images);
                body.put("videos", videos);

                Log.d("WriteReview", "Submitting review for item " + index + " with " + images.size() + " images");

                ApiClient.INSTANCE.getInstance().postReview("Bearer " + token, body).enqueue(new Callback<ApiResponse<ProductReview>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ProductReview>> call, @NonNull Response<ApiResponse<ProductReview>> response) {
                        if (!response.isSuccessful()) {
                            Log.e("WriteReview", "Failed to submit review for item " + index + ": " + response.code());
                        }
                        processItemRecursively(index + 1, items, results, token, userId);
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ProductReview>> call, @NonNull Throwable t) {
                        Log.e("WriteReview", "Error submitting review for item " + index, t);
                        processItemRecursively(index + 1, items, results, token, userId);
                    }
                });
            }
        });
    }

    private void onAllDone() {
        // Save reviewed status locally
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
        binding.btnSubmit.setText("GỬI");
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
                Log.e("WriteReview", "Could not convert Uri to file: " + uri);
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
                        Log.d("WriteReview", "Uploaded media success: " + fileId);
                    } else {
                        Log.e("WriteReview", "Upload media failed: " + response.code());
                    }
                    checkCount(count, total, uploadedImages, uploadedVideos, callback);
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                    Log.e("WriteReview", "Upload media error", t);
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
            File tempFile = new File(getCacheDir(), "upload_" + System.currentTimeMillis() + "_" + (uri.getLastPathSegment() != null ? uri.getLastPathSegment().replaceAll("[^a-zA-Z0-9.]", "_") : "file"));
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.close();
            is.close();
            return tempFile;
        } catch (Exception e) {
            Log.e("WriteReview", "Uri to File error", e);
            return null;
        }
    }
}
