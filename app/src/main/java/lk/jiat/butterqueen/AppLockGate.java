package lk.jiat.butterqueen;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.jiat.butterqueen.activity.AppLockActivity;
import lk.jiat.butterqueen.activity.SignUpActivity;
import lk.jiat.butterqueen.activity.SignInActivity;
import lk.jiat.butterqueen.activity.splashActivity;
import lk.jiat.butterqueen.data.AppLockStore;

/** Hides signed-in screens before they draw until the app is unlocked this foreground session. */
public final class AppLockGate implements Application.ActivityLifecycleCallbacks {
    private static AppLockGate instance;
    private int startedActivities;
    private int lockScreens;
    private boolean launchingLock;
    private String unlockedUid;

    public AppLockGate(Application app) {
        instance = this;
        app.registerActivityLifecycleCallbacks(this);
    }

    public static void unlock(String uid) {
        if (instance != null) instance.unlockedUid = uid;
    }

    public static void lockNow() {
        if (instance != null) instance.unlockedUid = null;
    }

    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle state) {
        if (activity instanceof AppLockActivity) {
            lockScreens++;
            launchingLock = false;
        }
    }

    @Override public void onActivityStarted(@NonNull Activity activity) {
        if (startedActivities++ == 0) unlockedUid = null;
        guard(activity);
    }

    @Override public void onActivityResumed(@NonNull Activity activity) {
        if (activity instanceof AppLockActivity) launchingLock = false;
        guard(activity);
    }

    private void guard(Activity activity) {
        if (activity instanceof splashActivity || activity instanceof SignInActivity
                || activity instanceof SignUpActivity || activity instanceof AppLockActivity
                || activity.isFinishing()) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean enabled = user != null && AppLockStore.enabled(activity, user.getUid());
        if (enabled) activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        else activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if (enabled
                && !user.getUid().equals(unlockedUid)) {
            activity.getWindow().getDecorView().setVisibility(View.INVISIBLE);
            if (launchingLock) return;
            Intent lock = new Intent(activity, AppLockActivity.class);
            lock.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (lockScreens > 0) lock.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            launchingLock = true;
            activity.startActivity(lock);
        } else {
            activity.getWindow().getDecorView().setVisibility(View.VISIBLE);
        }
    }

    @Override public void onActivityPaused(@NonNull Activity activity) { }

    @Override public void onActivityStopped(@NonNull Activity activity) {
        startedActivities = Math.max(0, startedActivities - 1);
        if (startedActivities == 0) unlockedUid = null;
    }

    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle state) { }

    @Override public void onActivityDestroyed(@NonNull Activity activity) {
        if (activity instanceof AppLockActivity) {
            lockScreens = Math.max(0, lockScreens - 1);
            launchingLock = false;
        }
    }
}
