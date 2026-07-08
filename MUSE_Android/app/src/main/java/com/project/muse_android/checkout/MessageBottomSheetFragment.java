package com.project.muse_android.checkout;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentMessageBottomSheetBinding;

public class MessageBottomSheetFragment extends BottomSheetDialogFragment {

    public interface OnMessageSubmittedListener {
        void onMessageSubmitted(String message);
    }

    private FragmentMessageBottomSheetBinding binding;
    private OnMessageSubmittedListener listener;
    private String initialMessage = "";

    public static MessageBottomSheetFragment newInstance(String message) {
        MessageBottomSheetFragment fragment = new MessageBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("message", message);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnMessageSubmittedListener(OnMessageSubmittedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            initialMessage = getArguments().getString("message", "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMessageBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.etMessage.setText(initialMessage);
        updateButtonState(initialMessage);

        binding.btnClose.setOnClickListener(v -> dismiss());

        // Focus and show keyboard
        binding.etMessage.requestFocus();
        binding.etMessage.postDelayed(() -> {
            if (getContext() != null) {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(binding.etMessage, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 150);

        binding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateButtonState(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnConfirm.setOnClickListener(v -> {
            String message = binding.etMessage.getText().toString().trim();
            if (listener != null) {
                listener.onMessageSubmitted(message);
            }
            dismiss();
        });
    }

    private void updateButtonState(String text) {
        if (text.trim().length() > 0) {
            binding.btnConfirm.setEnabled(true);
            binding.btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary_700)));
            binding.btnConfirm.setTextColor(getResources().getColor(R.color.white));
        } else {
            binding.btnConfirm.setEnabled(false);
            binding.btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF0F0F0));
            binding.btnConfirm.setTextColor(0xFFDDD8D8);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
