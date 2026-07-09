package com.project.muse_android.order;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.adapters.HorizontalProductAdapter;
import com.project.models.Order;
import com.project.models.Product;
import com.project.models.enums.HorizontalProductMode;
import com.project.muse_android.databinding.ActivityReturnRefundBinding;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ReturnRefundActivity extends AppCompatActivity {

    private ActivityReturnRefundBinding binding;
    private Order order;
    private HorizontalProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityReturnRefundBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        order = (Order) getIntent().getSerializableExtra("order");
        if (order == null) {
            Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        populateData();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        adapter = new HorizontalProductAdapter(this, HorizontalProductMode.READ_ONLY, null);
        binding.rvProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProducts.setAdapter(adapter);

        binding.btnSelectReason.setOnClickListener(v -> {
            Toast.makeText(this, "Chọn lý do trả hàng", Toast.LENGTH_SHORT).show();
        });

        binding.btnSelectMethod.setOnClickListener(v -> {
            Toast.makeText(this, "Chọn phương án hoàn tiền", Toast.LENGTH_SHORT).show();
        });

        binding.etNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.txtCharCount.setText(s.length() + "/2000");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnSubmit.setOnClickListener(v -> {
            Toast.makeText(this, "Yêu cầu trả hàng đã được gửi", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void populateData() {
        adapter.setData(order.getProducts());
        binding.txtRefundAmount.setText(formatPrice(order.getFinalPrice()));
        binding.txtEmail.setText(order.getEmail() != null ? order.getEmail() : "abc@gmail.com");
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + "đ";
    }
}
