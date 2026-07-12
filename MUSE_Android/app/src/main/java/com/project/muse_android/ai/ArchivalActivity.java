package com.project.muse_android.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.muse_android.R;
import com.project.muse_android.databinding.ActivityArchivalBinding;
import com.project.models.ArchivedChat;
import com.project.models.SavedOutfit;
import com.project.adapters.ArchivedChatAdapter;
import com.project.adapters.ArchivedOutfitAdapter;
import com.project.utils.AiStorageManager;

import java.util.ArrayList;
import java.util.List;

public class ArchivalActivity extends AppCompatActivity {
    private ActivityArchivalBinding binding;
    
    private final List<ArchivedChat> archivedChats = new ArrayList<>();
    private final List<SavedOutfit> savedOutfits = new ArrayList<>();
    
    private ArchivedChatAdapter chatAdapter;
    private ArchivedOutfitAdapter outfitAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArchivalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        com.project.utils.ViewUtils.applySystemBarsPadding(binding.layoutHeader, true, false);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAiAgent.setOnClickListener(v -> navigateToAiHub());

        setupRecyclerViews();
        setupTabs();
        loadHistoryData();
    }

    private void navigateToAiHub() {
        Intent intent = new Intent(this, com.project.muse_android.main.MainActivity.class);
        intent.putExtra("open_ai_hub", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void setupRecyclerViews() {
        float density = getResources().getDisplayMetrics().density;
        int padding = (int) (16 * density);

        androidx.recyclerview.widget.RecyclerView rvChats = new androidx.recyclerview.widget.RecyclerView(this);
        rvChats.setPadding(padding, padding, padding, padding);
        rvChats.setClipToPadding(false);
        rvChats.setLayoutManager(new LinearLayoutManager(this));

        androidx.recyclerview.widget.RecyclerView rvOutfits = new androidx.recyclerview.widget.RecyclerView(this);
        rvOutfits.setPadding(padding, padding, padding, padding);
        rvOutfits.setClipToPadding(false);
        rvOutfits.setLayoutManager(new LinearLayoutManager(this));

        chatAdapter = new ArchivedChatAdapter(this, archivedChats, new ArchivedChatAdapter.OnChatClickListener() {
            @Override
            public void onChatClick(ArchivedChat chat) {
                if (chat.getMessages() != null && !chat.getMessages().isEmpty()) {
                    AiStorageManager.saveTodayChat(ArchivalActivity.this, chat.getMessages());
                }
                Intent intent = new Intent(ArchivalActivity.this, ChatBotActivity.class);
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(ArchivedChat chat, int position) {
                if (position >= 0 && position < archivedChats.size()) {
                    archivedChats.remove(position);
                    chatAdapter.notifyItemRemoved(position);
                    AiStorageManager.saveArchivedChats(ArchivalActivity.this, archivedChats);
                    Toast.makeText(ArchivalActivity.this, "Đã xóa lịch sử trò chuyện", Toast.LENGTH_SHORT).show();
                }
            }
        });
        rvChats.setAdapter(chatAdapter);

        outfitAdapter = new ArchivedOutfitAdapter(this, savedOutfits, new ArchivedOutfitAdapter.OnOutfitDeleteListener() {
            @Override
            public void onDeleteClick(SavedOutfit outfit, int position) {
                if (position >= 0 && position < savedOutfits.size()) {
                    savedOutfits.remove(position);
                    outfitAdapter.notifyItemRemoved(position);
                    AiStorageManager.saveSavedOutfits(ArchivalActivity.this, savedOutfits);
                    Toast.makeText(ArchivalActivity.this, "Đã bỏ lưu bộ phối đồ", Toast.LENGTH_SHORT).show();
                }
            }
        });
        rvOutfits.setAdapter(outfitAdapter);
        List<View> views = new ArrayList<>();
        views.add(rvChats);
        views.add(rvOutfits);
        binding.viewPager.setAdapter(new com.project.adapters.ViewPagerAdapter(views));
    }

    private void setupTabs() {
        binding.tabConversations.setOnClickListener(v -> switchTab(true));
        binding.tabSavedOutfits.setOnClickListener(v -> switchTab(false));

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

    private void switchTab(boolean isConversations) {
        binding.viewPager.setCurrentItem(isConversations ? 0 : 1, true);
    }

    private void updateTabUI(boolean isConversations) {
        binding.tabConversations.setTextColor(getResources().getColor(isConversations ? R.color.primary_500 : R.color.neutral_600));
        binding.tabSavedOutfits.setTextColor(getResources().getColor(isConversations ? R.color.neutral_600 : R.color.primary_500));

        float targetX = isConversations ? 0f : (binding.layoutHeader.getWidth() / 2f);
        binding.tabIndicator.animate().translationX(targetX).setDuration(250).start();
    }

    private void loadHistoryData() {
        archivedChats.clear();
        archivedChats.addAll(AiStorageManager.loadArchivedChats(this));
        chatAdapter.notifyDataSetChanged();

        savedOutfits.clear();
        savedOutfits.addAll(AiStorageManager.loadSavedOutfits(this));
        outfitAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryData();
    }
}
