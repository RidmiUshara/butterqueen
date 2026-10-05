package lk.jiat.butterqueen.data;

import android.content.Context;
import android.content.SharedPreferences;

/** Device-only app-lock settings, scoped to the Firebase account UID. */
public final class AppLockStore {
    private static final String PREFIX = "butterqueen_app_lock_";
    private static final String ENABLED = "enabled";
    private static final String SALT = "salt";
    private static final String HASH = "hash";
    private static final String FAILURES = "failures";
    private static final String BLOCKED_UNTIL = "blocked_until";

    public enum Result { MATCH, WRONG, COOLDOWN }

    private AppLockStore() { }

    private static SharedPreferences prefs(Context context, String uid) {
        return context.getSharedPreferences(PREFIX + uid, Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context context, String uid) {
        return uid != null && prefs(context, uid).getBoolean(ENABLED, false);
    }

    public static synchronized boolean enable(Context context, String uid, char[] code) {
        if (uid == null || !PasscodeHash.valid(code) || enabled(context, uid)) return false;
        String[] verifier = PasscodeHash.create(code);
        return prefs(context, uid).edit().putString(SALT, verifier[0]).putString(HASH, verifier[1])
                .putInt(FAILURES, 0).putLong(BLOCKED_UNTIL, 0).putBoolean(ENABLED, true).commit();
    }

    public static synchronized Result verify(Context context, String uid, char[] code) {
        if (!enabled(context, uid)) return Result.WRONG;
        SharedPreferences p = prefs(context, uid);
        long now = System.currentTimeMillis();
        if (now < p.getLong(BLOCKED_UNTIL, 0)) return Result.COOLDOWN;
        boolean match = PasscodeHash.matches(code, p.getString(SALT, ""), p.getString(HASH, ""));
        if (match) {
            p.edit().putInt(FAILURES, 0).putLong(BLOCKED_UNTIL, 0).commit();
            return Result.MATCH;
        }
        int failures = Math.min(20, p.getInt(FAILURES, 0) + 1);
        long delay = failures < 5 ? 0 : Math.min(15 * 60_000L, 30_000L << Math.min(5, failures - 5));
        p.edit().putInt(FAILURES, failures).putLong(BLOCKED_UNTIL, now + delay).commit();
        return delay > 0 ? Result.COOLDOWN : Result.WRONG;
    }

    public static synchronized Result change(Context context, String uid, char[] oldCode,
                                              char[] newCode) {
        if (!PasscodeHash.valid(newCode)) return Result.WRONG;
        Result checked = verify(context, uid, oldCode);
        if (checked != Result.MATCH) return checked;
        String[] verifier = PasscodeHash.create(newCode);
        return prefs(context, uid).edit().putString(SALT, verifier[0])
                .putString(HASH, verifier[1]).commit() ? Result.MATCH : Result.WRONG;
    }

    /** Call only after verifying the existing code or reauthenticating with Firebase. */
    public static synchronized boolean disable(Context context, String uid) {
        return prefs(context, uid).edit().clear().commit();
    }
}
