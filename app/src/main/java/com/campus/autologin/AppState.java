package com.campus.autologin;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppState {
    private static final String PREFS = "campus_state";

    private AppState() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void setAutoEnabled(Context c, boolean enabled) {
        p(c).edit().putBoolean("auto_enabled", enabled).apply();
    }

    public static boolean isAutoEnabled(Context c) {
        return p(c).getBoolean("auto_enabled", false);
    }

    public static void setStatus(Context c, String status) {
        p(c).edit()
                .putString("last_status", status)
                .putLong("last_status_time", System.currentTimeMillis())
                .apply();
    }

    public static String getStatus(Context c) {
        return p(c).getString("last_status", "尚未运行");
    }

    public static long getStatusTime(Context c) {
        return p(c).getLong("last_status_time", 0L);
    }
}
