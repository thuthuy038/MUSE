package com.project.muse_android.ai;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.models.Product;
import com.project.muse_android.databinding.ActivityChatBotBinding;
import com.project.muse_android.R;
import com.project.network.HomeApiClient;
import com.project.network.HomeApiService;

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
    private String geminiApiKey = "AQ.Ab8RN6JuOuokeOJYL_uT-KidNvkSjLVBSLmANTmNrin_olwz6Q";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBotBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Fix header overlap with status bar
        com.project.utils.ViewUtils.applySystemBarsPadding(binding.layoutHeader, true, false);

        binding.btnBack.setOnClickListener(v -> finish());

        // Setup Chat Recycler View
        chatAdapter = new ChatAdapter(this, messageList);
        binding.rvChatHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChatHistory.setAdapter(chatAdapter);

        // Load Products from database
        loadShopProducts();

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
            int fallbackSets = Math.min(3, shopProducts.size() / 2);
            for (int i = 0; i < fallbackSets; i++) {
                Product top = shopProducts.get(i * 2);
                Product bottom = shopProducts.get(i * 2 + 1);
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

        // Call Gemini API
        GeminiClient.getClient().generateContent(geminiApiKey, body).enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                // Remove typing indicator
                if (typingPosition < messageList.size()) {
                    messageList.remove(typingPosition);
                    chatAdapter.notifyItemRemoved(typingPosition);
                }

                if (response.isSuccessful() && response.body() != null) {
                    String reply = response.body().getText();
                    if (reply == null || reply.isEmpty()) {
                        addBotMessage("Xin lỗi bạn, kết nối có chút gián đoạn. Hãy thử hỏi lại nhé!");
                        return;
                    }
                    parseAndDisplayReply(reply);
                } else {
                    String errMessage = "Lỗi kết nối với trí tuệ nhân tạo.";
                    try {
                        if (response.errorBody() != null) {
                            errMessage += "\nChi tiết: " + response.errorBody().string();
                        } else {
                            errMessage += " Code: " + response.code();
                        }
                    } catch (Exception e) {
                        errMessage += " Code: " + response.code();
                    }
                    addBotMessage(errMessage);
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                // Remove typing indicator
                if (typingPosition < messageList.size()) {
                    messageList.remove(typingPosition);
                    chatAdapter.notifyItemRemoved(typingPosition);
                }
                addBotMessage("Lỗi kết nối mạng. Chi tiết: " + t.toString());
            }
        });
    }

    private void parseAndDisplayReply(String replyText) {
        // Extract product IDs if present inside double square brackets: [[id1, id2]]
        Pattern pattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
        Matcher matcher = pattern.matcher(replyText);

        List<Product> suggestedList = new ArrayList<>();
        String cleanedReply = replyText;

        if (matcher.find()) {
            String idsGroup = matcher.group(1);
            if (idsGroup != null && !idsGroup.trim().isEmpty()) {
                String[] ids = idsGroup.split(",");
                for (String id : ids) {
                    String cleanId = id.trim().replace("\"", "").replace("'", "");
                    // Find product matching this id from shopProducts
                    for (Product p : shopProducts) {
                        if (p.get_id().equalsIgnoreCase(cleanId)) {
                            suggestedList.add(p);
                            break;
                        }
                    }
                }
            }
            // Remove the brackets from the displayed reply text
            cleanedReply = matcher.replaceFirst("").trim();
        }

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

    private String buildSystemInstruction() {
        SharedPreferences prefs = getSharedPreferences("AI_PREFS", MODE_PRIVATE);
        String gender = prefs.getString("gender", "Nữ");
        int height = prefs.getInt("height", 160);
        int weight = prefs.getInt("weight", 50);
        String vong1 = prefs.getString("vong1", "Chưa nhập");
        String vong2 = prefs.getString("vong2", "Chưa nhập");
        String vong3 = prefs.getString("vong3", "Chưa nhập");
        String styles = prefs.getString("styles", "Tự nhiên, thanh lịch");

        // Format product list for context
        StringBuilder productsCtx = new StringBuilder();
        productsCtx.append("Dưới đây là danh sách sản phẩm hiện có tại cửa hàng MUSE để bạn lựa chọn và gợi ý cho khách hàng. Bạn CHỈ được phép gợi ý các sản phẩm trong danh sách này, không được chế ra sản phẩm không có thực:\n");
        for (Product p : shopProducts) {
            productsCtx.append(String.format("- ID: %s | Tên: %s | Giá: %s | Danh mục: %s\n",
                    p.get_id(), p.getName(), p.getPrice() + " VNĐ", p.getCategory()));
        }

        return "Bạn là trợ lý thời trang AI ảo siêu ngọt ngào, nữ tính, chu đáo của thương hiệu thời trang cao cấp MUSE.\n" +
                "Bạn sẽ trò chuyện, tư vấn phối đồ, và giải đáp thắc mắc của khách hàng dựa trên thông tin hình thể của họ:\n" +
                "- Giới tính: " + gender + "\n" +
                "- Chiều cao: " + height + " cm\n" +
                "- Cân nặng: " + weight + " kg\n" +
                "- Số đo 3 vòng: Vòng 1: " + vong1 + " cm | Vòng 2: " + vong2 + " cm | Vòng 3: " + vong3 + " cm\n" +
                "- Gu phong cách ưa thích: " + styles + "\n\n" +
                productsCtx.toString() + "\n" +
                "QUY TẮC RECOMMEND SẢN PHẨM:\n" +
                "Nếu bạn khuyên khách hàng nên mua hoặc thử một hoặc nhiều sản phẩm nào ở trên, hãy đính kèm chính xác danh sách các ID của sản phẩm đó vào cuối câu trả lời của bạn bên trong dấu ngoặc kép vuông đôi theo định dạng sau: [[id1, id2]]. Ví dụ: [[65cfb..., 65cfc...]]. Nếu không khuyên mua sản phẩm nào, không cần đính kèm.\n" +
                "Hãy trả lời bằng tiếng Việt, giọng điệu ấm áp, đáng yêu, bánh bèo, thường xuyên dùng các icon dễ thương.";
    }
}
