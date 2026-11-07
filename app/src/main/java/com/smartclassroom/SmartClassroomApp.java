package com.smartclassroom;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class SmartClassroomApp extends Application {

    private static final String PREFS_NAME = "smartclassroom_settings";
    private static final String KEY_THEME_MODE = "theme_mode";

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply saved theme before opening activities
        int savedMode = getSavedThemeMode(this);
        AppCompatDelegate.setDefaultNightMode(savedMode);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Configure Firestore offline persistence
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FirebaseFirestoreSettings settings =
                new FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build();

        db.setFirestoreSettings(settings);
    }

    // Save and apply selected theme
    public static void setThemeMode(Context context, int mode) {

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                );

        preferences.edit()
                .putInt(KEY_THEME_MODE, mode)
                .apply();

        AppCompatDelegate.setDefaultNightMode(mode);
    }

    // Get saved theme mode
    public static int getSavedThemeMode(Context context) {

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                );

        return preferences.getInt(
                KEY_THEME_MODE,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
    }
}