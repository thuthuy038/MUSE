package com.project.utils;

public final class Constants {
    private Constants() {
        // Prevent instantiation
    }

    // BASE URL - lấy từ ApiClient
    public static final String BASE_URL = "https://server-testing-ymn9.onrender.com/";

    // SharedPreferences keys
    public static final String PREFS_NAME = "MUSE_PREFS";
    public static final String KEY_TOKEN = "user_token";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_EMAIL = "user_email";
    public static final String KEY_IS_LOGGED_IN = "is_logged_in";
    public static final String KEY_TERMS_ACCEPTED = "terms_accepted";
    public static final String KEY_PRIVACY_ACCEPTED = "privacy_accepted";
}
