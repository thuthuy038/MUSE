package com.project.muse_android.search;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryManager {
    private static final String PREF_NAME = "MUSE_SEARCH_HISTORY";
    private static final String KEY_HISTORY = "history_list";
    private final SharedPreferences prefs;
    private final Gson gson;

    public SearchHistoryManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public void addHistory(String query) {
        List<String> history = getHistory();
        // Remove if exists to move to top
        history.remove(query);
        history.add(0, query);
        
        // Keep only last 10
        if (history.size() > 10) {
            history = history.subList(0, 10);
        }
        
        saveHistory(history);
    }

    public List<String> getHistory() {
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) return new ArrayList<>();
        
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void removeHistory(String query) {
        List<String> history = getHistory();
        history.remove(query);
        saveHistory(history);
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }

    private void saveHistory(List<String> history) {
        String json = gson.toJson(history);
        prefs.edit().putString(KEY_HISTORY, json).apply();
    }
}
