package com.smartclassroom.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.smartclassroom.databinding.ActivityProfileBinding;
import com.smartclassroom.repositories.AuthRepository;
import com.smartclassroom.utils.Constants;
import com.smartclassroom.utils.DateUtils;
import com.smartclassroom.utils.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnLogoutProfile.setOnClickListener(v -> showLogoutDialog());

        populateProfile();
    }

    private void populateProfile() {
        binding.tvProfileName.setText(sessionManager.getFullName());
        String role = sessionManager.getRole();
        binding.tvProfileRole.setText(role);

        if (Constants.ROLE_TEACHER.equalsIgnoreCase(role)) {
            binding.tvIdLabel.setText("Teacher ID");
        } else {
            binding.tvIdLabel.setText("Student ID");
        }

        binding.tvProfileIdentifier.setText(sessionManager.getIdentifier());
        binding.tvProfileEmail.setText(sessionManager.getEmail());
        binding.tvProfileInstitution.setText(sessionManager.getInstitution());
        binding.tvProfileDepartment.setText(sessionManager.getDepartment());

        long expiry = sessionManager.getSessionExpiry();
        if (expiry > 0) {
            binding.tvProfileSessionStatus.setText("Active (Valid until " + DateUtils.formatDate(expiry) + ")");
        } else {
            binding.tvProfileSessionStatus.setText("Active");
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out of your session?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    sessionManager.clearSession();
                    AuthRepository.getInstance().logout();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
