package lk.jiat.butterqueen.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Device preferences. Motion input is off until explicitly enabled. */
public final class AppSettings {
    private static final String PREFS = "butterqueen_settings";
    private static final String THEME = "theme";
    private static final String SHAKE = "shake_refresh";

    private AppSettings() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int themeMode(Context context) {
        int saved = prefs(context).getInt(THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        return saved == AppCompatDelegate.MODE_NIGHT_NO
                || saved == AppCompatDelegate.MODE_NIGHT_YES
                ? saved : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    public static void setThemeMode(Context context, int mode) {
        prefs(context).edit().putInt(THEME, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static boolean shakeRefresh(Context context) {
        return prefs(context).getBoolean(SHAKE, false);
    }

    public static void setShakeRefresh(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(SHAKE, enabled).apply();
    }
}
