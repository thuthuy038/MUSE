package com.project.muse_android.ai;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AiStorageManager {
    private static final String PREF_NAME = "MUSE_AI_STORAGE";
    private static final String KEY_TODAY_CHAT = "MUSE_TODAY_CHAT";
    private static final String KEY_ARCHIVED_CHATS = "MUSE_ARCHIVED_CHATS";
    private static final String KEY_SAVED_OUTFITS = "MUSE_SAVED_OUTFITS";
    
    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    private static Gson getGson() {
        return new Gson();
    }

    // --- TODAY'S CHAT SESSION ---
    public static void saveTodayChat(Context context, List<ChatMessage> messages) {
        String json = getGson().toJson(messages);
        getPrefs(context).edit().putString(KEY_TODAY_CHAT, json).apply();
    }

    public static List<ChatMessage> loadTodayChat(Context context) {
        String json = getPrefs(context).getString(KEY_TODAY_CHAT, null);
        if (json == null) return new ArrayList<>();
        
        Type type = new TypeToken<ArrayList<ChatMessage>>() {}.getType();
        return getGson().fromJson(json, type);
    }

    // --- ARCHIVED CHATS HISTORY ---
    public static List<ArchivedChat> loadArchivedChats(Context context) {
        String json = getPrefs(context).getString(KEY_ARCHIVED_CHATS, null);
        if (json == null) {
            List<ArchivedChat> emptyList = new ArrayList<>();
            saveArchivedChats(context, emptyList);
            return emptyList;
        }
        
        Type type = new TypeToken<ArrayList<ArchivedChat>>() {}.getType();
        return getGson().fromJson(json, type);
    }

    public static void saveArchivedChats(Context context, List<ArchivedChat> list) {
        String json = getGson().toJson(list);
        getPrefs(context).edit().putString(KEY_ARCHIVED_CHATS, json).apply();
    }

    public static void archiveTodayChat(Context context, List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return;
        
        List<ArchivedChat> archived = loadArchivedChats(context);
        String todayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        
        // Find if we already archived today's chat, if so update it
        int existingIndex = -1;
        for (int i = 0; i < archived.size(); i++) {
            if (archived.get(i).getDate().equals(todayDate)) {
                existingIndex = i;
                break;
            }
        }

        // Use the last message content as part of the title
        String title = "Trò chuyện ngày " + todayDate;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).isUser()) {
                title = messages.get(i).getText();
                if (title.length() > 25) {
                    title = title.substring(0, 22) + "...";
                }
                break;
            }
        }

        ArchivedChat newArchive = new ArchivedChat(
                String.valueOf(System.currentTimeMillis()),
                title,
                todayDate,
                new ArrayList<>(messages)
        );

        if (existingIndex != -1) {
            archived.set(existingIndex, newArchive);
        } else {
            archived.add(0, newArchive);
        }
        
        saveArchivedChats(context, archived);
    }

    // --- SAVED OUTFITS HISTORY ---
    public static List<SavedOutfit> loadSavedOutfits(Context context) {
        String json = getPrefs(context).getString(KEY_SAVED_OUTFITS, null);
        if (json == null) {
            // Load some mock data for first-time use
            List<SavedOutfit> mocks = new ArrayList<>();
            saveSavedOutfits(context, mocks);
            return mocks;
        }
        
        Type type = new TypeToken<ArrayList<SavedOutfit>>() {}.getType();
        return getGson().fromJson(json, type);
    }

    public static void saveSavedOutfits(Context context, List<SavedOutfit> list) {
        String json = getGson().toJson(list);
        getPrefs(context).edit().putString(KEY_SAVED_OUTFITS, json).apply();
    }

    public static void saveOutfit(Context context, SavedOutfit outfit) {
        List<SavedOutfit> list = loadSavedOutfits(context);
        
        // Avoid duplicate saves
        for (SavedOutfit o : list) {
            if (o.getSetName().equalsIgnoreCase(outfit.getSetName()) && o.getSavedDate().equals(outfit.getSavedDate())) {
                return;
            }
        }
        
        list.add(0, outfit);
        saveSavedOutfits(context, list);
    }
}
