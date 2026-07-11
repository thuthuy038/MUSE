package com.project.muse_android.auth;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.project.models.Banner;
import com.project.models.User;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityAiSetupBinding;
import com.project.muse_android.main.MainActivity;
import com.project.network.ApiClient;
import com.project.network.HomeApiClient;
import com.project.network.HomeApiService;
import com.project.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AISetupActivity extends AppCompatActivity {

    private ActivityAiSetupBinding binding;
    private SessionManager sessionManager;
    private HomeApiService homeApiService;

    private int currentStep = 1;
    private String selectedGender = "female";
    private int selectedHeight = 170;
    private int selectedWeight = 62;
    private com.project.adapters.BannerAdapter bannerAdapter;

    private final List<Banner> bannerList = new ArrayList<>();
    private int currentBannerIndex = 0;
    private final Handler slideHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;

    private Uri photoUri;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        homeApiService = HomeApiClient.getHomeApiService();

        // Load existing AI Profile values from AI_PREFS
        android.content.SharedPreferences prefs = getSharedPreferences("AI_PREFS", MODE_PRIVATE);
        String userIdKey = getCurrentUserId();
        selectedGender = prefs.getString(userIdKey + "_gender", "female");
        selectedHeight = prefs.getInt(userIdKey + "_height", 170);
        selectedWeight = prefs.getInt(userIdKey + "_weight", 62);
        if (selectedHeight < 140) selectedHeight = 170;
        if (selectedWeight < 40) selectedWeight = 62;

        setupLaunchers();
        setupHeader();
        setupStep1Listeners();
        setupStep2Listeners();
        setupFooterActions();
        
        setupStep1Banners();

        // Pre-fill fields from SharedPreferences
        binding.sbHeight.setProgress(selectedHeight - 140);
        binding.txtHeightDisplay.setText(selectedHeight + " CM");

        binding.sbWeight.setProgress(selectedWeight - 40);
        binding.txtWeightDisplay.setText(selectedWeight + " KG");

        String vong1 = prefs.getString(userIdKey + "_vong1", "");
        String vong2 = prefs.getString(userIdKey + "_vong2", "");
        String vong3 = prefs.getString(userIdKey + "_vong3", "");
        binding.etStep2Vong1.setText(vong1);
        binding.etStep2Vong2.setText(vong2);
        binding.etStep2Vong3.setText(vong3);

        String styles = prefs.getString(userIdKey + "_styles", "");
        if (!styles.isEmpty()) {
            String[] styleArray = styles.split(",");
            for (String s : styleArray) {
                if ("thanh_lich".equalsIgnoreCase(s) || "Elegant".equalsIgnoreCase(s)) {
                    binding.btnStyleThanhLich.setSelected(true);
                } else if ("duong_pho".equalsIgnoreCase(s) || "Streetwear".equalsIgnoreCase(s)) {
                    binding.btnStyleDuongPho.setSelected(true);
                } else if ("co_dien".equalsIgnoreCase(s) || "Classic".equalsIgnoreCase(s) || "Vintage".equalsIgnoreCase(s)) {
                    binding.btnStyleCoDien.setSelected(true);
                } else if ("toi_gian".equalsIgnoreCase(s) || "Minimalist".equalsIgnoreCase(s)) {
                    binding.btnStyleToiGian.setSelected(true);
                }
            }
        }

        int skin = prefs.getInt(userIdKey + "_skin", 0);
        if (skin == 1) binding.btnSkin1.setSelected(true);
        else if (skin == 2) binding.btnSkin2.setSelected(true);
        else if (skin == 3) binding.btnSkin3.setSelected(true);
        else if (skin == 4) binding.btnSkin4.setSelected(true);
        else if (skin == 5) binding.btnSkin5.setSelected(true);

        updateGenderUI();
    }

    private void setupLaunchers() {
        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && photoUri != null) {
                binding.ivStep2Preview.setImageURI(photoUri);
            }
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                binding.ivStep2Preview.setImageURI(uri);
            }
        });

        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted != null && isGranted) {
                openCamera();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupHeader() {
        binding.btnBackHeader.setOnClickListener(v -> {
            if (currentStep > 1) {
                currentStep--;
                showStepLayout();
            } else {
                finish();
            }
        });
    }

    private void setupStep1Listeners() {
        binding.btnGenderFemale.setOnClickListener(v -> { selectedGender = "female"; updateGenderUI(); });
        binding.btnGenderMale.setOnClickListener(v -> { selectedGender = "male"; updateGenderUI(); });
        binding.btnGenderOther.setOnClickListener(v -> { selectedGender = "other"; updateGenderUI(); });

        binding.sbHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedHeight = 140 + progress;
                binding.txtHeightDisplay.setText(selectedHeight + " CM");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        binding.sbWeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedWeight = 40 + progress;
                binding.txtWeightDisplay.setText(selectedWeight + " KG");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupStep2Listeners() {
        binding.btnTakePhoto.setOnClickListener(v -> showPermissionPopup(true));
        binding.btnUploadImage.setOnClickListener(v -> openGallery());

        // Keyboard visibility listener to show/hide footer
        final View decorView = getWindow().getDecorView();
        decorView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            android.graphics.Rect r = new android.graphics.Rect();
            decorView.getWindowVisibleDisplayFrame(r);
            int screenHeight = decorView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * 0.15) { // Keyboard is shown
                if (currentStep == 2) {
                    binding.layoutFooter.setVisibility(View.GONE);
                }
            } else { // Keyboard is hidden
                binding.layoutFooter.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showPermissionPopup(boolean isCamera) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_camera_permission);
        
        View layoutRoot = dialog.findViewById(R.id.layoutRoot);
        if (layoutRoot != null) {
            AnimationDrawable animationDrawable = (AnimationDrawable) ContextCompat.getDrawable(this, R.drawable.bg_animated_gradient);
            layoutRoot.setBackground(animationDrawable);
            if (animationDrawable != null) {
                animationDrawable.setEnterFadeDuration(2000);
                animationDrawable.setExitFadeDuration(2000);
                animationDrawable.start();
            }
        }

        dialog.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnAllow).setOnClickListener(v -> {
            dialog.dismiss();
            if (isCamera) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            }
        });

        dialog.show();
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            takePictureLauncher.launch(photoUri);
        } catch (IOException e) {
            Log.e("AISetupActivity", "Error creating image file", e);
        }
    }

    private void openGallery() {
        pickImageLauncher.launch("image/*");
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void updateGenderUI() {
        binding.btnGenderFemale.setBackground(null);
        binding.btnGenderMale.setBackground(null);
        binding.btnGenderOther.setBackground(null);
        binding.btnGenderFemale.setTextColor(ContextCompat.getColor(this, R.color.neutral_600));
        binding.btnGenderMale.setTextColor(ContextCompat.getColor(this, R.color.neutral_600));
        binding.btnGenderOther.setTextColor(ContextCompat.getColor(this, R.color.neutral_600));

        View selected = "female".equals(selectedGender) ? binding.btnGenderFemale : ("male".equals(selectedGender) ? binding.btnGenderMale : binding.btnGenderOther);
        selected.setBackgroundResource(R.drawable.dialog_background);
        if (selected instanceof android.widget.TextView) {
            ((android.widget.TextView)selected).setTextColor(ContextCompat.getColor(this, R.color.primary_500));
        }
    }

    private void setupFooterActions() {
        binding.btnNextStep.setOnClickListener(v -> {
            if (currentStep < 3) { currentStep++; showStepLayout(); }
            else saveDataAndFinish();
        });
        binding.btnSkipStep.setOnClickListener(v -> {
            if (currentStep < 3) { currentStep++; showStepLayout(); }
            else { saveDataAndFinish(); }
        });

        // Step 3 specific selection logic
        View.OnClickListener styleClickListener = view -> {
            // Deselect others in a simple way for demo, or multi-select?
            // User usually wants to select one or more. Let's make it toggle.
            view.setSelected(!view.isSelected());
        };
        binding.btnStyleThanhLich.setOnClickListener(styleClickListener);
        binding.btnStyleDuongPho.setOnClickListener(styleClickListener);
        binding.btnStyleCoDien.setOnClickListener(styleClickListener);
        binding.btnStyleToiGian.setOnClickListener(styleClickListener);

        View.OnClickListener skinClickListener = view -> {
            // Radio button behavior for skin tone
            binding.btnSkin1.setSelected(false);
            binding.btnSkin2.setSelected(false);
            binding.btnSkin3.setSelected(false);
            binding.btnSkin4.setSelected(false);
            binding.btnSkin5.setSelected(false);
            view.setSelected(true);
        };
        binding.btnSkin1.setOnClickListener(skinClickListener);
        binding.btnSkin2.setOnClickListener(skinClickListener);
        binding.btnSkin3.setOnClickListener(skinClickListener);
        binding.btnSkin4.setOnClickListener(skinClickListener);
        binding.btnSkin5.setOnClickListener(skinClickListener);
    }

    private void showStepLayout() {
        binding.layoutStep1.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        binding.layoutStep2.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
        binding.layoutStep3.setVisibility(currentStep == 3 ? View.VISIBLE : View.GONE);
    }

    private void setupStep1Banners() {
        bannerAdapter = new com.project.adapters.BannerAdapter(bannerList);
        binding.vpBannersStep1.setAdapter(bannerAdapter);

        binding.vpBannersStep1.setPageTransformer((page, position) -> {
            page.setAlpha(1 - Math.abs(position));
            page.setScaleX(0.9f + (1 - Math.abs(position)) * 0.1f);
        });

        binding.vpBannersStep1.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position < bannerList.size()) {
                    Banner banner = bannerList.get(position);
                    binding.tvBannerContent.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                        if (binding == null) return;
                        if (banner.getTitle() != null && !banner.getTitle().isEmpty()) {
                            binding.tvBannerContent.setText(banner.getTitle());
                        }
                        binding.tvBannerContent.animate().alpha(1f).setDuration(200).start();
                    }).start();
                }
            }
        });

        loadBanners();
    }

    private void loadBanners() {
        homeApiService.getBanners().enqueue(new Callback<List<Banner>>() {
            @Override
            public void onResponse(Call<List<Banner>> call, Response<List<Banner>> response) {
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null) {
                    bannerList.clear();
                    for (Banner banner : response.body()) {
                        if (banner.getStatus() == null || "active".equalsIgnoreCase(banner.getStatus())) {
                            bannerList.add(banner);
                        }
                    }
                    if (!bannerList.isEmpty()) {
                        bannerAdapter.notifyDataSetChanged();
                        binding.vpBannersStep1.setCurrentItem(0, false);
                        if (bannerList.get(0).getTitle() != null) {
                            binding.tvBannerContent.setText(bannerList.get(0).getTitle());
                        }
                        // Request layout pass to solve ViewPager2 inside ScrollView measurement bug
                        binding.vpBannersStep1.post(() -> {
                            if (binding != null) {
                                binding.vpBannersStep1.requestLayout();
                            }
                        });
                        startRotationTimer();
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Banner>> call, Throwable t) {
                Log.e("AISetupActivity", "Error loading banners", t);
            }
        });
    }

    private void startRotationTimer() {
        if (sliderRunnable != null) {
            slideHandler.removeCallbacks(sliderRunnable);
        }

        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding == null || bannerList.isEmpty()) return;

                int nextItem = (binding.vpBannersStep1.getCurrentItem() + 1) % bannerList.size();
                binding.vpBannersStep1.setCurrentItem(nextItem, true);

                slideHandler.postDelayed(this, 5000);
            }
        };

        slideHandler.postDelayed(sliderRunnable, 5000);
    }

    private void saveDataAndFinish() {
        // Save locally to AI_PREFS
        android.content.SharedPreferences.Editor editor = getSharedPreferences("AI_PREFS", MODE_PRIVATE).edit();
        String userIdKey = getCurrentUserId();
        editor.putString(userIdKey + "_gender", selectedGender);
        editor.putInt(userIdKey + "_height", selectedHeight);
        editor.putInt(userIdKey + "_weight", selectedWeight);
        editor.putString(userIdKey + "_vong1", binding.etStep2Vong1.getText().toString().trim());
        editor.putString(userIdKey + "_vong2", binding.etStep2Vong2.getText().toString().trim());
        editor.putString(userIdKey + "_vong3", binding.etStep2Vong3.getText().toString().trim());

        // Save selected styles
        StringBuilder stylesSb = new StringBuilder();
        if (binding.btnStyleThanhLich.isSelected()) stylesSb.append("thanh_lich");
        if (binding.btnStyleDuongPho.isSelected()) {
            if (stylesSb.length() > 0) stylesSb.append(",");
            stylesSb.append("duong_pho");
        }
        if (binding.btnStyleCoDien.isSelected()) {
            if (stylesSb.length() > 0) stylesSb.append(",");
            stylesSb.append("co_dien");
        }
        if (binding.btnStyleToiGian.isSelected()) {
            if (stylesSb.length() > 0) stylesSb.append(",");
            stylesSb.append("toi_gian");
        }
        editor.putString(userIdKey + "_styles", stylesSb.toString());

        // Save selected skin
        int skin = 0;
        if (binding.btnSkin1.isSelected()) skin = 1;
        else if (binding.btnSkin2.isSelected()) skin = 2;
        else if (binding.btnSkin3.isSelected()) skin = 3;
        else if (binding.btnSkin4.isSelected()) skin = 4;
        else if (binding.btnSkin5.isSelected()) skin = 5;
        editor.putInt(userIdKey + "_skin", skin);

        editor.apply();

        String token = sessionManager.getToken();
        String userId = sessionManager.getUserId();
        if (token == null || userId == null) {
            // For guest, show success popup directly
            showSuccessPopup();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("gender", selectedGender);
        userData.put("height", selectedHeight);
        userData.put("weight", selectedWeight);
        userData.put("profileCompleted", true);

        // Convert styles list for server
        List<String> serverStyles = new ArrayList<>();
        if (binding.btnStyleThanhLich.isSelected()) serverStyles.add("Elegant");
        if (binding.btnStyleDuongPho.isSelected()) serverStyles.add("Streetwear");
        if (binding.btnStyleCoDien.isSelected()) serverStyles.add("Classic");
        if (binding.btnStyleToiGian.isSelected()) serverStyles.add("Minimalist");
        userData.put("favoriteStyles", serverStyles);

        binding.btnNextStep.setEnabled(false);
        binding.btnNextStep.setText("Đang lưu...");

        ApiClient.INSTANCE.getInstance().updateUser(userId, "Bearer " + token, userData).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    sessionManager.saveProfileCompleted(true);
                    showSuccessPopup();
                } else {
                    binding.btnNextStep.setEnabled(true);
                    binding.btnNextStep.setText("TIẾP THEO");
                    Toast.makeText(AISetupActivity.this, "Không thể đồng bộ với máy chủ, đã lưu cục bộ.", Toast.LENGTH_LONG).show();
                    showSuccessPopup();
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                binding.btnNextStep.setEnabled(true);
                binding.btnNextStep.setText("TIẾP THEO");
                Toast.makeText(AISetupActivity.this, "Lỗi kết nối máy chủ, đã lưu cục bộ.", Toast.LENGTH_LONG).show();
                showSuccessPopup();
            }
        });
    }

    private void showSuccessPopup() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_setup_completed);
        dialog.setCancelable(false);

        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.findViewById(R.id.btnOk).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(AISetupActivity.this, MainActivity.class);
            intent.putExtra("open_ai", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        dialog.show();
    }

    private String getCurrentUserId() {
        if (sessionManager == null) return "guest";
        String userId = sessionManager.getUserId();
        return (userId != null) ? userId : "guest";
    }

    @Override
    protected void onDestroy() {
        if (sliderRunnable != null) {
            slideHandler.removeCallbacks(sliderRunnable);
        }
        super.onDestroy();
    }
}
