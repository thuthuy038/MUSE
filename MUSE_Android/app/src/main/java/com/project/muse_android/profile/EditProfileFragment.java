package com.project.muse_android.profile;

import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.project.muse_android.R;
import com.project.muse_android.databinding.EditProfileScreenBinding;
import com.project.utils.SessionManager;
import com.project.models.User;
import com.project.models.Province;
import com.project.models.District;
import com.project.models.Ward;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileFragment extends Fragment {

    private EditProfileScreenBinding binding;
    private SessionManager sessionManager;

    private String savedCity = null;
    private String savedDistrict = null;
    private String savedWard = null;

    private int currentProvinceCode = -1;
    private int currentDistrictCode = -1;

    private String tempAvatarBase64 = null;
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && isAdded()) {
                    Glide.with(EditProfileFragment.this).load(uri).into(binding.ivProfile);
                    selectedImageUri = uri;
                    tempAvatarBase64 = null; // Clear base64 since we have raw URI now
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = EditProfileScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        binding.ivBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        binding.btnEditAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.tvChangePassword.setOnClickListener(v -> {
            String newPassword = binding.etPassword.getText().toString().trim();
            if (newPassword.isEmpty() || newPassword.equals("**********")) {
                Toast.makeText(getContext(), "Vui lòng nhập mật khẩu mới trước khi đổi", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 8) {
                Toast.makeText(getContext(), "Mật khẩu phải có ít nhất 8 ký tự", Toast.LENGTH_SHORT).show();
                return;
            }

            String token = sessionManager.getToken();
            String userId = sessionManager.getUserId();

            if (token != null && userId != null) {
                binding.tvChangePassword.setEnabled(false);
                binding.tvChangePassword.setText("Đang đổi...");

                Map<String, Object> userData = new HashMap<>();
                userData.put("password", newPassword);

                com.project.network.ApiClient.INSTANCE.getInstance().updateUser(userId, "Bearer " + token, userData).enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (!isAdded()) return;
                        binding.tvChangePassword.setEnabled(true);
                        binding.tvChangePassword.setText(getString(R.string.change_password));

                        if (response.isSuccessful()) {
                            binding.etPassword.setText("**********"); // Đặt lại placeholder
                            Toast.makeText(getContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Đổi mật khẩu thất bại: " + response.message(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        if (!isAdded()) return;
                        binding.tvChangePassword.setEnabled(true);
                        binding.tvChangePassword.setText(getString(R.string.change_password));
                        Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(getContext(), "Không tìm thấy phiên đăng nhập", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnSave.setOnClickListener(v -> {
            String newName = binding.etName.getText().toString().trim();
            String newEmail = binding.etEmail.getText().toString().trim();
            String newPhone = binding.etPhone.getText().toString().trim();

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(getContext(), "Họ tên và Email không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }

            String token = sessionManager.getToken();
            String userId = sessionManager.getUserId();

            if (token != null && userId != null) {
                binding.btnSave.setEnabled(false);
                binding.btnSave.setText("Đang xử lý...");

                if (selectedImageUri != null) {
                    uploadAvatarAndSaveProfile(userId, token, newName, newEmail, newPhone);
                } else {
                    saveUserProfile(userId, token, newName, newEmail, newPhone);
                }
            } else {
                Toast.makeText(getContext(), "Không tìm thấy phiên đăng nhập", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup item selection listeners for cascading dropdowns
        binding.spinnerCity.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                Province selectedProvince = (Province) parent.getItemAtPosition(position);
                loadDistricts(selectedProvince);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        binding.spinnerDistrict.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                District selectedDistrict = (District) parent.getItemAtPosition(position);
                loadWards(selectedDistrict);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Initialize fields to empty defaults (except password placeholder)
        binding.etPassword.setText("**********");
        binding.etStreet.setText("");
        binding.etAddressNote.setText("");
        binding.etAccountNumber.setText("");
        binding.etAccountName.setText("");
        binding.etBank.setText("");

        loadLocations();
        loadUserProfile();
    }

    private void uploadAvatarAndSaveProfile(String userId, String token, String name, String email, String phone) {
        okhttp3.MultipartBody.Part avatarPart = prepareImagePart(selectedImageUri);
        if (avatarPart == null) {
            Toast.makeText(getContext(), "Không thể chuẩn bị file ảnh", Toast.LENGTH_SHORT).show();
            binding.btnSave.setEnabled(true);
            binding.btnSave.setText("Lưu");
            return;
        }

        com.project.network.ApiClient.INSTANCE.getInstance().uploadAvatar(userId, avatarPart).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    // Save new avatar URL to cache
                    if (user.getAvatar() != null && user.getAvatar().getUrl() != null) {
                        String avatarUrl = user.getAvatar().getUrl();
                        if (avatarUrl.startsWith("/")) {
                            avatarUrl = "https://server-testing-ymn9.onrender.com" + avatarUrl;
                        }
                        sessionManager.saveAvatar(userId, avatarUrl);
                    }
                    // Step 2: Save the rest of the profile info
                    saveUserProfile(userId, token, name, email, phone);
                } else {
                    Toast.makeText(getContext(), "Không thể tải lên ảnh đại diện: " + response.message(), Toast.LENGTH_SHORT).show();
                    binding.btnSave.setEnabled(true);
                    binding.btnSave.setText("Lưu");
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi tải ảnh lên: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                binding.btnSave.setEnabled(true);
                binding.btnSave.setText("Lưu");
            }
        });
    }

    private void saveUserProfile(String userId, String token, String name, String email, String phone) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phone", phone);

        // Prepare address data
        Map<String, Object> addressMap = new HashMap<>();
        addressMap.put("street", binding.etStreet.getText().toString().trim());
        addressMap.put("ward", binding.spinnerWard.getSelectedItem() != null ? binding.spinnerWard.getSelectedItem().toString() : "");
        addressMap.put("district", binding.spinnerDistrict.getSelectedItem() != null ? binding.spinnerDistrict.getSelectedItem().toString() : "");
        addressMap.put("province", binding.spinnerCity.getSelectedItem() != null ? binding.spinnerCity.getSelectedItem().toString() : "");
        addressMap.put("addressNote", binding.etAddressNote.getText().toString().trim());
        addressMap.put("isDefault", true);

        List<Map<String, Object>> addressList = new ArrayList<>();
        addressList.add(addressMap);
        userData.put("addresses", addressList);

        // Prepare payment data
        Map<String, Object> paymentMap = new HashMap<>();
        paymentMap.put("accountNumber", binding.etAccountNumber.getText().toString().trim());
        paymentMap.put("accountName", binding.etAccountName.getText().toString().trim());
        paymentMap.put("bank", binding.etBank.getText().toString().trim());
        userData.put("payment", paymentMap);

        com.project.network.ApiClient.INSTANCE.getInstance().updateUser(userId, "Bearer " + token, userData).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!isAdded()) return;
                binding.btnSave.setEnabled(true);
                binding.btnSave.setText("Lưu");

                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    // Update local session
                    sessionManager.saveUser(user.get_id(), user.getName(), user.getEmail());
                    Toast.makeText(getContext(), "Thông tin đã được lưu thành công", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).popBackStack();
                } else {
                    Toast.makeText(getContext(), "Không thể cập nhật thông tin: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                if (!isAdded()) return;
                binding.btnSave.setEnabled(true);
                binding.btnSave.setText("Lưu");
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private okhttp3.MultipartBody.Part prepareImagePart(Uri uri) {
        try {
            if (getContext() == null) return null;
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = new File(getContext().getCacheDir(), "temp_avatar.jpg");
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            RequestBody requestFile = RequestBody.create(
                    tempFile,
                    MediaType.parse(getContext().getContentResolver().getType(uri))
            );

            return okhttp3.MultipartBody.Part.createFormData("avatar", tempFile.getName(), requestFile);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadLocations() {
        com.project.network.ApiClient.INSTANCE.getInstance().getProvinces("https://provinces.open-api.vn/api/?depth=1").enqueue(new Callback<List<Province>>() {
            @Override
            public void onResponse(Call<List<Province>> call, Response<List<Province>> response) {
                if (!isAdded()) return;
                List<Province> list;
                if (response.isSuccessful() && response.body() != null) {
                    list = response.body();
                } else {
                    list = getFallbackProvinces();
                }
                populateSpinner(list);
            }

            @Override
            public void onFailure(Call<List<Province>> call, Throwable t) {
                if (!isAdded()) return;
                populateSpinner(getFallbackProvinces());
            }
        });
    }

    private void populateSpinner(List<Province> list) {
        if (!isAdded()) return;
        ArrayAdapter<Province> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_spinner_selected, list);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        binding.spinnerCity.setAdapter(adapter);

        // Auto-select city if already loaded
        if (savedCity != null) {
            selectSpinnerProvince(binding.spinnerCity, savedCity);
        }
    }

    private void loadDistricts(Province province) {
        if (province == null) return;
        final int provinceCode = province.getCode();
        currentProvinceCode = provinceCode;

        com.project.network.ApiClient.INSTANCE.getInstance().getProvinceDetails("https://provinces.open-api.vn/api/p/" + provinceCode + "?depth=2").enqueue(new Callback<Province>() {
            @Override
            public void onResponse(Call<Province> call, Response<Province> response) {
                if (!isAdded()) return;
                if (provinceCode != currentProvinceCode) {
                    return; // Discard outdated response
                }
                List<District> list = null;
                if (response.isSuccessful() && response.body() != null) {
                    list = response.body().getDistricts();
                }
                if (list == null || list.isEmpty()) {
                    list = getFallbackDistricts(province);
                }
                populateDistricts(list);
            }

            @Override
            public void onFailure(Call<Province> call, Throwable t) {
                if (!isAdded()) return;
                if (provinceCode != currentProvinceCode) {
                    return; // Discard outdated response
                }
                populateDistricts(getFallbackDistricts(province));
            }
        });
    }

    private void populateDistricts(List<District> list) {
        if (!isAdded()) return;
        ArrayAdapter<District> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_spinner_selected, list);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        binding.spinnerDistrict.setAdapter(adapter);

        if (savedDistrict != null) {
            selectSpinnerDistrict(binding.spinnerDistrict, savedDistrict);
        }
    }

    private void loadWards(District district) {
        if (district == null) return;
        final int districtCode = district.getCode();
        currentDistrictCode = districtCode;

        com.project.network.ApiClient.INSTANCE.getInstance().getDistrictDetails("https://provinces.open-api.vn/api/d/" + districtCode + "?depth=2").enqueue(new Callback<District>() {
            @Override
            public void onResponse(Call<District> call, Response<District> response) {
                if (!isAdded()) return;
                if (districtCode != currentDistrictCode) {
                    return; // Discard outdated response
                }
                List<Ward> list = null;
                if (response.isSuccessful() && response.body() != null) {
                    list = response.body().getWards();
                }
                if (list == null || list.isEmpty()) {
                    list = getFallbackWards(district);
                }
                populateWards(list);
            }

            @Override
            public void onFailure(Call<District> call, Throwable t) {
                if (!isAdded()) return;
                if (districtCode != currentDistrictCode) {
                    return; // Discard outdated response
                }
                populateWards(getFallbackWards(district));
            }
        });
    }

    private void populateWards(List<Ward> list) {
        if (!isAdded()) return;
        ArrayAdapter<Ward> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_spinner_selected, list);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        binding.spinnerWard.setAdapter(adapter);

        if (savedWard != null) {
            selectSpinnerWard(binding.spinnerWard, savedWard);
        }
    }

    private void loadUserProfile() {
        String token = sessionManager.getToken();
        String cachedName = sessionManager.getUserName();
        String cachedEmail = sessionManager.getUserEmail();

        if (cachedName != null) {
            binding.etName.setText(cachedName);
        }
        if (cachedEmail != null) {
            binding.etEmail.setText(cachedEmail);
        }

        // Load cached avatar immediately for better UX
        String cachedAvatar = sessionManager.getAvatar(sessionManager.getUserId());
        setAvatarImage(cachedAvatar);

        if (token != null) {
            com.project.network.ApiClient.INSTANCE.getInstance().getProfile("Bearer " + token).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null) {
                        User user = response.body();
                        binding.etName.setText(user.getName());
                        binding.etEmail.setText(user.getEmail());
                        if (user.getPhone() != null) {
                            binding.etPhone.setText(user.getPhone());
                        }

                        if (user.getAvatar() != null && user.getAvatar().getUrl() != null && !user.getAvatar().getUrl().isEmpty()) {
                            String avatarUrl = user.getAvatar().getUrl();
                            setAvatarImage(avatarUrl);
                            
                            // Cache the full URL
                            String fullCacheUrl = avatarUrl;
                            if (!avatarUrl.startsWith("http")) {
                                if (avatarUrl.startsWith("/")) {
                                    fullCacheUrl = "https://server-testing-ymn9.onrender.com" + avatarUrl;
                                } else {
                                    fullCacheUrl = "https://server-testing-ymn9.onrender.com/" + avatarUrl;
                                }
                            }
                            sessionManager.saveAvatar(user.get_id(), fullCacheUrl);
                        } else {
                            // Try local cache if server doesn't have it
                            String cached = sessionManager.getAvatar(user.get_id());
                            setAvatarImage(cached);
                        }

                        // Parse Address (filter default, otherwise take first one)
                        List<User.Address> addresses = user.getAddresses();
                        if (addresses != null && !addresses.isEmpty()) {
                            User.Address activeAddress = null;
                            for (User.Address addr : addresses) {
                                if (addr.isDefault()) {
                                    activeAddress = addr;
                                    break;
                                }
                            }
                            if (activeAddress == null) {
                                activeAddress = addresses.get(0);
                            }

                            binding.etStreet.setText(activeAddress.getStreet() != null ? activeAddress.getStreet() : "");
                            binding.etAddressNote.setText(activeAddress.getAddressNote() != null ? activeAddress.getAddressNote() : "");

                            savedCity = activeAddress.getProvince();
                            savedDistrict = activeAddress.getDistrict();
                            savedWard = activeAddress.getWard();

                            // Trigger cascade selection starting from City/Province
                            if (savedCity != null) {
                                selectSpinnerProvince(binding.spinnerCity, savedCity);
                            }
                        } else {
                            binding.etStreet.setText("");
                            binding.etAddressNote.setText("");
                            if (binding.spinnerCity.getAdapter() != null) {
                                binding.spinnerCity.setSelection(0);
                            }
                        }

                        // Parse Payment
                        User.Payment payment = user.getPayment();
                        if (payment != null) {
                            binding.etAccountNumber.setText(payment.getAccountNumber() != null ? payment.getAccountNumber() : "");
                            binding.etAccountName.setText(payment.getAccountName() != null ? payment.getAccountName() : "");
                            binding.etBank.setText(payment.getBank() != null ? payment.getBank() : "");
                        } else {
                            binding.etAccountNumber.setText("");
                            binding.etAccountName.setText("");
                            binding.etBank.setText("");
                        }

                        // Update cache
                        sessionManager.saveUser(user.get_id(), user.getName(), user.getEmail());
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    // Fail silently, fall back to cached preferences
                }
            });
        }
    }

    private void setAvatarImage(String avatar) {
        if (avatar == null || avatar.isEmpty()) {
            binding.ivProfile.setImageResource(R.drawable.ic_account_circle);
            return;
        }

        if (avatar.startsWith("http") || avatar.startsWith("/")) {
            String fullUrl = avatar;
            if (avatar.startsWith("/")) {
                fullUrl = "https://server-testing-ymn9.onrender.com" + avatar;
            }
            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_account_circle)
                    .error(R.drawable.ic_account_circle)
                    .into(binding.ivProfile);
        } else if (avatar.length() > 200 || !avatar.contains("/")) {
            // Likely Base64 or a weird relative path without slash
            try {
                byte[] decodedString = Base64.decode(avatar, Base64.DEFAULT);
                Glide.with(this)
                        .load(decodedString)
                        .placeholder(R.drawable.ic_account_circle)
                        .error(R.drawable.ic_account_circle)
                        .into(binding.ivProfile);
            } catch (Exception e) {
                // If Base64 fails, try treating it as relative path without leading slash
                String fullUrl = "https://server-testing-ymn9.onrender.com/" + avatar;
                Glide.with(this)
                        .load(fullUrl)
                        .placeholder(R.drawable.ic_account_circle)
                        .error(R.drawable.ic_account_circle)
                        .into(binding.ivProfile);
            }
        } else {
            // Likely relative path without leading slash
            String fullUrl = "https://server-testing-ymn9.onrender.com/" + avatar;
            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_account_circle)
                    .error(R.drawable.ic_account_circle)
                    .into(binding.ivProfile);
        }
    }

    private void selectSpinnerProvince(Spinner spinner, String value) {
        if (value == null || value.isEmpty()) return;
        ArrayAdapter<Province> adapter = (ArrayAdapter<Province>) spinner.getAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                Province p = adapter.getItem(i);
                String name = p.getName().toLowerCase();
                String valLower = value.toLowerCase();

                if (name.equals(valLower)
                        || name.contains(valLower)
                        || valLower.contains(name)
                        || (valLower.replace("tp.", "thành phố").trim().contains(name))
                        || (name.replace("thành phố", "tp.").trim().contains(valLower))) {
                    spinner.setSelection(i);
                    savedCity = null; // Clear ONLY when matched
                    break;
                }
            }
        }
    }

    private void selectSpinnerDistrict(Spinner spinner, String value) {
        if (value == null || value.isEmpty()) return;
        ArrayAdapter<District> adapter = (ArrayAdapter<District>) spinner.getAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                District d = adapter.getItem(i);
                String name = d.getName().toLowerCase();
                String valLower = value.toLowerCase();

                if (name.equals(valLower)
                        || name.contains(valLower)
                        || valLower.contains(name)
                        || (valLower.replace("q.", "quận").trim().contains(name))
                        || (name.replace("quận", "q.").trim().contains(valLower))
                        || (valLower.replace("h.", "huyện").trim().contains(name))
                        || (name.replace("huyện", "h.").trim().contains(valLower))) {
                    spinner.setSelection(i);
                    savedDistrict = null; // Clear ONLY when matched
                    break;
                }
            }
        }
    }

    private void selectSpinnerWard(Spinner spinner, String value) {
        if (value == null || value.isEmpty()) return;
        ArrayAdapter<Ward> adapter = (ArrayAdapter<Ward>) spinner.getAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                Ward w = adapter.getItem(i);
                String name = w.getName().toLowerCase();
                String valLower = value.toLowerCase();

                if (name.equals(valLower)
                        || name.contains(valLower)
                        || valLower.contains(name)
                        || (valLower.replace("p.", "phường").trim().contains(name))
                        || (name.replace("phường", "p.").trim().contains(valLower))
                        || (valLower.replace("x.", "xã").trim().contains(name))
                        || (name.replace("xã", "x.").trim().contains(valLower))) {
                    spinner.setSelection(i);
                    savedWard = null; // Clear ONLY when matched
                    break;
                }
            }
        }
    }

    private String convertUriToBase64(Uri uri) {
        try {
            if (getContext() == null) return null;
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            // Decode dimensions first to avoid loading huge bitmap into memory
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // Downscale image if larger than 300px
            int maxDim = Math.max(options.outWidth, options.outHeight);
            int sampleSize = 1;
            if (maxDim > 300) {
                sampleSize = maxDim / 300;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;

            // Load and decode actual scaled bitmap
            inputStream = getContext().getContentResolver().openInputStream(uri);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (bitmap == null) return null;

            // Compress to JPEG with 70% quality to get a tiny payload (<30KB)
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, byteBuffer);
            byte[] byteArray = byteBuffer.toByteArray();

            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<Province> getFallbackProvinces() {
        List<Province> list = new ArrayList<>();
        String[] provinceNames = {
                "Thành phố Hà Nội", "Thành phố Hồ Chí Minh", "Thành phố Hải Phòng", "Thành phố Đà Nẵng", "Thành phố Cần Thơ",
                "Tỉnh Hà Giang", "Tỉnh Cao Bằng", "Tỉnh Bắc Kạn", "Tỉnh Tuyên Quang", "Tỉnh Lào Cai", "Tỉnh Điện Biên",
                "Tỉnh Lai Châu", "Tỉnh Sơn La", "Tỉnh Yên Bái", "Tỉnh Hoà Bình", "Tỉnh Thái Nguyên", "Tỉnh Lạng Sơn",
                "Tỉnh Quảng Ninh", "Tỉnh Bắc Giang", "Tỉnh Phú Thọ", "Tỉnh Vĩnh Phúc", "Tỉnh Bắc Ninh", "Tỉnh Hải Dương",
                "Tỉnh Hưng Yên", "Tỉnh Thái Bình", "Tỉnh Hà Nam", "Tỉnh Nam Định", "Tỉnh Ninh Bình", "Tỉnh Thanh Hóa",
                "Tỉnh Nghệ An", "Tỉnh Hà Tĩnh", "Tỉnh Quảng Bình", "Tỉnh Quảng Trị", "Tỉnh Thừa Thiên Huế", "Tỉnh Quảng Nam",
                "Tỉnh Quảng Ngãi", "Tỉnh Bình Định", "Tỉnh Phú Yên", "Tỉnh Khánh Hòa", "Tỉnh Ninh Thuận", "Tỉnh Bình Thuận",
                "Tỉnh Kon Tum", "Tỉnh Gia Lai", "Tỉnh Đắk Lắk", "Tỉnh Đắk Nông", "Tỉnh Lâm Đồng", "Tỉnh Bình Phước",
                "Tỉnh Tây Ninh", "Tỉnh Bình Dương", "Tỉnh Đồng Nai", "Tỉnh Bà Rịa - Vũng Tàu", "Tỉnh Long An", "Tỉnh Tiền Giang",
                "Tỉnh Bến Tre", "Tỉnh Trà Vinh", "Tỉnh Vĩnh Long", "Tỉnh Đồng Tháp", "Tỉnh An Giang", "Tỉnh Kiên Giang",
                "Tỉnh Hậu Giang", "Tỉnh Sóc Trăng", "Tỉnh Bạc Liêu", "Tỉnh Cà Mau"
        };
        for (int i = 0; i < provinceNames.length; i++) {
            list.add(new Province(provinceNames[i], i + 1, ""));
        }
        return list;
    }

    private List<District> getFallbackDistricts(Province province) {
        List<District> list = new ArrayList<>();
        if (savedDistrict != null) {
            list.add(new District(savedDistrict, 1));
        } else {
            list.add(new District("Quận / Huyện mặc định", 1));
        }
        return list;
    }

    private List<Ward> getFallbackWards(District district) {
        List<Ward> list = new ArrayList<>();
        if (savedWard != null) {
            list.add(new Ward(savedWard, 1));
        } else {
            list.add(new Ward("Phường / Xã mặc định", 1));
        }
        return list;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
