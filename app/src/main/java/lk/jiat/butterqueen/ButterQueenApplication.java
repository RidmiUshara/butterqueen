package lk.jiat.butterqueen;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import lk.jiat.butterqueen.data.AppSettings;

public final class ButterQueenApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppSettings.themeMode(this));
        new AppLockGate(this);
    }
}
