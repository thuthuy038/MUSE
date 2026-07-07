package com.project.muse_android.dialog;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.project.muse_android.R;
import com.project.muse_android.auth.AuthActivity;
import com.project.muse_android.databinding.DialogNewMemberOfferBinding;
import com.project.utils.SessionManager;

public class NewMemberOfferBottomSheet extends BottomSheetDialogFragment {

    private DialogNewMemberOfferBinding binding;
    private SessionManager sessionManager;

    public static NewMemberOfferBottomSheet newInstance() {
        return new NewMemberOfferBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogNewMemberOfferBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Handle Login button click
        binding.btnOfferLogin.setOnClickListener(v -> {
            saveDontShowAgainPreferenceIfNeeded();
            
            // Navigate to AuthActivity with from_offer = true to show Login page
            Intent intent = new Intent(requireContext(), AuthActivity.class);
            intent.putExtra("from_offer", true);
            startActivity(intent);
            
            dismiss();
        });

        // Handle Skip button click
        binding.btnOfferSkip.setOnClickListener(v -> {
            saveDontShowAgainPreferenceIfNeeded();
            dismiss();
        });
    }

    private void saveDontShowAgainPreferenceIfNeeded() {
        if (binding.cbDontShowAgain.isChecked()) {
            sessionManager.setDontShowOfferAgain(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Remove the default background color of BottomSheetDialog container
            View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
