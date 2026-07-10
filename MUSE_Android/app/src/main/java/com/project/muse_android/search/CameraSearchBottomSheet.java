package com.project.muse_android.search;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.project.muse_android.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CameraSearchBottomSheet extends BottomSheetDialogFragment {

    public interface CameraSearchListener {
        void onImageCaptured(String imagePath);
    }

    private CameraSearchListener listener;
    private String capturedPhotoPath = "";
    private String capturedVideoPath = "";
    private Uri currentOutputUri = null;

    // Permissions Request Launchers
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(getContext(), "Cần quyền Camera để chụp ảnh.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> requestVideoPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchVideoCamera();
                } else {
                    Toast.makeText(getContext(), "Cần quyền Camera để quay video.", Toast.LENGTH_SHORT).show();
                }
            });

    // Action Launchers
    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), isSuccess -> {
                if (isSuccess && !capturedPhotoPath.isEmpty()) {
                    if (listener != null) {
                        listener.onImageCaptured(capturedPhotoPath);
                    }
                    dismiss();
                }
            });

    private final ActivityResultLauncher<Intent> recordVideoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && !capturedVideoPath.isEmpty()) {
                    extractFrameFromVideo(capturedVideoPath);
                }
            });

    private final ActivityResultLauncher<String> selectImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && getContext() != null) {
                    String localPath = copyUriToTempFile(uri);
                    if (!localPath.isEmpty() && listener != null) {
                        listener.onImageCaptured(localPath);
                    }
                    dismiss();
                }
            });

    public void setCameraSearchListener(CameraSearchListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_camera_search_bottom_sheet, container, false);

        LinearLayout btnCapturePhoto = view.findViewById(R.id.btnCapturePhoto);
        LinearLayout btnRecordVideo = view.findViewById(R.id.btnRecordVideo);
        LinearLayout btnChooseGallery = view.findViewById(R.id.btnChooseGallery);

        btnCapturePhoto.setOnClickListener(v -> checkPermissionAndTakePhoto());
        btnRecordVideo.setOnClickListener(v -> checkPermissionAndRecordVideo());
        btnChooseGallery.setOnClickListener(v -> selectImageLauncher.launch("image/*"));

        return view;
    }

    private void checkPermissionAndTakePhoto() {
        if (getContext() == null) return;
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void checkPermissionAndRecordVideo() {
        if (getContext() == null) return;
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchVideoCamera();
        } else {
            requestVideoPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        if (getContext() == null) return;
        try {
            File tempFile = File.createTempFile("search_photo_", ".jpg", getContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES));
            capturedPhotoPath = tempFile.getAbsolutePath();
            currentOutputUri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", tempFile);
            takePictureLauncher.launch(currentOutputUri);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi khởi tạo Camera.", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchVideoCamera() {
        if (getContext() == null) return;
        try {
            File tempFile = File.createTempFile("search_video_", ".mp4", getContext().getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES));
            capturedVideoPath = tempFile.getAbsolutePath();
            currentOutputUri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", tempFile);

            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, currentOutputUri);
            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 2); // Set maximum 2 seconds duration limit
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0); // Low quality to optimize processing
            recordVideoLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi khởi tạo camera quay video.", Toast.LENGTH_SHORT).show();
        }
    }

    private void extractFrameFromVideo(String videoPath) {
        if (getContext() == null) return;
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(videoPath);
            
            // Extract frame at 1 second (1000000 microseconds)
            Bitmap frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            retriever.release();

            if (frame != null) {
                File tempFrameFile = File.createTempFile("search_frame_", ".jpg", getContext().getCacheDir());
                FileOutputStream out = new FileOutputStream(tempFrameFile);
                frame.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();

                if (listener != null) {
                    listener.onImageCaptured(tempFrameFile.getAbsolutePath());
                }
                dismiss();
            } else {
                Toast.makeText(getContext(), "Không thể trích xuất hình ảnh từ video.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi xử lý video.", Toast.LENGTH_SHORT).show();
        }
    }

    private String copyUriToTempFile(Uri uri) {
        if (getContext() == null) return "";
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("search_gallery_", ".jpg", getContext().getCacheDir());
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
}
