package com.smartclassroom.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.smartclassroom.databinding.ActivitySplashBinding;
import com.smartclassroom.models.User;
import com.smartclassroom.repositories.AuthRepository;
import com.smartclassroom.utils.Constants;
import com.smartclassroom.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private SessionManager sessionManager;
    private AuthRepository authRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        authRepo = AuthRepository.getInstance();

        // 1.2 second splash delay for smooth branding
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthenticationState, 1200);
    }

    private void checkAuthenticationState() {
        FirebaseUser currentUser = authRepo.getCurrentFirebaseUser();

        // Check if user is logged in and session not expired (6-month session management)
        if (currentUser == null || !sessionManager.isLoggedIn() || sessionManager.isSessionExpired()) {
            sessionManager.clearSession();
            authRepo.logout();
            goToLogin();
            return;
        }

        // Validate backend session & role from Firestore
        authRepo.fetchUserDocument(currentUser.getUid(), true, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                sessionManager.saveUserSession(user, sessionManager.isRememberMe());
                redirectToRoleDashboard(user.getRole());
            }

            @Override
            public void onError(String message) {
                // If offline or network issue, fallback to cached session if valid
                if (sessionManager.isLoggedIn() && !sessionManager.isSessionExpired()) {
                    redirectToRoleDashboard(sessionManager.getRole());
                } else {
                    Toast.makeText(SplashActivity.this, message, Toast.LENGTH_SHORT).show();
                    sessionManager.clearSession();
                    authRepo.logout();
                    goToLogin();
                }
            }
        });
    }

    private void redirectToRoleDashboard(String role) {
        Intent intent;
        if (Constants.ROLE_TEACHER.equalsIgnoreCase(role)) {
            intent = new Intent(SplashActivity.this, TeacherMainActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, StudentMainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
