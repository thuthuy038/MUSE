package com.project.muse_android.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.project.muse_android.databinding.DialogAiErrorBinding;

public class AiErrorDialog extends DialogFragment {

    private DialogAiErrorBinding binding;
    private String message;

    public static AiErrorDialog newInstance(String message) {
        AiErrorDialog fragment = new AiErrorDialog();
        Bundle args = new Bundle();
        args.putString("message", message);
        fragment.setArguments(args);
        return fragment;
    }

    public static AiErrorDialog newInstance() {
        return newInstance("Ôi, hiện Muse AI đang bận mất rồi, vui lòng thử lại sau...");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            message = getArguments().getString("message");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogAiErrorBinding.inflate(inflater, container, false);
        
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        if (message != null) {
            binding.tvErrorMessage.setText(message);
        }
        
        binding.btnClose.setOnClickListener(v -> dismiss());

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            int maxVal = (int) (400 * getResources().getDisplayMetrics().density);
            if (width > maxVal) {
                width = maxVal;
            }
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
