package com.project.muse_android.dialog;

import android.app.Dialog;
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

import com.project.muse_android.databinding.DialogLogoutBinding;

public class LogoutDialog extends DialogFragment {

    private DialogLogoutBinding binding;
    private OnLogoutListener listener;

    public interface OnLogoutListener {
        void onConfirm();
    }

    public void setOnLogoutListener(OnLogoutListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogLogoutBinding.inflate(inflater, container, false);
        
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        binding.btnConfirm.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConfirm();
            }
            dismiss();
        });

        binding.btnCancel.setOnClickListener(v -> dismiss());

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            int maxWidth = (int) (400 * getResources().getDisplayMetrics().density);
            if (width > maxWidth) {
                width = maxWidth;
            }
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}