package com.project.muse_android.ai;

import androidx.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.activity.EdgeToEdge;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.project.models.Product;
import com.project.models.User;
import com.project.muse_android.BuildConfig;
import com.project.muse_android.databinding.ActivityChatBotBinding;
import com.project.muse_android.R;
import com.project.network.ApiClient;
import com.project.network.HomeApiClient;
import com.project.network.HomeApiService;
import com.project.network.GeminiClient;
import com.project.utils.SessionManager;
import com.project.utils.AiStorageManager;
import com.project.models.ChatMessage;
import com.project.models.OutfitSet;
import com.project.models.GeminiResponse;
import com.project.adapters.ChatAdapter;
import com.project.adapters.OutfitSetAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatBotActivity extends AppCompatActivity {
    private ActivityChatBotBinding binding;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private final List<Product> shopProducts = new ArrayList<>();
    private final List<OutfitSet> outfitSets = new ArrayList<>();
    private final List<String> geminiApiKeys = new ArrayList<>();
    private int currentKeyIndex = 0;
    private User currentUser = null;
    private int originalChatPaddingBottom = 0;
    private int originalSuggestionsPaddingBottom = 0;
    private int navigationBarHeight = 0;

    private LocalGeminiApiClient localGeminiClient;

    interface LocalGeminiApiClient {
        @retrofit2.http.POST("v1beta/models/gemini-2.5-flash:generateContent")
        Call<GeminiResponse> generateContent(
            @retrofit2.http.Query("key") String apiKey,
            @retrofit2.http.Body Map<String, Object> body
        );
    }
    
    private final androidx.activity.result.ActivityResultLauncher<String> recordAudioPermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showVoiceSearchDialog();
                } else {
                    Toast.makeText(this, "Quyền ghi âm âm thanh bị từ chối.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityChatBotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Listen for Window Insets to get navigation bar and keyboard height
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            boolean isKeyboardVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime());
            
            navigationBarHeight = systemBars.bottom;
            int keyboardHeight = ime.bottom;
            
            if (isKeyboardVisible) {
                binding.layoutInputArea.setTranslationY(-keyboardHeight);
                binding.rvChatHistory.setPadding(
                        binding.rvChatHistory.getPaddingLeft(),
                        binding.rvChatHistory.getPaddingTop(),
                        binding.rvChatHistory.getPaddingRight(),
                        keyboardHeight + originalChatPaddingBottom
                );
            } else {
                if (isBottomNavHidden) {
                    binding.layoutInputArea.setTranslationY(-navigationBarHeight);
                    binding.rvChatHistory.setPadding(
                            binding.rvChatHistory.getPaddingLeft(),
                            binding.rvChatHistory.getPaddingTop(),
                            binding.rvChatHistory.getPaddingRight(),
                            navigationBarHeight + originalChatPaddingBottom
                    );
                } else {
                    int navHeight = binding.bottomNavigationView.getHeight();
                    if (navHeight == 0) {
                        navHeight = (int) (56 * getResources().getDisplayMetrics().density);
                    }
                    binding.layoutInputArea.setTranslationY(-navHeight);
                    binding.rvChatHistory.setPadding(
                            binding.rvChatHistory.getPaddingLeft(),
                            binding.rvChatHistory.getPaddingTop(),
                            binding.rvChatHistory.getPaddingRight(),
                            navHeight + originalChatPaddingBottom
                    );
                }
            }
            return windowInsets;
        });

        // Fix header overlap with status bar
        com.project.utils.ViewUtils.applySystemBarsPadding(binding.layoutHeader, true, false);

        binding.btnBack.setOnClickListener(v -> finish());

        // Setup Chat Recycler View
        chatAdapter = new ChatAdapter(this, messageList);
        binding.rvChatHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChatHistory.setAdapter(chatAdapter);

        // Load Products from database
        loadUserProfile();
        loadShopProducts();

        // Update tabs and headers with today's date
        String todayDate = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
        binding.tabSuggestions.setText("GỢI Ý " + todayDate);
        binding.tvStyleTodayHeader.setText("STYLE CỦA BẠN HÔM NAY (" + todayDate + ") ✨");

        // Load Today's saved conversation session if exists
        List<ChatMessage> todayChat = AiStorageManager.loadTodayChat(this);
        if (todayChat != null && !todayChat.isEmpty()) {
            messageList.addAll(todayChat);
            chatAdapter.notifyDataSetChanged();
            binding.rvChatHistory.scrollToPosition(messageList.size() - 1);
        } else {
            // Add Initial AI Message
            addBotMessage("Xin chào! Mình là trợ lý thời trang MUSE AI. Hãy chia sẻ cho mình gu ăn mặc của bạn hoặc dịp bạn cần phối đồ nhé! ✨");
        }

        // Send Button Click
        binding.btnSend.setOnClickListener(v -> sendMessage());

        // Voice Record Button Click
        binding.btnVoiceRecord.setOnClickListener(v -> {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                showVoiceSearchDialog();
            } else {
                recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO);
            }
        });

        // Suggestion Chips Clicks
        binding.btnChipColor.setOnClickListener(v -> {
            binding.etMessage.setText("Gợi ý phối màu sắc phù hợp với tone da của mình 🎨");
        });
        binding.btnChipAccessory.setOnClickListener(v -> {
            binding.etMessage.setText("Gợi ý thêm phụ kiện hoặc giày phối cùng set đồ này nhé ✨");
        });
        binding.btnChipStyle.setOnClickListener(v -> {
            binding.etMessage.setText("Đổi sang một phong cách khác trẻ trung năng động hơn");
        });

        // Tab Switchers
        binding.tabStylist.setOnClickListener(v -> switchTab(true));
        binding.tabSuggestions.setOnClickListener(v -> switchTab(false));

        setupViewPager();

        // Setup btnAiAgent click listener
        binding.btnAiAgent.setOnClickListener(v -> navigateToAiHub());

        // Setup Bottom Navigation
        com.project.utils.ViewUtils.setupBottomNavigation(binding.bottomNavigationView, this);

        setupFooterBehavior();

        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl("https://generativelanguage.googleapis.com/")
                .client(new okhttp3.OkHttpClient.Builder()
                        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .build())
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();
        localGeminiClient = retrofit.create(LocalGeminiApiClient.class);

        // Load keys from BuildConfig (split by comma if multiple keys are present)
        String rawKeys = BuildConfig.GEMINI_API_KEY;
        if (rawKeys != null && !rawKeys.trim().isEmpty()) {
            if (rawKeys.contains(",")) {
                String[] parts = rawKeys.split(",");
                for (String part : parts) {
                    String clean = part.trim();
                    if (!clean.isEmpty() && !geminiApiKeys.contains(clean)) {
                        geminiApiKeys.add(clean);
                    }
                }
            } else {
                String clean = rawKeys.trim();
                if (!clean.isEmpty() && !geminiApiKeys.contains(clean)) {
                    geminiApiKeys.add(clean);
                }
            }
        }
    }

    private void navigateToAiHub() {
        Intent intent = new Intent(this, com.project.muse_android.main.MainActivity.class);
        intent.putExtra("open_ai_hub", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private boolean isBottomNavHidden = false;

    private void showBottomNav() {
        if (!isBottomNavHidden) return;
        isBottomNavHidden = false;
        binding.bottomNavigationView.animate()
                .translationY(0)
                .setDuration(225)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        int navHeight = binding.bottomNavigationView.getHeight();
        if (navHeight == 0) {
            navHeight = (int) (56 * getResources().getDisplayMetrics().density);
        }
        binding.layoutInputArea.animate()
                .translationY(-navHeight)
                .setDuration(225)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        // Restore bottom padding
        final int finalNavHeight = navHeight;
        binding.rvChatHistory.setPadding(
                binding.rvChatHistory.getPaddingLeft(),
                binding.rvChatHistory.getPaddingTop(),
                binding.rvChatHistory.getPaddingRight(),
                finalNavHeight + originalChatPaddingBottom
        );
        binding.layoutSuggestionsContent.setPadding(
                binding.layoutSuggestionsContent.getPaddingLeft(),
                binding.layoutSuggestionsContent.getPaddingTop(),
                binding.layoutSuggestionsContent.getPaddingRight(),
                finalNavHeight + originalSuggestionsPaddingBottom
        );
    }

    private void hideBottomNav() {
        if (isBottomNavHidden) return;
        isBottomNavHidden = true;
        binding.bottomNavigationView.animate()
                .translationY(binding.bottomNavigationView.getHeight())
                .setDuration(175)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        binding.layoutInputArea.animate()
                .translationY(-navigationBarHeight)
                .setDuration(175)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        // Remove bottom padding (since bottom nav is hidden, but keep navigation bar padding)
        binding.rvChatHistory.setPadding(
                binding.rvChatHistory.getPaddingLeft(),
                binding.rvChatHistory.getPaddingTop(),
                binding.rvChatHistory.getPaddingRight(),
                navigationBarHeight + originalChatPaddingBottom
        );
        binding.layoutSuggestionsContent.setPadding(
                binding.layoutSuggestionsContent.getPaddingLeft(),
                binding.layoutSuggestionsContent.getPaddingTop(),
                binding.layoutSuggestionsContent.getPaddingRight(),
                navigationBarHeight + originalSuggestionsPaddingBottom
        );
    }

    private void setupFooterBehavior() {
        // Set initial state and dynamically set padding to prevent overlaps
        binding.bottomNavigationView.post(() -> {
            int navHeight = binding.bottomNavigationView.getHeight();
            if (navHeight == 0) {
                navHeight = (int) (56 * getResources().getDisplayMetrics().density);
            }
            
            // Push input area above bottom nav initially
            binding.layoutInputArea.setTranslationY(-navHeight);

            // Add bottom padding to chat history to prevent overlap
            originalChatPaddingBottom = binding.rvChatHistory.getPaddingBottom();
            binding.rvChatHistory.setPadding(
                    binding.rvChatHistory.getPaddingLeft(),
                    binding.rvChatHistory.getPaddingTop(),
                    binding.rvChatHistory.getPaddingRight(),
                    navHeight + originalChatPaddingBottom
            );

            // Add bottom padding to suggestions content
            originalSuggestionsPaddingBottom = binding.layoutSuggestionsContent.getPaddingBottom();
            binding.layoutSuggestionsContent.setPadding(
                    binding.layoutSuggestionsContent.getPaddingLeft(),
                    binding.layoutSuggestionsContent.getPaddingTop(),
                    binding.layoutSuggestionsContent.getPaddingRight(),
                    navHeight + originalSuggestionsPaddingBottom
            );
        });

        // Scroll listener for Chat (RecyclerView)
        binding.rvChatHistory.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (binding.etMessage.hasFocus()) {
                    return; // Keep hidden while inputting
                }
                if (dy > 0) {
                    hideBottomNav();
                } else if (dy < 0) {
                    showBottomNav();
                }
            }
        });

        // Scroll listener for Suggestions (NestedScrollView)
        binding.layoutSuggestionsContent.setOnScrollChangeListener(new androidx.core.widget.NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(@NonNull androidx.core.widget.NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                int dy = scrollY - oldScrollY;
                if (binding.etMessage.hasFocus()) {
                    return; // Keep hidden while inputting
                }
                if (dy > 0) {
                    hideBottomNav();
                } else if (dy < 0) {
                    showBottomNav();
                }
            }
        });

        // Focus listener for message input
        binding.etMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                hideBottomNav();
            } else {
                showBottomNav();
            }
        });

        // Global Layout Listener to clear focus and show bottom nav when keyboard is hidden
        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            private int previousHeight = 0;
            @Override
            public void onGlobalLayout() {
                if (binding == null) return;
                int height = binding.getRoot().getHeight();
                if (previousHeight != 0) {
                    if (height > previousHeight) {
                        // Keyboard hidden
                        if (binding.etMessage.hasFocus()) {
                            binding.etMessage.clearFocus();
                        }
                    }
                }
                previousHeight = height;
            }
        });
    }

    private void setupViewPager() {
        // Remove views from root constraint layout
        androidx.constraintlayout.widget.ConstraintLayout root = (androidx.constraintlayout.widget.ConstraintLayout) binding.getRoot();
        root.removeView(binding.rvChatHistory);
        root.removeView(binding.layoutInputArea);
        root.removeView(binding.layoutSuggestionsContent);

        // Container for Tab 1 (Chat)
        androidx.appcompat.widget.LinearLayoutCompat chatContainer = new androidx.appcompat.widget.LinearLayoutCompat(this);
        chatContainer.setOrientation(androidx.appcompat.widget.LinearLayoutCompat.VERTICAL);
        chatContainer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        binding.rvChatHistory.setLayoutParams(new androidx.appcompat.widget.LinearLayoutCompat.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        binding.layoutInputArea.setLayoutParams(new androidx.appcompat.widget.LinearLayoutCompat.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        chatContainer.addView(binding.rvChatHistory);
        chatContainer.addView(binding.layoutInputArea);

        // Container for Tab 2 (Suggestions)
        binding.layoutSuggestionsContent.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        binding.layoutSuggestionsContent.setVisibility(View.VISIBLE);

        // Populate ViewPager views
        List<View> pagerViews = new ArrayList<>();
        pagerViews.add(chatContainer);
        pagerViews.add(binding.layoutSuggestionsContent);

        binding.viewPager.setAdapter(new com.project.adapters.ViewPagerAdapter(pagerViews));
        binding.viewPager.setVisibility(View.VISIBLE);

        // Register Page change callback
        binding.viewPager.addOnPageChangeListener(new androidx.viewpager.widget.ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                updateTabUI(position == 0);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    private void switchTab(boolean isStylist) {
        binding.viewPager.setCurrentItem(isStylist ? 0 : 1, true);
    }

    private void updateTabUI(boolean isStylist) {
        // Update Text Colors
        binding.tabStylist.setTextColor(getResources().getColor(isStylist ? R.color.primary_500 : R.color.neutral_600));
        binding.tabSuggestions.setTextColor(getResources().getColor(isStylist ? R.color.neutral_600 : R.color.primary_500));

        // Animate Indicator Bar translationX
        float targetX = isStylist ? 0f : (binding.layoutHeader.getWidth() / 2f);
        binding.tabIndicator.animate().translationX(targetX).setDuration(250).start();

        // Populate Grid Suggestions if switching to Suggestions tab
        if (!isStylist) {
            if (outfitSets.isEmpty() && !shopProducts.isEmpty()) {
                generateOutfitSets();
            }
            if (!outfitSets.isEmpty() && binding.rvSuggestionsGrid.getAdapter() == null) {
                binding.rvSuggestionsGrid.setLayoutManager(new LinearLayoutManager(this));
                OutfitSetAdapter adapter = new OutfitSetAdapter(this, outfitSets);
                binding.rvSuggestionsGrid.setAdapter(adapter);
            }
        }
    }

    private long getTodaySeed() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year = cal.get(java.util.Calendar.YEAR);
        int month = cal.get(java.util.Calendar.MONTH);
        int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
        return year * 10000L + month * 100L + day;
    }

    private void generateOutfitSets() {
        outfitSets.clear();
        
        List<Product> tops = new ArrayList<>();
        List<Product> bottoms = new ArrayList<>();
        
        for (Product p : shopProducts) {
            if (p.getName() == null) continue;
            String name = p.getName().toLowerCase();
            if (name.contains("ao") || name.contains("áo") || name.contains("sơ mi") || name.contains("croptop") || name.contains("blazer")) {
                tops.add(p);
            } else if (name.contains("quan") || name.contains("quần") || name.contains("chân váy") || name.contains("skirt") || name.contains("short")) {
                bottoms.add(p);
            }
        }

        // Shuffle based on current date seed to vary recommendations daily
        long seed = getTodaySeed();
        java.util.Collections.shuffle(tops, new java.util.Random(seed));
        java.util.Collections.shuffle(bottoms, new java.util.Random(seed + 1));
        
        // Let's pair them! We create up to 3 sets
        int count = Math.min(3, Math.min(tops.size(), bottoms.size()));
        for (int i = 0; i < count; i++) {
            Product top = tops.get(i);
            Product bottom = bottoms.get(i);
            String setName = "BỘ PHỐI ĐỒ " + (i + 1) + ": " + getStyleNameForIndex(i);
            
            // Clean names for description
            String cleanTop = top.getName().split("(?i)nhiều màu|phù hợp")[0].trim();
            String cleanBottom = bottom.getName().split("(?i)nhiều màu|phù hợp")[0].trim();
            String desc = "Sự kết hợp tinh tế giữa " + cleanTop + " và " + cleanBottom + ".";
            
            outfitSets.add(new OutfitSet(setName, top, bottom, desc));
        }
        
        // Fallback: If we couldn't find distinct tops and bottoms, just pair any products
        if (outfitSets.isEmpty() && shopProducts.size() >= 2) {
            List<Product> fallbackList = new ArrayList<>(shopProducts);
            java.util.Collections.shuffle(fallbackList, new java.util.Random(seed + 2));
            int fallbackSets = Math.min(3, fallbackList.size() / 2);
            for (int i = 0; i < fallbackSets; i++) {
                Product top = fallbackList.get(i * 2);
                Product bottom = fallbackList.get(i * 2 + 1);
                String setName = "BỘ PHỐI ĐỒ " + (i + 1) + ": " + getStyleNameForIndex(i);
                String desc = "Phối đồ hoàn hảo giữa " + top.getName() + " và " + bottom.getName() + ".";
                outfitSets.add(new OutfitSet(setName, top, bottom, desc));
            }
        }
    }
    
    private String getStyleNameForIndex(int index) {
        if (index == 0) return "NĂNG ĐỘNG TRẺ TRUNG";
        if (index == 1) return "THANH LỊCH DẠO PHỐ";
        return "CÁ TÍNH PHÁ CÁCH";
    }

    private void loadShopProducts() {
        HomeApiClient.getApiService().getProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    shopProducts.clear();
                    shopProducts.addAll(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                // Fail silently, fallback to general advice without products list
            }
        });
    }

    private void sendMessage() {
        String input = binding.etMessage.getText().toString().trim();
        if (input.isEmpty()) return;

        // Display user message
        addUserMessage(input);
        binding.etMessage.setText("");

        // Clear focus and hide keyboard
        binding.etMessage.clearFocus();
        View focusView = this.getCurrentFocus();
        if (focusView != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
            }
        }

        // Show typing indicator or temp typing message
        int typingPosition = messageList.size();
        messageList.add(new ChatMessage("Muse đang suy nghĩ...", false));
        chatAdapter.notifyItemInserted(typingPosition);
        binding.rvChatHistory.scrollToPosition(typingPosition);

        // Construct System Instruction context
        String systemInstruction = buildSystemInstruction();

        // Prepare request payload for Gemini
        Map<String, Object> body = new HashMap<>();

        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> contentMap = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> partMap = new HashMap<>();
        partMap.put("text", input);
        parts.add(partMap);
        contentMap.put("parts", parts);
        contents.add(contentMap);
        body.put("contents", contents);

        Map<String, Object> systemInstructionMap = new HashMap<>();
        List<Map<String, Object>> systemParts = new ArrayList<>();
        Map<String, Object> systemPartMap = new HashMap<>();
        systemPartMap.put("text", systemInstruction);
        systemParts.add(systemPartMap);
        systemInstructionMap.put("parts", systemParts);
        body.put("systemInstruction", systemInstructionMap);

        // Call Gemini API with automatic key rotation and retry
        callGeminiWithRetry(currentKeyIndex, body, 0);
    }

    private void removeTypingIndicator() {
        for (int i = messageList.size() - 1; i >= 0; i--) {
            if ("Muse đang suy nghĩ...".equals(messageList.get(i).getText()) && !messageList.get(i).isUser()) {
                messageList.remove(i);
                chatAdapter.notifyItemRemoved(i);
                break;
            }
        }
    }

    private void callGeminiWithRetry(int keyIndex, Map<String, Object> body, int retryCount) {
        if (geminiApiKeys.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy API Key nào trong cấu hình!", Toast.LENGTH_LONG).show();
            removeTypingIndicator();
            showAiErrorDialog();
            return;
        }

        int index = keyIndex % geminiApiKeys.size();
        String activeKey = geminiApiKeys.get(index);

        android.util.Log.d("MUSE_Gemini", "Calling Gemini with API Key index: " + index + " (Retry: " + retryCount + ")");

        localGeminiClient.generateContent(activeKey, body).enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                // Rate limit (429), Permission/Quota issue (403), or Bad Request/Invalid Key (400)
                if (!response.isSuccessful() && (response.code() == 429 || response.code() == 403 || response.code() == 400)
                        && retryCount < geminiApiKeys.size() - 1) {
                    
                    String errorBodyStr = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBodyStr = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}

                    android.util.Log.w("MUSE_Gemini", "Key index " + index + " failed with code " + response.code() + ": " + errorBodyStr + ". Retrying with next key...");
                    
                    Toast.makeText(ChatBotActivity.this, "Key " + (index + 1) + " lỗi (" + response.code() + "). Đang chuyển sang Key tiếp theo...", Toast.LENGTH_SHORT).show();

                    // Advance to next key and retry
                    currentKeyIndex = (index + 1) % geminiApiKeys.size();
                    callGeminiWithRetry(currentKeyIndex, body, retryCount + 1);
                    return;
                }

                removeTypingIndicator();

                if (response.isSuccessful() && response.body() != null) {
                    String reply = response.body().getText();
                    if (reply == null || reply.isEmpty()) {
                        Toast.makeText(ChatBotActivity.this, "API returned empty text", Toast.LENGTH_LONG).show();
                        showAiErrorDialog();
                        return;
                    }
                    parseAndDisplayReply(reply);

                    // Successfully completed: rotate index round-robin for the next message
                    currentKeyIndex = (index + 1) % geminiApiKeys.size();
                } else {
                    String errMsg = "API Error: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errMsg += "\nDetails: " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    android.util.Log.e("MUSE_Gemini", errMsg);
                    Toast.makeText(ChatBotActivity.this, errMsg, Toast.LENGTH_LONG).show();
                    showAiErrorDialog();
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                if (retryCount < geminiApiKeys.size() - 1) {
                    android.util.Log.w("MUSE_Gemini", "Network failure. Retrying with next key...", t);
                    Toast.makeText(ChatBotActivity.this, "Lỗi kết nối. Đang chuyển sang Key tiếp theo...", Toast.LENGTH_SHORT).show();
                    currentKeyIndex = (index + 1) % geminiApiKeys.size();
                    callGeminiWithRetry(currentKeyIndex, body, retryCount + 1);
                } else {
                    removeTypingIndicator();
                    String errMsg = "Network Failure: " + t.getMessage();
                    android.util.Log.e("MUSE_Gemini", errMsg, t);
                    Toast.makeText(ChatBotActivity.this, errMsg, Toast.LENGTH_LONG).show();
                    showAiErrorDialog();
                }
            }
        });
    }

    private void parseAndDisplayReply(String replyText) {
        // Extract product IDs if present inside double square brackets: [[id1, id2]]
        Pattern pattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
        Matcher matcher = pattern.matcher(replyText);

        List<Product> suggestedList = new ArrayList<>();

        while (matcher.find()) {
            String idsGroup = matcher.group(1);
            if (idsGroup != null && !idsGroup.trim().isEmpty()) {
                String[] ids = idsGroup.split(",");
                for (String id : ids) {
                    String cleanId = id.trim().replace("\"", "").replace("'", "");
                    if (cleanId.isEmpty()) continue;
                    
                    // Find product matching this id from shopProducts
                    for (Product p : shopProducts) {
                        String pid = p.get_id() != null ? p.get_id() : p.getId();
                        if (pid != null && pid.equalsIgnoreCase(cleanId)) {
                            // Avoid duplicate recommendations
                            boolean alreadyAdded = false;
                            for (Product existing : suggestedList) {
                                String existingId = existing.get_id() != null ? existing.get_id() : existing.getId();
                                if (pid.equalsIgnoreCase(existingId)) {
                                    alreadyAdded = true;
                                    break;
                                }
                            }
                            if (!alreadyAdded) {
                                suggestedList.add(p);
                            }
                            break;
                        }
                    }
                }
            }
        }

        // Remove all brackets from the displayed reply text
        String cleanedReply = pattern.matcher(replyText).replaceAll("").trim();

        // Add message to chat list
        messageList.add(new ChatMessage(cleanedReply, false, suggestedList));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        binding.rvChatHistory.scrollToPosition(messageList.size() - 1);
        saveChatSession();
    }

    private void addUserMessage(String text) {
        messageList.add(new ChatMessage(text, true));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        binding.rvChatHistory.scrollToPosition(messageList.size() - 1);
        saveChatSession();
    }

    private void addBotMessage(String text) {
        messageList.add(new ChatMessage(text, false));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        binding.rvChatHistory.scrollToPosition(messageList.size() - 1);
        saveChatSession();
    }

    private void saveChatSession() {
        AiStorageManager.saveTodayChat(this, messageList);
        AiStorageManager.archiveTodayChat(this, messageList);
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
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // Fail silently, fallback to SharedPreferences
            }
        });
    }

    private String joinStrings(List<String> list) {
        if (list == null || list.isEmpty()) return "Chưa thiết lập";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private String getSkinToneText(int skinVal) {
        switch (skinVal) {
            case 1: return "Trắng sáng (Fair)";
            case 2: return "Trắng hồng (Medium Light)";
            case 3: return "Tự nhiên (Medium)";
            case 4: return "Hơi ngăm (Warm/Olive)";
            case 5: return "Bánh mật (Dark/Tan)";
            default: return "Chưa thiết lập";
        }
    }

    private String buildSystemInstruction() {
        SharedPreferences prefs = getSharedPreferences("AI_PREFS", MODE_PRIVATE);
        SessionManager sessionManager = new SessionManager(this);
        String userId = sessionManager.getUserId();
        String userIdKey = (userId != null) ? userId : "guest";

        // Base fallback values from SharedPreferences
        String gender = prefs.getString(userIdKey + "_gender", "female");
        int height = prefs.getInt(userIdKey + "_height", 160);
        float weight = prefs.getInt(userIdKey + "_weight", 50);
        String vong1 = prefs.getString(userIdKey + "_vong1", "Chưa nhập");
        String vong2 = prefs.getString(userIdKey + "_vong2", "Chưa nhập");
        String vong3 = prefs.getString(userIdKey + "_vong3", "Chưa nhập");
        String styles = prefs.getString(userIdKey + "_styles", "Tự nhiên, thanh lịch");

        // Extra info from SharedPreferences
        int skinVal = prefs.getInt(userIdKey + "_skin", 0);
        String skinTone = getSkinToneText(skinVal);
        String bodyShape = prefs.getString(userIdKey + "_spinner_body_shape", "Chưa thiết lập");
        String favColors = prefs.getString(userIdKey + "_spinner_color_palette", "Chưa thiết lập");
        String fashionPurpose = prefs.getString(userIdKey + "_spinner_style_vibe", "Chưa thiết lập");

        // If logged-in profile data is available, override with database values
        if (currentUser != null) {
            if (currentUser.getGender() != null) {
                gender = currentUser.getGender();
            }
            if (currentUser.getHeight() > 0) {
                height = currentUser.getHeight();
            }
            if (currentUser.getWeight() > 0) {
                weight = currentUser.getWeight();
            }
            if (currentUser.getFavoriteStyles() != null && !currentUser.getFavoriteStyles().isEmpty()) {
                styles = joinStrings(currentUser.getFavoriteStyles());
            }
            if (currentUser.getFavoriteColors() != null && !currentUser.getFavoriteColors().isEmpty()) {
                favColors = joinStrings(currentUser.getFavoriteColors());
            }
            if (currentUser.getFashionPurpose() != null && !currentUser.getFashionPurpose().isEmpty()) {
                fashionPurpose = joinStrings(currentUser.getFashionPurpose());
            }
        }

        // Auto-calculate body shape (BMI) if not set explicitly
        if (bodyShape.equals("Chưa thiết lập") || bodyShape.contains("Chưa thiết lập")) {
            if (height > 0 && weight > 0) {
                float heightM = height / 100f;
                float bmi = weight / (heightM * heightM);
                if (bmi < 18.5f) bodyShape = "Mảnh mai";
                else if (bmi >= 25f && bmi < 30f) bodyShape = "Đầy đặn";
                else if (bmi >= 30f) bodyShape = "Tròn trịa";
                else bodyShape = "Cân đối";
            }
        }

        // Localized gender
        String genderVn = "Nữ";
        if ("male".equalsIgnoreCase(gender)) {
            genderVn = "Nam";
        } else if ("other".equalsIgnoreCase(gender)) {
            genderVn = "Khác";
        }

        // Format product list for context
        StringBuilder productsCtx = new StringBuilder();
        productsCtx.append("Dưới đây là danh sách sản phẩm hiện có tại cửa hàng MUSE để bạn lựa chọn và gợi ý cho khách hàng. Bạn CHỈ được phép gợi ý các sản phẩm trong danh sách này, không được chế ra sản phẩm không có thực:\n");
        for (Product p : shopProducts) {
            productsCtx.append(String.format("- ID: %s | Tên: %s | Giá: %s | Danh mục: %s\n",
                    p.get_id(), p.getName(), p.getPrice() + " VNĐ", p.getCategory()));
        }

        return "Bạn là trợ lý thời trang AI ảo siêu ngọt ngào, nữ tính, chu đáo của thương hiệu thời trang cao cấp MUSE.\n" +
                "Bạn sẽ trò chuyện, tư vấn phối đồ, và giải đáp thắc mắc của khách hàng dựa trên thông tin hình thể và sở thích của họ để có gợi ý phù hợp nhất:\n" +
                "- Giới tính: " + genderVn + "\n" +
                "- Chiều cao: " + height + " cm\n" +
                "- Cân nặng: " + weight + " kg\n" +
                "- Dáng người: " + bodyShape + "\n" +
                "- Số đo 3 vòng: Vòng 1: " + vong1 + " cm | Vòng 2: " + vong2 + " cm | Vòng 3: " + vong3 + " cm\n" +
                "- Tone màu da: " + skinTone + "\n" +
                "- Gu phong cách ưa thích: " + styles + "\n" +
                "- Sở thích màu sắc trang phục: " + favColors + "\n" +
                "- Mục đích/Dịp diện đồ: " + fashionPurpose + "\n\n" +
                productsCtx.toString() + "\n" +
                "QUY TẮC RECOMMEND SẢN PHẨM:\n" +
                "Nếu bạn khuyên khách hàng nên mua hoặc thử một hoặc nhiều sản phẩm nào ở trên, hãy đính kèm chính xác danh sách các ID của sản phẩm đó vào cuối câu trả lời của bạn bên trong dấu ngoặc kép vuông đôi theo định dạng sau: [[id1, id2]]. Ví dụ: [[65cfb..., 65cfc...]]. Nếu không khuyên mua sản phẩm nào, không cần đính kèm.\n" +
                "Hãy trả lời bằng tiếng Việt, giọng điệu ấm áp, đáng yêu, bánh bèo, thường xuyên dùng các icon dễ thương.";
    }

    private void showVoiceSearchDialog() {
        com.project.muse_android.search.VoiceSearchDialog dialog = new com.project.muse_android.search.VoiceSearchDialog();
        dialog.setVoiceSearchListener(result -> {
            if (result != null && !result.trim().isEmpty()) {
                binding.etMessage.setText(result);
            }
        });
        dialog.show(getSupportFragmentManager(), "VoiceSearchDialog");
    }

    private void showAiErrorDialog() {
        com.project.muse_android.dialog.AiErrorDialog dialog = com.project.muse_android.dialog.AiErrorDialog.newInstance();
        dialog.show(getSupportFragmentManager(), "AiErrorDialog");
    }
}
