package com.project.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private final SharedPreferences prefs;

    private static final String KEY_TOKEN = "user_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_PROFILE_COMPLETED = "profile_completed";

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences("MUSE_PREFS", Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveUser(String userId, String name, String email) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();
    }

    private static final String KEY_USER_AVATAR = "user_avatar";

    private static final String KEY_IS_FIRST_LAUNCH = "is_first_launch";

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    public void saveAvatar(String userId, String avatarBase64) {
        if (userId != null) {
            prefs.edit().putString("user_avatar_" + userId, avatarBase64).apply();
        }
    }

    public String getAvatar(String userId) {
        if (userId == null) return null;
        return prefs.getString("user_avatar_" + userId, null);
    }

    public boolean isFirstLaunch() {
        return prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true);
    }

    public void setFirstLaunch(boolean isFirstLaunch) {
        prefs.edit().putBoolean(KEY_IS_FIRST_LAUNCH, isFirstLaunch).apply();
    }

    private static final String KEY_DONT_SHOW_OFFER_AGAIN = "dont_show_offer_again";

    public boolean isDontShowOfferAgain() {
        return prefs.getBoolean(KEY_DONT_SHOW_OFFER_AGAIN, false);
    }

    public void setDontShowOfferAgain(boolean dontShow) {
        prefs.edit().putBoolean(KEY_DONT_SHOW_OFFER_AGAIN, dontShow).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void saveProfileCompleted(boolean completed) {
        prefs.edit().putBoolean(KEY_PROFILE_COMPLETED, completed).apply();
    }

    public boolean isProfileCompleted() {
        return prefs.getBoolean(KEY_PROFILE_COMPLETED, false);
    }

    public void clearSession() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_PROFILE_COMPLETED)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();
    }
}
