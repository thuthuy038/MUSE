package com.project.muse_android.address;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.adapters.AddressAdapter;
import com.project.models.User;
import com.project.muse_android.databinding.ActivityShippingAddressBinding;
import com.project.network.ApiClient;
import com.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShippingAddressActivity extends AppCompatActivity {

    private ActivityShippingAddressBinding binding;
    private AddressAdapter adapter;
    private SessionManager sessionManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        binding = ActivityShippingAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);
        setupUI();
        fetchAddresses();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());
        
        adapter = new AddressAdapter(new AddressAdapter.OnAddressActionListener() {
            @Override
            public void onAddressSelected(User.Address address) {
                // Return selected address to CheckoutActivity
                Intent data = new Intent();
                data.putExtra("selected_address_street", address.getStreet());
                data.putExtra("selected_address_ward", address.getWard());
                data.putExtra("selected_address_district", address.getDistrict());
                data.putExtra("selected_address_province", address.getProvince());
                data.putExtra("selected_user_name", address.getFullName());
                data.putExtra("selected_user_phone", address.getPhone());
                setResult(RESULT_OK, data);
                finish();
            }

            @Override
            public void onAddressEdit(User.Address address) {
                Intent intent = new Intent(ShippingAddressActivity.this, EditShippingAddressActivity.class);
                intent.putExtra("address", address);
                startActivity(intent);
            }
        });

        binding.rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAddresses.setAdapter(adapter);
        
        binding.btnAddAddress.setOnClickListener(v -> {
            startActivity(new Intent(this, AddShippingAddressActivity.class));
        });
    }

    private void fetchAddresses() {
        String token = sessionManager.getToken();
        if (token == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        ApiClient.INSTANCE.getInstance().getProfile("Bearer " + token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    if (currentUser.getAddresses() != null) {
                        adapter.setAddresses(currentUser.getAddresses());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(ShippingAddressActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAddresses();
    }
}
