package com.smartclassroom.repositories;

import android.util.Patterns;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smartclassroom.models.User;
import com.smartclassroom.utils.Constants;

public class AuthRepository {

    private static AuthRepository instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    private AuthRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository();
        }
        return instance;
    }

    // Get Current Firebase User
    public FirebaseUser getCurrentFirebaseUser() {
        return auth.getCurrentUser();
    }

    // Common Input Validation
    private String validateRegistration(
            String fullName,
            String identifier,
            String institution,
            String department,
            String email,
            String password) {

        if (fullName == null || fullName.trim().isEmpty()) {
            return "Full name is required.";
        }

        if (identifier == null || identifier.trim().isEmpty()) {
            return "Student/Teacher ID is required.";
        }

        if (institution == null || institution.trim().isEmpty()) {
            return "Institution is required.";
        }

        if (department == null || department.trim().isEmpty()) {
            return "Department is required.";
        }

        if (email == null || email.trim().isEmpty()
                || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return "Enter a valid email address.";
        }

        if (password == null || password.length() < 6) {
            return "Password must contain at least 6 characters.";
        }

        return null;
    }

    // Common Registration Method
    private void registerUser(
            String fullName,
            String identifier,
            String institution,
            String department,
            String email,
            String password,
            String role,
            AuthCallback callback) {

        String validationError = validateRegistration(
                fullName,
                identifier,
                institution,
                department,
                email,
                password
        );

        if (validationError != null) {
            callback.onError(validationError);
            return;
        }

        String cleanName = fullName.trim();
        String cleanIdentifier = identifier.trim();
        String cleanInstitution = institution.trim();
        String cleanDepartment = department.trim();
        String cleanEmail = email.trim();

        auth.createUserWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener(authResult -> {

                    FirebaseUser fbUser = authResult.getUser();

                    if (fbUser == null) {
                        callback.onError("User registration failed.");
                        return;
                    }

                    String uid = fbUser.getUid();

                    long now = System.currentTimeMillis();
                    long expiry = now + Constants.SIX_MONTHS_MILLIS;

                    User user = new User(
                            uid,
                            cleanName,
                            cleanEmail,
                            role,
                            cleanIdentifier,
                            cleanInstitution,
                            cleanDepartment,
                            expiry,
                            now
                    );

                    db.collection(Constants.COLLECTION_USERS)
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(unused ->
                                    callback.onSuccess(user))
                            .addOnFailureListener(e -> {

                                // Roll back Firebase Auth account
                                fbUser.delete()
                                        .addOnCompleteListener(task ->
                                                callback.onError(
                                                        "Profile creation failed: "
                                                                + e.getMessage()
                                                )
                                        );
                            });
                })
                .addOnFailureListener(e ->
                        callback.onError(e.getMessage()));
    }

    // Student Registration
    public void registerStudent(
            String fullName,
            String studentId,
            String institution,
            String department,
            String email,
            String password,
            AuthCallback callback) {

        registerUser(
                fullName,
                studentId,
                institution,
                department,
                email,
                password,
                Constants.ROLE_STUDENT,
                callback
        );
    }

    // Teacher Registration (Open Registration)
    public void registerTeacher(
            String fullName,
            String teacherId,
            String institution,
            String department,
            String email,
            String password,
            AuthCallback callback) {

        registerUser(
                fullName,
                teacherId,
                institution,
                department,
                email,
                password,
                Constants.ROLE_TEACHER,
                callback
        );
    }

    // Login
    public void login(
            String email,
            String password,
            AuthCallback callback) {

        if (email == null || email.trim().isEmpty()
                || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            callback.onError("Enter a valid email address.");
            return;
        }

        if (password == null || password.isEmpty()) {
            callback.onError("Password is required.");
            return;
        }

        auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(authResult -> {

                    FirebaseUser fbUser = authResult.getUser();

                    if (fbUser == null) {
                        auth.signOut();
                        callback.onError("User not found.");
                        return;
                    }

                    fetchUserDocument(
                            fbUser.getUid(),
                            true,
                            callback
                    );
                })
                .addOnFailureListener(e ->
                        callback.onError(e.getMessage()));
    }

    // Fetch User Profile
    public void fetchUserDocument(
            String uid,
            boolean refreshExpiry,
            AuthCallback callback) {

        FirebaseUser fbUser = auth.getCurrentUser();

        if (fbUser == null) {
            callback.onError("User is not authenticated.");
            return;
        }

        if (uid == null || uid.trim().isEmpty()
                || !fbUser.getUid().equals(uid)) {
            callback.onError("Unauthorized user access.");
            return;
        }

        db.collection(Constants.COLLECTION_USERS)
                .document(fbUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc == null || !doc.exists()) {
                        callback.onError("User record does not exist.");
                        return;
                    }

                    User user = doc.toObject(User.class);

                    if (user == null
                            || !fbUser.getUid().equals(user.getUid())) {
                        callback.onError("User profile data is invalid.");
                        return;
                    }

                    long now = System.currentTimeMillis();

                    // Check session expiry
                    if (user.getSessionExpiryTimestamp() > 0
                            && now > user.getSessionExpiryTimestamp()) {

                        auth.signOut();

                        callback.onError(
                                "Session expired. Please log in again."
                        );
                        return;
                    }

                    // Refresh expiry after successful login
                    if (refreshExpiry) {

                        long newExpiry =
                                now + Constants.SIX_MONTHS_MILLIS;

                        db.collection(Constants.COLLECTION_USERS)
                                .document(fbUser.getUid())
                                .update(
                                        "sessionExpiryTimestamp",
                                        newExpiry
                                )
                                .addOnSuccessListener(unused -> {

                                    user.setSessionExpiryTimestamp(newExpiry);
                                    callback.onSuccess(user);
                                })
                                .addOnFailureListener(e ->
                                        callback.onError(
                                                "Session refresh failed: "
                                                        + e.getMessage()
                                        )
                                );

                    } else {
                        callback.onSuccess(user);
                    }
                })
                .addOnFailureListener(e ->
                        callback.onError(e.getMessage()));
    }

    // Send Password Reset Email
    public void sendPasswordReset(
            String email,
            SimpleCallback callback) {

        if (email == null || email.trim().isEmpty()
                || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            callback.onError("Enter a valid email address.");
            return;
        }

        auth.sendPasswordResetEmail(email.trim())
                .addOnSuccessListener(unused ->
                        callback.onSuccess())
                .addOnFailureListener(e ->
                        callback.onError(e.getMessage()));
    }

    // Logout
    public void logout() {
        auth.signOut();
    }
}