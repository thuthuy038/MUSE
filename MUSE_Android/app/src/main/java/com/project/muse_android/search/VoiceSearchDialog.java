package com.project.muse_android.search;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.project.muse_android.R;

import java.util.ArrayList;

public class VoiceSearchDialog extends DialogFragment implements RecognitionListener {

    public interface VoiceSearchListener {
        void onVoiceSearchResult(String result);
    }

    private VoiceSearchListener listener;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;

    private FrameLayout layoutMicContainer;
    private ImageView ivMicIcon;
    private TextView tvVoiceStatus;
    private TextView tvVoiceHint;
    private Button btnCancelVoice;

    private boolean isListening = false;

    public void setVoiceSearchListener(VoiceSearchListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_voice_search, container, false);

        layoutMicContainer = view.findViewById(R.id.layoutMicContainer);
        ivMicIcon = view.findViewById(R.id.ivMicIcon);
        tvVoiceStatus = view.findViewById(R.id.tvVoiceStatus);
        tvVoiceHint = view.findViewById(R.id.tvVoiceHint);
        btnCancelVoice = view.findViewById(R.id.btnCancelVoice);
        Button btnDoneVoice = view.findViewById(R.id.btnDoneVoice);

        // Cancel Button Action
        btnCancelVoice.setOnClickListener(v -> {
            stopListening();
            dismiss();
        });

        // Done Button Action - stops recording and triggers processing of whatever was spoken
        btnDoneVoice.setOnClickListener(v -> {
            if (isListening && speechRecognizer != null) {
                speechRecognizer.stopListening();
            } else {
                dismiss();
            }
        });

        // Click Mic to toggle listening or restart if failed
        layoutMicContainer.setOnClickListener(v -> {
            if (isListening) {
                stopListening();
            } else {
                startListening();
            }
        });

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Add fade-in animation
            dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        }
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup Speech Recognizer
        if (getContext() != null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
            speechRecognizer.setRecognitionListener(this);

            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN"); // Vietnamese language support
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN");
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "vi-VN");
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            startListening();
        } else {
            dismiss();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            float density = getResources().getDisplayMetrics().density;
            int width = (int) (340 * density);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void startListening() {
        if (speechRecognizer != null) {
            speechRecognizer.startListening(recognizerIntent);
            isListening = true;
            updateMicUI(true);
            tvVoiceStatus.setText("MUSE AI Đang nghe...");
            tvVoiceHint.setText("Hãy nói món đồ hoặc phong cách bạn muốn tìm kiếm...");
        }
    }

    private void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            isListening = false;
            updateMicUI(false);
        }
    }

    private void updateMicUI(boolean active) {
        if (active) {
            layoutMicContainer.setBackgroundResource(R.drawable.bg_circle_mic_active);
            ivMicIcon.setColorFilter(getResources().getColor(R.color.primary_500));
        } else {
            layoutMicContainer.setBackgroundResource(R.drawable.bg_circle_mic_inactive);
            ivMicIcon.setColorFilter(Color.GRAY);
            layoutMicContainer.setScaleX(1.0f);
            layoutMicContainer.setScaleY(1.0f);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    // --- RecognitionListener Callbacks ---

    @Override
    public void onReadyForSpeech(Bundle params) {
        tvVoiceStatus.setText("Đang ghi âm...");
        updateMicUI(true);
    }

    @Override
    public void onBeginningOfSpeech() {
        tvVoiceStatus.setText("MUSE Đang nghe...");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        // Dynamic voice scale micro-animation
        if (isListening && rmsdB > 0) {
            float scale = 1.0f + (rmsdB / 15f); // Scale factor
            if (scale > 1.4f) scale = 1.4f;
            layoutMicContainer.setScaleX(scale);
            layoutMicContainer.setScaleY(scale);
        }
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
    }

    @Override
    public void onEndOfSpeech() {
        tvVoiceStatus.setText("Đang phân tích...");
        updateMicUI(false);
    }

    @Override
    public void onError(int error) {
        isListening = false;
        updateMicUI(false);

        String message;
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Lỗi âm thanh.";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Lỗi thiết bị khách.";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Thiếu quyền ghi âm.";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Lỗi kết nối mạng.";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "Không nhận diện được giọng nói.";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "Hệ thống nhận giọng nói đang bận.";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "Không có giọng nói nhận được.";
                break;
            default:
                message = "Lỗi không xác định.";
                break;
        }

        tvVoiceStatus.setText("Thử lại");
        tvVoiceHint.setText(message + "\nChạm vào mic để nói lại ✨");
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            String resultText = matches.get(0);
            if (listener != null) {
                listener.onVoiceSearchResult(resultText);
            }
            dismiss();
        } else {
            onError(SpeechRecognizer.ERROR_NO_MATCH);
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            String partialText = matches.get(0);
            tvVoiceHint.setText(partialText); // Show what is currently recognized realtime
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }
}
