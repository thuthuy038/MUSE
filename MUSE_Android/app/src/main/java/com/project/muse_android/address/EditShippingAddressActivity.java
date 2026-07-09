package com.project.muse_android.address;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.project.models.Province;
import com.project.models.User;
import com.project.models.Ward;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityEditShippingAddressBinding;
import com.project.network.ApiClient;
import com.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditShippingAddressActivity extends AppCompatActivity {

    private ActivityEditShippingAddressBinding binding;
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;
    private User.Address currentAddress;
    
    private final List<Province> provinceList = new ArrayList<>();
    private final List<Ward> wardList = new ArrayList<>();
    
    private Province selectedProvince;
    private Ward selectedWard;
    private String selectedType = "nha_rieng"; // Default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityEditShippingAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        sessionManager = new SessionManager(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang xử lý...");
        progressDialog.setCancelable(false);

        // Get Address from intent
        currentAddress = (User.Address) getIntent().getSerializableExtra("address");
        if (currentAddress == null) {
            Toast.makeText(this, "Không tìm thấy dữ liệu địa chỉ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        setupSwitchColors();
        populateData();
        fetchProvinces();
    }

    private void setupSwitchColors() {
        int[][] states = new int[][] {
            new int[] {-android.R.attr.state_checked}, // unchecked
            new int[] {android.R.attr.state_checked}  // checked
        };
        int[] thumbColors = new int[] {
            android.graphics.Color.parseColor("#B0BEC5"), // grey
            android.graphics.Color.parseColor("#E63F69")  // pink
        };
        int[] trackColors = new int[] {
            android.graphics.Color.parseColor("#ECEFF1"), // light grey
            android.graphics.Color.parseColor("#FFE5EC")  // pink track
        };
        binding.swDefault.setThumbTintList(new android.content.res.ColorStateList(states, thumbColors));
        binding.swDefault.setTrackTintList(new android.content.res.ColorStateList(states, trackColors));
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.btnSelectCity.setOnClickListener(v -> {
            if (provinceList.isEmpty()) {
                fetchProvinces();
            } else {
                showProvinceDialog();
            }
        });

        binding.btnSelectWard.setOnClickListener(v -> {
            if (selectedProvince == null && currentAddress.getProvince() == null) {
                Toast.makeText(this, "Vui lòng chọn Tỉnh/Thành phố trước", Toast.LENGTH_SHORT).show();
                return;
            }
            if (wardList.isEmpty()) {
                String code = selectedProvince != null ? selectedProvince.getCode() : "";
                fetchWards(code, true);
            } else {
                showWardDialog();
            }
        });
        
        binding.btnTypeOffice.setOnClickListener(v -> setAddressType("van_phong"));
        binding.btnTypeHome.setOnClickListener(v -> setAddressType("nha_rieng"));
        
        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        binding.btnComplete.setOnClickListener(v -> saveAddress());
    }

    private void populateData() {
        binding.etFullName.setText(currentAddress.getFullName());
        binding.etPhone.setText(currentAddress.getPhone());
        binding.etStreet.setText(currentAddress.getStreet());
        
        binding.txtCity.setText(currentAddress.getProvince() != null ? currentAddress.getProvince() : "Chọn tỉnh thành");
        binding.txtWard.setText(currentAddress.getWard() != null ? currentAddress.getWard() : "Chọn phường xã");
        
        binding.swDefault.setChecked(currentAddress.isDefault());
        setAddressType(currentAddress.getType() != null ? currentAddress.getType() : "nha_rieng");
    }

    private void updateSelectionUI() {
        if (selectedProvince != null) {
            binding.txtCity.setText(selectedProvince.getName());
            binding.txtCity.setTextColor(android.graphics.Color.BLACK);
            binding.txtCity.setAlpha(1.0f);
        }
        if (selectedWard != null) {
            binding.txtWard.setText(selectedWard.getName());
            binding.txtWard.setTextColor(android.graphics.Color.BLACK);
            binding.txtWard.setAlpha(1.0f);
        } else if (selectedProvince == null) {
            binding.txtWard.setAlpha(0.5f);
            binding.txtWard.setText("Phường/Xã");
        }
    }

    private void setAddressType(String type) {
        selectedType = type;
        if ("office".equalsIgnoreCase(type) || "van_phong".equalsIgnoreCase(type)) {
            binding.btnTypeOffice.setBackgroundResource(R.drawable.bg_default_address_badge);
            binding.btnTypeOffice.setTextColor(getColor(R.color.primary_700));
            
            binding.btnTypeHome.setBackgroundResource(R.drawable.bg_segmented_control);
            binding.btnTypeHome.setTextColor(getColor(R.color.neutral_600));
        } else {
            binding.btnTypeHome.setBackgroundResource(R.drawable.bg_default_address_badge);
            binding.btnTypeHome.setTextColor(getColor(R.color.primary_700));
            
            binding.btnTypeOffice.setBackgroundResource(R.drawable.bg_segmented_control);
            binding.btnTypeOffice.setTextColor(getColor(R.color.neutral_600));
        }
    }

    private void fetchProvinces() {
        ApiClient.INSTANCE.getInstance().getProvinces("https://provinces.open-api.vn/api/v2/p/").enqueue(new Callback<List<Province>>() {
            @Override
            public void onResponse(@NonNull Call<List<Province>> call, @NonNull Response<List<Province>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    provinceList.clear();
                    provinceList.addAll(response.body());
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Province>> call, @NonNull Throwable t) {}
        });
    }

    private <T> void showSearchableDialog(String title, List<T> list, java.util.function.Function<T, String> getNameFunc, OnSelectedListener<T> listener) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        android.widget.EditText searchEdit = new android.widget.EditText(this);
        searchEdit.setHint("Nhập chữ cái để tìm kiếm...");
        searchEdit.setSingleLine(true);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        searchEdit.setLayoutParams(params);
        layout.addView(searchEdit);

        android.widget.ListView listView = new android.widget.ListView(this);
        android.widget.LinearLayout.LayoutParams listParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                800, 1.0f);
        listView.setLayoutParams(listParams);
        layout.addView(listView);

        List<String> names = new ArrayList<>();
        for (T item : list) {
            names.add(getNameFunc.apply(item));
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(layout)
                .setNegativeButton("Hủy", null)
                .create();

        searchEdit.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = adapter.getItem(position);
            for (T item : list) {
                if (getNameFunc.apply(item).equals(selectedName)) {
                    listener.onSelected(item);
                    break;
                }
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    interface OnSelectedListener<T> {
        void onSelected(T item);
    }

    private void showProvinceDialog() {
        if (provinceList.isEmpty()) {
            fetchProvinces();
            Toast.makeText(this, "Đang tải dữ liệu tỉnh thành...", Toast.LENGTH_SHORT).show();
            return;
        }

        showSearchableDialog("Chọn Tỉnh/Thành phố", provinceList, Province::getName, province -> {
            selectedProvince = province;
            selectedWard = null;
            wardList.clear();
            updateSelectionUI();
            fetchWards(selectedProvince.getCode(), false);
        });
    }

    private void fetchWards(String code, boolean showDialogAfter) {
        if (code.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn Tỉnh/Thành phố trước", Toast.LENGTH_SHORT).show();
            return;
        }
        if (showDialogAfter) progressDialog.show();
        ApiClient.INSTANCE.getInstance().getProvinceDetails("https://provinces.open-api.vn/api/v2/p/" + code + "?depth=2").enqueue(new Callback<Province>() {
            @Override
            public void onResponse(@NonNull Call<Province> call, @NonNull Response<Province> response) {
                if (showDialogAfter) progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null && response.body().getWards() != null) {
                    wardList.clear();
                    wardList.addAll(response.body().getWards());
                    if (showDialogAfter) showWardDialog();
                }
            }
            @Override
            public void onFailure(@NonNull Call<Province> call, @NonNull Throwable t) {
                if (showDialogAfter) progressDialog.dismiss();
                Toast.makeText(EditShippingAddressActivity.this, "Lỗi tải phường/xã", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showWardDialog() {
        if (wardList.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy dữ liệu phường/xã", Toast.LENGTH_SHORT).show();
            return;
        }

        showSearchableDialog("Chọn Phường/Xã", wardList, Ward::getName, ward -> {
            selectedWard = ward;
            updateSelectionUI();
        });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa địa chỉ")
                .setMessage("Bạn có chắc chắn muốn xóa địa chỉ này?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteAddress())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteAddress() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getToken();
        if (userId == null || token == null) return;

        progressDialog.show();
        ApiClient.INSTANCE.getInstance().getProfile("Bearer " + token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User.Address> addresses = response.body().getAddresses();
                    if (addresses != null) {
                        addresses.removeIf(a -> a.get_id().equals(currentAddress.get_id()));
                        updateUserAddresses(userId, token, addresses);
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(EditShippingAddressActivity.this, "Lỗi tải thông tin", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(EditShippingAddressActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAddress() {
        String fullName = binding.etFullName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String street = binding.etStreet.getText().toString().trim();
        
        if (fullName.isEmpty() || phone.isEmpty() || street.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedProvince != null && selectedWard == null) {
            Toast.makeText(this, "Vui lòng chọn Phường/Xã", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getUserId();
        String token = sessionManager.getToken();
        if (userId == null || token == null) return;

        progressDialog.show();
        ApiClient.INSTANCE.getInstance().getProfile("Bearer " + token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User.Address> addresses = response.body().getAddresses();
                    if (addresses != null) {
                        for (User.Address a : addresses) {
                            if (a.get_id().equals(currentAddress.get_id())) {
                                a.setFullName(fullName);
                                a.setPhone(phone);
                                a.setStreet(street);
                                if (selectedProvince != null) a.setProvince(selectedProvince.getName());
                                a.setDistrict(""); 
                                if (selectedWard != null) a.setWard(selectedWard.getName());
                                a.setDefault(binding.swDefault.isChecked());
                                a.setType(selectedType);
                                break;
                            }
                        }
                        
                        // If we set this one to default, server might handle it, but let's be safe
                        if (binding.swDefault.isChecked()) {
                            for (User.Address a : addresses) {
                                if (!a.get_id().equals(currentAddress.get_id())) {
                                    a.setDefault(false);
                                }
                            }
                        }

                        updateUserAddresses(userId, token, addresses);
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(EditShippingAddressActivity.this, "Lỗi tải thông tin", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(EditShippingAddressActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserAddresses(String userId, String token, List<User.Address> addresses) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("addresses", addresses);

        ApiClient.INSTANCE.getInstance().updateUser(userId, "Bearer " + token, updateData).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    Toast.makeText(EditShippingAddressActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditShippingAddressActivity.this, "Lỗi cập nhật: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(EditShippingAddressActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
