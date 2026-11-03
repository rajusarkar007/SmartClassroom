package com.smartclassroom.repositories;

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

    public FirebaseUser getCurrentFirebaseUser() {
        return auth.getCurrentUser();
    }

    public void login(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser fbUser = authResult.getUser();
                    if (fbUser == null) {
                        callback.onError("User not found");
                        return;
                    }
                    fetchUserDocument(fbUser.getUid(), true, callback);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void registerStudent(String fullName, String studentId, String institution,
                                String department, String email, String password,
                                AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser fbUser = authResult.getUser();
                    if (fbUser == null) {
                        callback.onError("User registration failed");
                        return;
                    }
                    long now = System.currentTimeMillis();
                    long expiry = now + Constants.SIX_MONTHS_MILLIS;
                    User user = new User(fbUser.getUid(), fullName, email, Constants.ROLE_STUDENT,
                            studentId, institution, department, expiry, now);

                    db.collection(Constants.COLLECTION_USERS)
                            .document(fbUser.getUid())
                            .set(user)
                            .addOnSuccessListener(unused -> callback.onSuccess(user))
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void registerTeacher(String fullName, String teacherId, String institution,
                                String department, String email, String password,
                                AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser fbUser = authResult.getUser();
                    if (fbUser == null) {
                        callback.onError("User registration failed");
                        return;
                    }
                    long now = System.currentTimeMillis();
                    long expiry = now + Constants.SIX_MONTHS_MILLIS;
                    User user = new User(fbUser.getUid(), fullName, email, Constants.ROLE_TEACHER,
                            teacherId, institution, department, expiry, now);

                    db.collection(Constants.COLLECTION_USERS)
                            .document(fbUser.getUid())
                            .set(user)
                            .addOnSuccessListener(unused -> callback.onSuccess(user))
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void fetchUserDocument(String uid, boolean refreshExpiry, AuthCallback callback) {
        db.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            long now = System.currentTimeMillis();
                            // Check backend session expiry (6 months)
                            if (user.getSessionExpiryTimestamp() > 0 && now > user.getSessionExpiryTimestamp()) {
                                auth.signOut();
                                callback.onError("Session expired. Please log in again.");
                                return;
                            }

                            if (refreshExpiry) {
                                long newExpiry = now + Constants.SIX_MONTHS_MILLIS;
                                user.setSessionExpiryTimestamp(newExpiry);
                                db.collection(Constants.COLLECTION_USERS)
                                        .document(uid)
                                        .update("sessionExpiryTimestamp", newExpiry);
                            }
                            callback.onSuccess(user);
                        } else {
                            callback.onError("User profile data is missing");
                        }
                    } else {
                        callback.onError("User record does not exist");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void sendPasswordReset(String email, SimpleCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void logout() {
        auth.signOut();
    }
}
