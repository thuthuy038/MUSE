package com.project.muse_android.ai;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.project.models.Product;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityVirtualFittingBinding;
import com.project.network.HomeApiClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VirtualFittingActivity extends AppCompatActivity {
    private ActivityVirtualFittingBinding binding;
    private boolean isScanMode = false;
    private boolean isTryOnMode = false;
    private String productId = "";
    private String selectedSize = "";
    private String selectedColor = "";
    private boolean fromCart = false;
    private String capturedPhotoPath = "";
    private Uri currentPhotoUri = null;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private ObjectAnimator laserAnimator;

    // Activity launchers
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Quyền mở Camera bị từ chối.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), isSuccess -> {
                if (isSuccess && capturedPhotoPath != null && !capturedPhotoPath.isEmpty()) {
                    onPhotoSelected(Uri.fromFile(new File(capturedPhotoPath)));
                }
            });

    private final ActivityResultLauncher<String> selectImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    String localPath = copyUriToTempFile(uri);
                    if (!localPath.isEmpty()) {
                        capturedPhotoPath = localPath;
                        onPhotoSelected(uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVirtualFittingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Fix header overlap
        com.project.utils.ViewUtils.applySystemBarsPadding(binding.layoutHeader, true, false);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAiAgent.setOnClickListener(v -> navigateToAiHub());

        // Hub Navigation Setup
        binding.btnFittingCart.setOnClickListener(v -> {
            Intent intent = new Intent(this, FittingCartActivity.class);
            startActivity(intent);
        });

        binding.btnFittingSearch.setOnClickListener(v -> {
            Intent intent = new Intent(this, FittingSearchActivity.class);
            startActivity(intent);
        });

        // Setup Bottom Navigation
        com.project.utils.ViewUtils.setupBottomNavigation(binding.bottomNavigationView, this);

        // Read Intent State routing
        Intent intent = getIntent();
        if (intent != null) {
            isScanMode = intent.getBooleanExtra("outfit_scan", false);
            productId = intent.getStringExtra("product_id");
            selectedSize = intent.getStringExtra("size");
            selectedColor = intent.getStringExtra("color");
            fromCart = intent.getBooleanExtra("from_cart", false);
            if (productId != null && !productId.isEmpty()) {
                isTryOnMode = true;
            }
        }

        // Configure UI based on Mode Routing
        configureModeRouting();

        // Capture actions setup
        setupCaptureButtons();
    }

    private void navigateToAiHub() {
        Intent intent = new Intent(this, com.project.muse_android.main.MainActivity.class);
        intent.putExtra("open_ai_hub", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void configureModeRouting() {
        if (isTryOnMode || isScanMode) {
            binding.layoutHubContent.setVisibility(View.GONE);
            binding.layoutCaptureContent.setVisibility(View.VISIBLE);

            if (isTryOnMode) {
                binding.tvCaptureHeader.setText("AI OUTFIT FITTING ROOM");
                binding.btnStartAnalysis.setText("TIẾN HÀNH THỬ ĐỒ VỚI AI");
                binding.cardTargetProduct.setVisibility(View.VISIBLE);
                loadTargetProductDetails();
            } else {
                binding.tvCaptureHeader.setText("AI OUTFIT SCANNER");
                binding.btnStartAnalysis.setText("TIẾN HÀNH QUÉT TRANG PHỤC");
                binding.cardTargetProduct.setVisibility(View.GONE);
            }
        } else {
            binding.layoutHubContent.setVisibility(View.VISIBLE);
            binding.layoutCaptureContent.setVisibility(View.GONE);
        }
    }

    private void loadTargetProductDetails() {
        HomeApiClient.getHomeApiService().searchProducts("").enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Product p : response.body()) {
                        String pid = p.get_id() != null ? p.get_id() : p.getId();
                        if (productId.equals(pid)) {
                            binding.tvTargetProductName.setText(p.getName());
                            binding.tvTargetProductVariant.setText(
                                    String.format("Size %s — Màu %s", 
                                            selectedSize != null ? selectedSize : "Mặc định", 
                                            selectedColor != null ? selectedColor : "Mặc định")
                            );

                            String imageUrl = "";
                            if (p.getImages() != null && !p.getImages().isEmpty()) {
                                imageUrl = p.getImages().get(0).getUrl();
                            }
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                if (!imageUrl.startsWith("http")) {
                                    imageUrl = "https://server-testing-ymn9.onrender.com" + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
                                }
                                Glide.with(VirtualFittingActivity.this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.demo_product)
                                        .into(binding.ivTargetProductImage);
                            }
                            break;
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                // Fail silently
            }
        });
    }

    private void setupCaptureButtons() {
        binding.btnCaptureCamera.setOnClickListener(v -> {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        });

        binding.btnSelectGallery.setOnClickListener(v -> {
            selectImageLauncher.launch("image/*");
        });

        binding.btnStartAnalysis.setOnClickListener(v -> {
            if (capturedPhotoPath == null || capturedPhotoPath.isEmpty()) {
                Toast.makeText(this, "Vui lòng chụp ảnh hoặc chọn ảnh chân dung trước khi phân tích.", Toast.LENGTH_SHORT).show();
                return;
            }
            startAIScanningAnimation();
        });
    }

    private void openCamera() {
        try {
            File tempFile = File.createTempFile("model_capture_", ".jpg", getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES));
            capturedPhotoPath = tempFile.getAbsolutePath();
            currentPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempFile);
            takePictureLauncher.launch(currentPhotoUri);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tạo file lưu ảnh.", Toast.LENGTH_SHORT).show();
        }
    }

    private void onPhotoSelected(Uri uri) {
        binding.layoutPlaceholder.setVisibility(View.GONE);
        binding.ivModelPreview.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.demo_product)
                .into(binding.ivModelPreview);
    }

    private String copyUriToTempFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("gallery_select_", ".jpg", getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            return tempFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void startAIScanningAnimation() {
        binding.layoutProcessingContent.setVisibility(View.VISIBLE);
        
        // Load user photo in scanning background
        if (capturedPhotoPath != null) {
            Glide.with(this)
                    .load(capturedPhotoPath)
                    .placeholder(R.drawable.demo_product)
                    .into(binding.ivScanBodyPreview);
        }

        // Setup Laser Scan Animation
        binding.vScanLaser.post(() -> {
            float parentHeight = binding.ivScanBodyPreview.getHeight();
            laserAnimator = ObjectAnimator.ofFloat(binding.vScanLaser, "translationY", 0f, parentHeight);
            laserAnimator.setDuration(1200);
            laserAnimator.setRepeatMode(ValueAnimator.REVERSE);
            laserAnimator.setRepeatCount(ValueAnimator.INFINITE);
            laserAnimator.setInterpolator(new LinearInterpolator());
            laserAnimator.start();
        });

        // Animate processing text status updates
        progressHandler.postDelayed(() -> binding.tvProgressStatus.setText("ĐANG PHÁT HIỆN BIÊN CƠ THỂ... 🔍"), 0);
        progressHandler.postDelayed(() -> binding.tvProgressStatus.setText("ĐANG ĐIỀU CHỈNH TỶ LỆ TRANG PHỤC... 📏"), 1000);
        progressHandler.postDelayed(() -> binding.tvProgressStatus.setText("ĐANG TẠO HÌNH ẢNH ƯỚM THỬ AI... ✨"), 2000);
        progressHandler.postDelayed(() -> binding.tvProgressStatus.setText("HOÀN THÀNH PHÂN TÍCH OUTFIT! 🎉"), 3000);

        // Transition to results screen
        progressHandler.postDelayed(() -> {
            if (laserAnimator != null) laserAnimator.cancel();
            binding.layoutProcessingContent.setVisibility(View.GONE);

            Intent resultIntent;
            if (isTryOnMode) {
                resultIntent = new Intent(this, VirtualFittingResultActivity.class);
            } else {
                resultIntent = new Intent(this, OutfitAnalysisResultActivity.class);
                resultIntent.putExtra("mode", "scanner");
            }
            resultIntent.putExtra("image_path", capturedPhotoPath);
            resultIntent.putExtra("product_id", productId);
            resultIntent.putExtra("size", selectedSize);
            resultIntent.putExtra("color", selectedColor);
            resultIntent.putExtra("from_cart", fromCart);
            startActivity(resultIntent);
            finish();
        }, 3600);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (laserAnimator != null) {
            laserAnimator.cancel();
        }
        progressHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        
        isScanMode = intent.getBooleanExtra("outfit_scan", false);
        productId = intent.getStringExtra("product_id");
        selectedSize = intent.getStringExtra("size");
        selectedColor = intent.getStringExtra("color");
        fromCart = intent.getBooleanExtra("from_cart", false);
        if (productId != null && !productId.isEmpty()) {
            isTryOnMode = true;
        } else {
            isTryOnMode = false;
        }

        configureModeRouting();
        
        // Reset preview states in case there was a previous selection
        capturedPhotoPath = "";
        currentPhotoUri = null;
        binding.ivModelPreview.setVisibility(View.GONE);
        binding.layoutPlaceholder.setVisibility(View.VISIBLE);
    }
}
