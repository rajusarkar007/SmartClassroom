package com.smartclassroom.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.smartclassroom.databinding.ActivityTeacherSignupBinding;
import com.smartclassroom.models.User;
import com.smartclassroom.repositories.AuthRepository;
import com.smartclassroom.utils.SessionManager;

public class TeacherSignupActivity extends AppCompatActivity {

    private ActivityTeacherSignupBinding binding;
    private AuthRepository authRepo;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTeacherSignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepo = AuthRepository.getInstance();
        sessionManager = new SessionManager(this);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSignup.setOnClickListener(v -> handleSignup());
    }

    private void handleSignup() {
        String fullName = getText(binding.etFullName);
        String teacherId = getText(binding.etTeacherId);
        String institution = getText(binding.etInstitution);
        String department = getText(binding.etDepartment);
        String email = getText(binding.etEmail);
        String password = getText(binding.etPassword);
        String confirmPassword = getText(binding.etConfirmPassword);

        if (TextUtils.isEmpty(fullName)) {
            binding.tilFullName.setError("Full name is required");
            binding.etFullName.requestFocus();
            return;
        }
        binding.tilFullName.setError(null);

        if (TextUtils.isEmpty(teacherId)) {
            binding.tilTeacherId.setError("Teacher ID is required");
            binding.etTeacherId.requestFocus();
            return;
        }
        binding.tilTeacherId.setError(null);

        if (TextUtils.isEmpty(institution)) {
            binding.tilInstitution.setError("Institution name is required");
            binding.etInstitution.requestFocus();
            return;
        }
        binding.tilInstitution.setError(null);

        if (TextUtils.isEmpty(department)) {
            binding.tilDepartment.setError("Department is required");
            binding.etDepartment.requestFocus();
            return;
        }
        binding.tilDepartment.setError(null);

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Enter a valid email address");
            binding.etEmail.requestFocus();
            return;
        }
        binding.tilEmail.setError(null);

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.tilPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus();
            return;
        }
        binding.tilPassword.setError(null);

        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError("Passwords do not match");
            binding.etConfirmPassword.requestFocus();
            return;
        }
        binding.tilConfirmPassword.setError(null);

        setLoading(true);

        authRepo.registerTeacher(fullName, teacherId, institution, department, email, password,
                new AuthRepository.AuthCallback() {
                    @Override
                    public void onSuccess(User user) {
                        setLoading(false);
                        sessionManager.saveUserSession(user, true);
                        Toast.makeText(TeacherSignupActivity.this, "Teacher Account created successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(TeacherSignupActivity.this, TeacherMainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        setLoading(false);
                        Toast.makeText(TeacherSignupActivity.this, "Signup Failed: " + message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String getText(android.widget.EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnSignup.setEnabled(!isLoading);
        binding.btnBack.setEnabled(!isLoading);
    }
}
