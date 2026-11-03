package com.smartclassroom.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.smartclassroom.models.User;

public class SessionManager {
    private static final String PREF_NAME = "smart_classroom_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_UID = "uid";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_IDENTIFIER = "identifier";
    private static final String KEY_INSTITUTION = "institution";
    private static final String KEY_DEPARTMENT = "department";
    private static final String KEY_SESSION_EXPIRY = "session_expiry";
    private static final String KEY_REMEMBER_ME = "remember_me";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveUserSession(User user, boolean rememberMe) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_UID, user.getUid());
        editor.putString(KEY_FULL_NAME, user.getFullName());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_ROLE, user.getRole());
        editor.putString(KEY_IDENTIFIER, user.getIdentifier());
        editor.putString(KEY_INSTITUTION, user.getInstitution());
        editor.putString(KEY_DEPARTMENT, user.getDepartment());
        editor.putLong(KEY_SESSION_EXPIRY, user.getSessionExpiryTimestamp());
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public boolean isRememberMe() {
        return pref.getBoolean(KEY_REMEMBER_ME, true);
    }

    public boolean isSessionExpired() {
        long expiry = pref.getLong(KEY_SESSION_EXPIRY, 0L);
        return expiry > 0 && System.currentTimeMillis() > expiry;
    }

    public String getUid() {
        return pref.getString(KEY_UID, "");
    }

    public String getFullName() {
        return pref.getString(KEY_FULL_NAME, "");
    }

    public String getEmail() {
        return pref.getString(KEY_EMAIL, "");
    }

    public String getRole() {
        return pref.getString(KEY_ROLE, "");
    }

    public String getIdentifier() {
        return pref.getString(KEY_IDENTIFIER, "");
    }

    public String getInstitution() {
        return pref.getString(KEY_INSTITUTION, "");
    }

    public String getDepartment() {
        return pref.getString(KEY_DEPARTMENT, "");
    }

    public long getSessionExpiry() {
        return pref.getLong(KEY_SESSION_EXPIRY, 0L);
    }

    public User getCachedUser() {
        if (!isLoggedIn()) return null;
        User user = new User();
        user.setUid(getUid());
        user.setFullName(getFullName());
        user.setEmail(getEmail());
        user.setRole(getRole());
        user.setIdentifier(getIdentifier());
        user.setInstitution(getInstitution());
        user.setDepartment(getDepartment());
        user.setSessionExpiryTimestamp(getSessionExpiry());
        return user;
    }

    public void clearSession() {
        boolean remember = pref.getBoolean(KEY_REMEMBER_ME, true);
        editor.clear();
        editor.putBoolean(KEY_REMEMBER_ME, remember);
        editor.apply();
    }
}
