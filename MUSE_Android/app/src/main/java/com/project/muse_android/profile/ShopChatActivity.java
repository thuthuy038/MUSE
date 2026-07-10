package com.project.muse_android.profile;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.database.AppDatabase;
import com.project.database.ShopMessage;
import com.project.models.ChatResponse;
import com.project.muse_android.BuildConfig;
import com.project.muse_android.ai.GeminiClient;
import com.project.muse_android.ai.GeminiResponse;
import com.project.muse_android.databinding.ActivityShopChatBinding;
import com.project.network.ApiClient;
import com.project.network.ApiService;
import com.project.network.ApiResponse;
import com.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShopChatActivity extends AppCompatActivity {

    private ActivityShopChatBinding binding;
    private AppDatabase db;
    private ApiService apiService;
    private SessionManager sessionManager;
    private String userId;
    private List<ShopMessage> messageList;
    private ShopChatAdapter chatAdapter;
    private String geminiApiKey;

    private static final boolean USE_SERVER_API = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityShopChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        apiService = ApiClient.INSTANCE.getInstance();
        sessionManager = new SessionManager(this);
        userId = sessionManager.getUserCode();
        if (userId == null) {
            userId = sessionManager.getUserId();
            if (userId == null) userId = "guest";

            String token = sessionManager.getToken();
            if (token != null) {
                apiService.getProfile("Bearer " + token).enqueue(new Callback<com.project.models.User>() {
                    @Override
                    public void onResponse(@NonNull Call<com.project.models.User> call, @NonNull Response<com.project.models.User> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            com.project.models.User user = response.body();
                            if (user.getCode() != null) {
                                sessionManager.saveUserCode(user.getCode());
                                userId = user.getCode();
                                loadChatHistory();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<com.project.models.User> call, @NonNull Throwable t) {
                        // Silent fallback
                    }
                });
            }
        }

        geminiApiKey = BuildConfig.GEMINI_API_KEY;

        setupUI();
        loadChatHistory();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnDeleteChat.setOnClickListener(v -> showDeleteConfirmDialog());

        binding.btnSend.setOnClickListener(v -> sendMessage());

        messageList = new ArrayList<>();
        chatAdapter = new ShopChatAdapter(this, messageList);
        binding.rvChatHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChatHistory.setAdapter(chatAdapter);
    }

    private void loadChatHistory() {
        if (USE_SERVER_API) {
            apiService.getChatMessages(userId).enqueue(new Callback<ChatResponse>() {
                @Override
                public void onResponse(@NonNull Call<ChatResponse> call, @NonNull Response<ChatResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<ShopMessage> history = response.body().getMessages();
                        messageList.clear();
                        if (history == null || history.isEmpty() || !hasAdminWelcomeToday(history)) {
                            sendWelcomeMessageToServer();
                        } else {
                            messageList.addAll(history);
                            chatAdapter.notifyDataSetChanged();
                            binding.rvChatHistory.scrollToPosition(messageList.size() - 1);
                        }
                    } else {
                        loadChatHistoryFromRoom();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ChatResponse> call, @NonNull Throwable t) {
                    loadChatHistoryFromRoom();
                }
            });
        } else {
            loadChatHistoryFromRoom();
        }
    }

    private void loadChatHistoryFromRoom() {
        new Thread(() -> {
            List<ShopMessage> history = db.shopMessageDao().getMessagesForUser(userId);
            runOnUiThread(() -> {
                messageList.clear();
                if (history == null || history.isEmpty()) {
                    new Thread(() -> {
                        ShopMessage welcome = new ShopMessage("admin", "Chào bạn! MUSE Fashion Shop có thể hỗ trợ gì cho bạn hôm nay ạ? 💕", null, String.valueOf(System.currentTimeMillis()), userId);
                        db.shopMessageDao().insert(welcome);
                        runOnUiThread(() -> {
                            messageList.add(welcome);
                            chatAdapter.notifyDataSetChanged();
                        });
                    }).start();
                } else {
                    messageList.addAll(history);
                    if (!hasAdminWelcomeToday(messageList)) {
                        new Thread(() -> {
                            ShopMessage welcome = new ShopMessage("admin", "Chào bạn! MUSE Fashion Shop có thể hỗ trợ gì cho bạn hôm nay ạ? 💕", null, String.valueOf(System.currentTimeMillis()), userId);
                            db.shopMessageDao().insert(welcome);
                            runOnUiThread(() -> {
                                messageList.add(welcome);
                                chatAdapter.notifyDataSetChanged();
                                binding.rvChatHistory.scrollToPosition(messageList.size() - 1);
                            });
                        }).start();
                    } else {
                        chatAdapter.notifyDataSetChanged();
                        binding.rvChatHistory.scrollToPosition(messageList.size() - 1);
                    }
                }
            });
        }).start();
    }

    private void sendWelcomeMessageToServer() {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("customerId", userId);
        bodyMap.put("sender", "admin");
        bodyMap.put("content", "Chào bạn! MUSE Fashion Shop có thể hỗ trợ gì cho bạn hôm nay ạ? 💕");
        bodyMap.put("image", null);
        bodyMap.put("timestamp", System.currentTimeMillis());

        apiService.sendChatMessage(bodyMap).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatResponse> call, @NonNull Response<ChatResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ShopMessage> history = response.body().getMessages();
                    if (history != null && !history.isEmpty()) {
                        messageList.clear();
                        messageList.addAll(history);
                    } else {
                        messageList.add(new ShopMessage("admin", "Chào bạn! MUSE Fashion Shop có thể hỗ trợ gì cho bạn hôm nay ạ? 💕", null, String.valueOf(System.currentTimeMillis()), userId));
                    }
                } else {
                    messageList.add(new ShopMessage("admin", "Chào bạn! MUSE Fashion Shop có thể hỗ trợ gì cho bạn hôm nay ạ? 💕", null, String.valueOf(System.currentTimeMillis()), userId));
                }
                chatAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(@NonNull Call<ChatResponse> call, @NonNull Throwable t) {
                messageList.add(new ShopMessage("admin", "Chào bạn! MUSE Fashion Shop có thể hỗ trợ gì cho bạn hôm nay ạ? 💕", null, String.valueOf(System.currentTimeMillis()), userId));
                chatAdapter.notifyDataSetChanged();
            }
        });
    }

    private void sendMessage() {
        String text = binding.etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        binding.etMessage.setText("");

        if (USE_SERVER_API) {
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("customerId", userId);
            bodyMap.put("sender", "customer");
            bodyMap.put("content", text);
            bodyMap.put("image", null);
            bodyMap.put("timestamp", System.currentTimeMillis());

            apiService.sendChatMessage(bodyMap).enqueue(new Callback<ChatResponse>() {
                @Override
                public void onResponse(@NonNull Call<ChatResponse> call, @NonNull Response<ChatResponse> response) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful() && response.body() != null) {
                            List<ShopMessage> history = response.body().getMessages();
                            if (history != null) {
                                messageList.clear();
                                messageList.addAll(history);
                                chatAdapter.notifyDataSetChanged();
                                binding.rvChatHistory.smoothScrollToPosition(messageList.size() - 1);
                            }
                        } else {
                            Toast.makeText(ShopChatActivity.this, "Lỗi gửi tin nhắn lên server.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onFailure(@NonNull Call<ChatResponse> call, @NonNull Throwable t) {
                    Toast.makeText(ShopChatActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Local Room DB flow
            new Thread(() -> {
                ShopMessage userMsg = new ShopMessage("customer", text, null, String.valueOf(System.currentTimeMillis()), userId);
                db.shopMessageDao().insert(userMsg);

                runOnUiThread(() -> {
                    messageList.add(userMsg);
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    binding.rvChatHistory.smoothScrollToPosition(messageList.size() - 1);
                });
            }).start();
        }
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa lịch sử chat")
                .setMessage("Bạn có chắc chắn muốn xóa tất cả lịch sử trò chuyện với shop?")
                .setPositiveButton("Xóa", (dialog, which) -> clearChatHistory())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void clearChatHistory() {
        if (USE_SERVER_API) {
            apiService.deleteChatHistory(userId).enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                    sendWelcomeMessageToServer();
                    Toast.makeText(ShopChatActivity.this, "Đã xóa lịch sử trò chuyện trên server.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                    Toast.makeText(ShopChatActivity.this, "Lỗi xóa lịch sử chat.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            new Thread(() -> {
                db.shopMessageDao().clearChat(userId);
                ShopMessage welcome = new ShopMessage("admin", "Chào bạn! MUSE Fashion Shop có thể hỗ trợ gì cho bạn hôm nay ạ? 💕", null, String.valueOf(System.currentTimeMillis()), userId);
                db.shopMessageDao().insert(welcome);

                runOnUiThread(() -> {
                    messageList.clear();
                    messageList.add(welcome);
                    chatAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Đã xóa lịch sử trò chuyện.", Toast.LENGTH_SHORT).show();
                });
            }).start();
        }
    }

    private boolean isToday(String timestampStr) {
        if (timestampStr == null || timestampStr.isEmpty()) return false;
        try {
            long time;
            if (timestampStr.contains("-") || timestampStr.contains("T") || timestampStr.contains(":")) {
                java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                String msgDate = isoFormat.format(isoFormat.parse(timestampStr));
                String todayDate = isoFormat.format(new java.util.Date());
                return msgDate.equals(todayDate);
            } else {
                time = Long.parseLong(timestampStr);
            }
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            String msgDate = sdf.format(new java.util.Date(time));
            String todayDate = sdf.format(new java.util.Date());
            return msgDate.equals(todayDate);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasAdminWelcomeToday(List<ShopMessage> history) {
        if (history == null || history.isEmpty()) return false;
        for (int i = history.size() - 1; i >= 0; i--) {
            ShopMessage msg = history.get(i);
            if ("admin".equalsIgnoreCase(msg.getSender()) && isToday(msg.getTimestamp())) {
                return true;
            }
        }
        return false;
    }
}
