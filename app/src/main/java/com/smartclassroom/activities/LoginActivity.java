package com.smartclassroom.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.smartclassroom.databinding.ActivityLoginBinding;
import com.smartclassroom.models.User;
import com.smartclassroom.repositories.AuthRepository;
import com.smartclassroom.utils.Constants;
import com.smartclassroom.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthRepository authRepo;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepo = AuthRepository.getInstance();
        sessionManager = new SessionManager(this);

        initListeners();
    }

    private void initListeners() {
        binding.btnLogin.setOnClickListener(v -> handleLogin());

        binding.tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        binding.btnGoStudentSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, StudentSignupActivity.class);
            startActivity(intent);
        });

        binding.btnGoTeacherSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, TeacherSignupActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Enter a valid email address");
            binding.etEmail.requestFocus();
            return;
        }
        binding.tilEmail.setError(null);

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return;
        }
        binding.tilPassword.setError(null);

        setLoading(true);

        boolean rememberMe = binding.cbRememberMe.isChecked();

        authRepo.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                sessionManager.saveUserSession(user, rememberMe);
                Toast.makeText(LoginActivity.this, "Welcome back, " + user.getFullName(), Toast.LENGTH_SHORT).show();
                redirectUser(user.getRole());
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Login Failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showForgotPasswordDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter your registered email");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email address to receive password reset instructions:")
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        authRepo.sendPasswordReset(email, new AuthRepository.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(LoginActivity.this, "Password reset email sent.", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(LoginActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(LoginActivity.this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void redirectUser(String role) {
        Intent intent;
        if (Constants.ROLE_TEACHER.equalsIgnoreCase(role)) {
            intent = new Intent(LoginActivity.this, TeacherMainActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, StudentMainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!isLoading);
        binding.btnGoStudentSignup.setEnabled(!isLoading);
        binding.btnGoTeacherSignup.setEnabled(!isLoading);
    }
}
