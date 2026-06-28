package com.project.muse_android.profile;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.project.muse_android.databinding.EditProfileScreenBinding;

public class EditProfileActivity extends AppCompatActivity {

    private EditProfileScreenBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = EditProfileScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupSpinner();

        binding.ivBack.setOnClickListener(v -> finish());

        binding.btnSave.setOnClickListener(v -> {
            Toast.makeText(this, "Thông tin đã được lưu", Toast.LENGTH_SHORT).show();
            finish();
        });

        // Placeholder data to match mockup
        binding.etEmail.setText("thvan@gmail.com");
        binding.etPhone.setText("123456789");
        binding.etPassword.setText("**********");
        binding.etStreet.setText("Đỗ Xuân Hợp");
        binding.etWard.setText("Phước Long");
        binding.etAddressNote.setText("235");
        binding.etAccountNumber.setText("204356XXXXXXXX");
        binding.etAccountName.setText("HUYNH THANH VAN");
        binding.etBank.setText("Vietcombank");
    }

    private void setupSpinner() {
        String[] cities = {"TP. Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Cần Thơ", "Hải Phòng"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCity.setAdapter(adapter);
    }
}
