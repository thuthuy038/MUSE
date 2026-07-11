package com.project.muse_android.order;

import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.project.adapters.HorizontalProductAdapter;
import com.project.adapters.ReturnMediaAdapter;

import com.project.models.Order;
import com.project.models.Product;
import com.project.models.User;
import com.project.models.enums.HorizontalProductMode;
import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityReturnRefundBinding;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.app.ProgressDialog;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.project.network.ApiClient;
import com.project.utils.SessionManager;

public class ReturnRefundActivity extends AppCompatActivity {

    private ActivityReturnRefundBinding binding;
    private Order order;
    private HorizontalProductAdapter adapter;
    
    private List<Product> selectedProducts = new ArrayList<>();
    private List<Uri> selectedMedia = new ArrayList<>();
    private ReturnMediaAdapter mediaAdapter;
    
    private String selectedReason = "";
    private String selectedMethod = "";

    private ActivityResultLauncher<Intent> mediaPickerLauncher;
    private ProgressDialog progressDialog;

    private User currentUser = null;
    private boolean hasBankInfo = false;

    private static final String[] VIETNAMESE_BANKS = {
        "Chọn ngân hàng",
        "Vietcombank (VCB)",
        "VietinBank (CTG)",
        "BIDV",
        "Agribank",
        "Techcombank (TCB)",
        "MB Bank (MBB)",
        "ACB",
        "VPBank (VPB)",
        "TPBank (TPB)",
        "Sacombank (STB)",
        "VIB",
        "HDBank (HDB)",
        "SHB",
        "MSB",
        "SeABank",
        "OCB",
        "Eximbank"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityReturnRefundBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang gửi yêu cầu...");
        progressDialog.setCancelable(false);

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

        setupMediaPicker();
        setupUI();
        loadUserProfile();

        // Show the return details form directly
        binding.scrollForm.setVisibility(View.VISIBLE);
        binding.layoutBottom.setVisibility(View.VISIBLE);
        selectedProducts.clear();
        if (order.getProducts() != null) {
            selectedProducts.addAll(order.getProducts());
        }
        populateData();

        if (selectedProducts.size() > 1) {
            binding.layoutMultiProductNotice.setVisibility(View.VISIBLE);
        } else {
            binding.layoutMultiProductNotice.setVisibility(View.GONE);
        }

        validateSubmitButton();
    }

    private void setupMediaPicker() {
        mediaPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        List<Uri> tempUris = new ArrayList<>();
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                tempUris.add(result.getData().getClipData().getItemAt(i).getUri());
                            }
                        } else if (result.getData().getData() != null) {
                            tempUris.add(result.getData().getData());
                        }

                        if (selectedMedia.size() + tempUris.size() > 10) {
                            Toast.makeText(this, "Chỉ được chọn tối đa 10 tệp hình ảnh/video", Toast.LENGTH_SHORT).show();
                            int allowedCount = 10 - selectedMedia.size();
                            for (int i = 0; i < allowedCount; i++) {
                                selectedMedia.add(tempUris.get(i));
                            }
                        } else {
                            selectedMedia.addAll(tempUris);
                        }

                        mediaAdapter.notifyDataSetChanged();
                        updateMediaInfo();
                        validateSubmitButton();
                    }
                }
        );
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Setup Form RecyclerView
        adapter = new HorizontalProductAdapter(this, HorizontalProductMode.READ_ONLY, null);
        binding.rvProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProducts.setAdapter(adapter);

        binding.btnSelectReason.setOnClickListener(v -> showReasonBottomSheet());

        binding.btnSelectMethod.setOnClickListener(v -> showMethodBottomSheet());

        binding.btnUploadMedia.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            mediaPickerLauncher.launch(Intent.createChooser(intent, "Chọn hình ảnh/video"));
        });

        mediaAdapter = new ReturnMediaAdapter(this, selectedMedia, new ReturnMediaAdapter.OnMediaActionListener() {
            @Override
            public void onDelete(int position) {
                selectedMedia.remove(position);
                mediaAdapter.notifyDataSetChanged();
                updateMediaInfo();
                validateSubmitButton();
            }

            @Override
            public void onMediaClick(Uri uri, boolean isVideo) {
                // Handle media click if needed (e.g., preview)
            }
        });
        binding.rvMedia.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvMedia.setAdapter(mediaAdapter);

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

        binding.txtEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateSubmitButton();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup Bank elements
        android.widget.ArrayAdapter<String> bankAdapter = new android.widget.ArrayAdapter<>(
                this, R.layout.item_spinner_selected, VIETNAMESE_BANKS);
        bankAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        binding.spinnerBank.setAdapter(bankAdapter);

        binding.etAccountName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateSubmitButton(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etAccountNumber.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateSubmitButton(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.spinnerBank.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { validateSubmitButton(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        binding.btnChangeBank.setOnClickListener(v -> {
            binding.layoutBankSaved.setVisibility(View.GONE);
            binding.layoutBankForm.setVisibility(View.VISIBLE);
            if (currentUser != null && currentUser.getPayment() != null) {
                binding.etAccountName.setText(currentUser.getPayment().getAccountName());
                binding.etAccountNumber.setText(currentUser.getPayment().getAccountNumber());
                String savedBank = currentUser.getPayment().getBank();
                for (int i = 0; i < VIETNAMESE_BANKS.length; i++) {
                    if (VIETNAMESE_BANKS[i].equalsIgnoreCase(savedBank)) {
                        binding.spinnerBank.setSelection(i);
                        break;
                    }
                }
            }
            hasBankInfo = false;
            validateSubmitButton();
        });

        binding.btnSaveBank.setOnClickListener(v -> {
            saveBankInfoOnly();
        });

        binding.btnChatWithShop.setOnClickListener(v -> {
            Intent chatIntent = new Intent(this, com.project.muse_android.profile.ShopChatActivity.class);
            startActivity(chatIntent);
        });

        binding.btnSubmit.setOnClickListener(v -> {
            if (!hasBankInfo) {
                int selectedBankIndex = binding.spinnerBank.getSelectedItemPosition();
                if (selectedBankIndex == 0) {
                    Toast.makeText(this, "Vui lòng chọn ngân hàng nhận hoàn tiền", Toast.LENGTH_SHORT).show();
                    return;
                }
                String bankName = VIETNAMESE_BANKS[selectedBankIndex];
                String accountName = binding.etAccountName.getText().toString().trim();
                String accountNumber = binding.etAccountNumber.getText().toString().trim();

                if (accountName.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập tên chủ tài khoản", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (accountNumber.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập số tài khoản", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveBankAndSubmit(bankName, accountName, accountNumber);
            } else {
                submitReturnRequest();
            }
        });
    }

    private void loadUserProfile() {
        SessionManager sessionManager = new SessionManager(this);
        String token = sessionManager.getToken();
        if (token == null) return;

        ApiClient.INSTANCE.getInstance().getProfile("Bearer " + token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    updateBankUI();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // Ignore silently
            }
        });
    }

    private void updateBankUI() {
        if (currentUser != null && currentUser.getPayment() != null 
                && currentUser.getPayment().getAccountNumber() != null && !currentUser.getPayment().getAccountNumber().isEmpty()
                && currentUser.getPayment().getAccountName() != null && !currentUser.getPayment().getAccountName().isEmpty()
                && currentUser.getPayment().getBank() != null && !currentUser.getPayment().getBank().isEmpty()) {
            
            binding.layoutBankSaved.setVisibility(View.VISIBLE);
            binding.layoutBankForm.setVisibility(View.GONE);

            binding.txtBankNameSaved.setText(currentUser.getPayment().getBank());
            binding.txtAccountNameSaved.setText(currentUser.getPayment().getAccountName());
            
            String accNum = currentUser.getPayment().getAccountNumber();
            if (accNum.length() > 4) {
                binding.txtAccountNumberSaved.setText("******" + accNum.substring(accNum.length() - 4));
            } else {
                binding.txtAccountNumberSaved.setText(accNum);
            }
            hasBankInfo = true;
        } else {
            binding.layoutBankSaved.setVisibility(View.GONE);
            binding.layoutBankForm.setVisibility(View.VISIBLE);
            hasBankInfo = false;
        }
        validateSubmitButton();
    }

    private void saveBankInfoOnly() {
        int selectedBankIndex = binding.spinnerBank.getSelectedItemPosition();
        if (selectedBankIndex == 0) {
            Toast.makeText(this, "Vui lòng chọn ngân hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        String bankName = VIETNAMESE_BANKS[selectedBankIndex];
        String accountName = binding.etAccountName.getText().toString().trim();
        String accountNumber = binding.etAccountNumber.getText().toString().trim();

        if (accountName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên chủ tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }
        if (accountNumber.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressDialog != null) {
            progressDialog.setMessage("Đang lưu tài khoản ngân hàng...");
            progressDialog.show();
        }

        SessionManager sessionManager = new SessionManager(this);
        String token = sessionManager.getToken();
        String userId = sessionManager.getUserId();

        Map<String, Object> paymentMap = new HashMap<>();
        paymentMap.put("bank", bankName);
        paymentMap.put("accountName", accountName);
        paymentMap.put("accountNumber", accountNumber);

        Map<String, Object> userData = new HashMap<>();
        userData.put("payment", paymentMap);

        ApiClient.INSTANCE.getInstance().updateUser(userId, "Bearer " + token, userData).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (progressDialog != null) progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    Toast.makeText(ReturnRefundActivity.this, "Đã lưu tài khoản ngân hàng thành công", Toast.LENGTH_SHORT).show();
                    updateBankUI();
                } else {
                    Toast.makeText(ReturnRefundActivity.this, "Lưu tài khoản ngân hàng thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                if (progressDialog != null) progressDialog.dismiss();
                Toast.makeText(ReturnRefundActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveBankAndSubmit(String bankName, String accountName, String accountNumber) {
        if (progressDialog != null) {
            progressDialog.setMessage("Đang lưu tài khoản ngân hàng...");
            progressDialog.show();
        }

        SessionManager sessionManager = new SessionManager(this);
        String token = sessionManager.getToken();
        String userId = sessionManager.getUserId();

        Map<String, Object> paymentMap = new HashMap<>();
        paymentMap.put("bank", bankName);
        paymentMap.put("accountName", accountName);
        paymentMap.put("accountNumber", accountNumber);

        Map<String, Object> userData = new HashMap<>();
        userData.put("payment", paymentMap);

        ApiClient.INSTANCE.getInstance().updateUser(userId, "Bearer " + token, userData).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    hasBankInfo = true;
                    updateBankUI();
                    
                    if (progressDialog != null) {
                        progressDialog.setMessage("Đang gửi yêu cầu trả hàng...");
                    }
                    uploadFilesAndSubmit(0, new ArrayList<>());
                } else {
                    if (progressDialog != null) progressDialog.dismiss();
                    Toast.makeText(ReturnRefundActivity.this, "Lưu tài khoản ngân hàng thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                if (progressDialog != null) progressDialog.dismiss();
                Toast.makeText(ReturnRefundActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showReasonBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_return_reason, null);
        dialog.setContentView(view);

        RadioGroup rgReasons = view.findViewById(R.id.rgReasons);
        View btnConfirm = view.findViewById(R.id.btnConfirm);
        View btnClose = view.findViewById(R.id.btnClose);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            int selectedId = rgReasons.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton rb = view.findViewById(selectedId);
                selectedReason = rb.getText().toString();
                binding.txtReason.setText(selectedReason);
                binding.txtReason.setTextColor(getResources().getColor(android.R.color.black));
                dialog.dismiss();
                validateSubmitButton();
            } else {
                Toast.makeText(this, "Vui lòng chọn lý do", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showMethodBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_return_method, null);
        dialog.setContentView(view);

        RadioGroup rgMethods = view.findViewById(R.id.rgMethods);
        View btnConfirm = view.findViewById(R.id.btnConfirm);
        View btnClose = view.findViewById(R.id.btnClose);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            int selectedId = rgMethods.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton rb = view.findViewById(selectedId);
                selectedMethod = rb.getText().toString();
                binding.txtMethod.setText(selectedMethod);
                binding.txtMethod.setTextColor(getResources().getColor(android.R.color.black));
                dialog.dismiss();
                validateSubmitButton();
            } else {
                Toast.makeText(this, "Vui lòng chọn phương án", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void updateMediaInfo() {
        int imgCount = 0;
        int vidCount = 0;
        for (Uri uri : selectedMedia) {
            String type = getContentResolver().getType(uri);
            if (type != null) {
                if (type.startsWith("image")) imgCount++;
                else if (type.startsWith("video")) vidCount++;
            }
        }
        binding.txtMediaCount.setText("Đã chọn: " + imgCount + " ảnh, " + vidCount + " video");
    }

    private void validateSubmitButton() {
        int imgCount = 0;
        int vidCount = 0;
        for (Uri uri : selectedMedia) {
            String type = getContentResolver().getType(uri);
            if (type != null) {
                if (type.startsWith("image")) imgCount++;
                else if (type.startsWith("video")) vidCount++;
            }
        }

        String email = binding.txtEmail.getText().toString().trim();
        boolean isEmailValid = !email.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();

        boolean isBankValid = hasBankInfo || (
            binding.spinnerBank.getSelectedItemPosition() > 0 &&
            !binding.etAccountName.getText().toString().trim().isEmpty() &&
            !binding.etAccountNumber.getText().toString().trim().isEmpty()
        );

        boolean isValid = imgCount >= 3 && vidCount >= 1 && !selectedReason.isEmpty() && !selectedMethod.isEmpty() && isEmailValid && isBankValid;
        binding.btnSubmit.setEnabled(isValid);
        binding.btnSubmit.setAlpha(isValid ? 1.0f : 0.5f);
    }

    private void populateData() {
        adapter.setData(selectedProducts);
        binding.txtRefundAmount.setText(formatPrice(calculateRefundAmount()));
        
        String emailToFill = "";
        if (order.getReturnEmail() != null && !order.getReturnEmail().isEmpty()) {
            emailToFill = order.getReturnEmail();
        } else if (order.getEmail() != null && !order.getEmail().isEmpty()) {
            emailToFill = order.getEmail();
        } else {
            com.project.utils.SessionManager sessionManager = new com.project.utils.SessionManager(this);
            if (sessionManager.getUserEmail() != null) {
                emailToFill = sessionManager.getUserEmail();
            }
        }
        binding.txtEmail.setText(emailToFill);
    }

    private double calculateRefundAmount() {
        if (selectedProducts.size() == order.getProducts().size()) {
            return order.getFinalPrice();
        }
        double total = 0;
        for (Product product : selectedProducts) {
            total += product.getPrice() * (product.getQuantity() > 0 ? product.getQuantity() : 1);
        }
        return Math.min(total, order.getFinalPrice());
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#,###", symbols);
        return decimalFormat.format(price) + "đ";
    }

    private void submitReturnRequest() {
        if (progressDialog != null) {
            progressDialog.setMessage("Đang gửi yêu cầu trả hàng...");
            progressDialog.show();
        }
        uploadFilesAndSubmit(0, new ArrayList<>());
    }

    private void uploadFilesAndSubmit(final int index, final List<String> uploadedIds) {
        if (index < selectedMedia.size()) {
            Uri uri = selectedMedia.get(index);
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    if (progressDialog != null) progressDialog.dismiss();
                    Toast.makeText(this, "Không thể đọc file phương tiện", Toast.LENGTH_SHORT).show();
                    return;
                }
                String type = getContentResolver().getType(uri);
                String extension = type != null && type.contains("video") ? ".mp4" : ".jpg";
                File tempFile = new File(getCacheDir(), "return_media_" + index + extension);
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
                        MediaType.parse(type != null ? type : "image/jpeg")
                );
                MultipartBody.Part body = MultipartBody.Part.createFormData("image", tempFile.getName(), requestFile);

                ApiClient.INSTANCE.getInstance().uploadMedia(body).enqueue(new Callback<Map<String, String>>() {
                    @Override
                    public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().containsKey("fileId")) {
                            String fileId = response.body().get("fileId");
                            uploadedIds.add(fileId);
                            uploadFilesAndSubmit(index + 1, uploadedIds);
                        } else {
                            if (progressDialog != null) progressDialog.dismiss();
                            Toast.makeText(ReturnRefundActivity.this, "Tải ảnh/video lên thất bại: " + response.message(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, String>> call, Throwable t) {
                        if (progressDialog != null) progressDialog.dismiss();
                        Toast.makeText(ReturnRefundActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                if (progressDialog != null) progressDialog.dismiss();
                Toast.makeText(this, "Lỗi xử lý file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // Send update request to server
            Map<String, Object> body = new HashMap<>();
            body.put("status", "Yêu cầu trả hàng");
            body.put("returnEmail", binding.txtEmail.getText().toString().trim());
            body.put("returnReason", selectedReason);
            body.put("returnMethod", "Tài khoản ngân hàng");
            body.put("returnNote", binding.etNote.getText().toString().trim());
            body.put("returnMedia", uploadedIds);
            body.put("returnRequestedAt", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new java.util.Date()));

            // Include returnItems
            List<Map<String, Object>> returnItemsList = new ArrayList<>();
            for (Product product : selectedProducts) {
                for (Order.OrderItem item : order.getItems()) {
                    if (item.getProductId().equals(product.getId())) {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("productId", item.getProductId());
                        itemMap.put("name", item.getName());
                        itemMap.put("image", item.getImage());
                        itemMap.put("size", item.getSize());
                        itemMap.put("color", item.getColor());
                        itemMap.put("quantity", item.getQuantity());
                        itemMap.put("price", item.getPrice());
                        returnItemsList.add(itemMap);
                        break;
                    }
                }
            }
            body.put("returnItems", returnItemsList);

            ApiClient.INSTANCE.getInstance().updateOrder(order.get_id(), body).enqueue(new Callback<Order>() {
                @Override
                public void onResponse(Call<Order> call, Response<Order> response) {
                    if (progressDialog != null) progressDialog.dismiss();
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(ReturnRefundActivity.this, "Yêu cầu trả hàng đã được gửi thành công", Toast.LENGTH_SHORT).show();
                        Intent data = new Intent();
                        data.putExtra("updated_order", response.body());
                        setResult(RESULT_OK, data);
                        finish();
                    } else {
                        Toast.makeText(ReturnRefundActivity.this, "Gửi yêu cầu thất bại: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Order> call, Throwable t) {
                    if (progressDialog != null) progressDialog.dismiss();
                    Toast.makeText(ReturnRefundActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
