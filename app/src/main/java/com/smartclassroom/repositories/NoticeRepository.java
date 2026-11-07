package com.smartclassroom.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.smartclassroom.models.Notice;
import com.smartclassroom.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class NoticeRepository {

    private static NoticeRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private NoticeRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized NoticeRepository getInstance() {
        if (instance == null) {
            instance = new NoticeRepository();
        }
        return instance;
    }

    private FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // Create Notice
    public void createNotice(Notice notice, Callback<Void> callback) {

        FirebaseUser user = getCurrentUser();

        if (user == null) {
            callback.onError("User is not authenticated.");
            return;
        }

        if (notice == null) {
            callback.onError("Notice cannot be empty.");
            return;
        }

        if (notice.getClassroomId() == null
                || notice.getClassroomId().trim().isEmpty()) {
            callback.onError("Classroom ID is required.");
            return;
        }

        if (notice.getTitle() == null
                || notice.getTitle().trim().isEmpty()) {
            callback.onError("Notice title is required.");
            return;
        }

        if (notice.getContent() == null
                || notice.getContent().trim().isEmpty()) {
            callback.onError("Notice content is required.");
            return;
        }

        DocumentReference doc = db.collection(Constants.COLLECTION_NOTICES)
                .document();

        notice.setNoticeId(doc.getId());
        notice.setAuthorUid(user.getUid());
        notice.setCreatedAt(System.currentTimeMillis());

        doc.set(notice)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e ->
                        callback.onError(e.getMessage()));
    }

    // Delete Notice
    public void deleteNotice(String noticeId, Callback<Void> callback) {

        FirebaseUser user = getCurrentUser();

        if (user == null) {
            callback.onError("User is not authenticated.");
            return;
        }

        if (noticeId == null || noticeId.trim().isEmpty()) {
            callback.onError("Invalid notice ID.");
            return;
        }

        db.collection(Constants.COLLECTION_NOTICES)
                .document(noticeId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e ->
                        callback.onError(e.getMessage()));
    }

    // Listen to Classroom Notices
    public ListenerRegistration listenNotices(
            String classroomId,
            Callback<List<Notice>> callback) {

        if (getCurrentUser() == null) {
            callback.onError("User is not authenticated.");
            return null;
        }

        if (classroomId == null || classroomId.trim().isEmpty()) {
            callback.onError("Invalid classroom ID.");
            return null;
        }

        return db.collection(Constants.COLLECTION_NOTICES)
                .whereEqualTo("classroomId", classroomId)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }

                    List<Notice> list = new ArrayList<>();

                    if (snapshots != null) {
                        for (var doc : snapshots.getDocuments()) {

                            Notice notice = doc.toObject(Notice.class);

                            if (notice != null) {
                                list.add(notice);
                            }
                        }
                    }

                    // Sort by createdAt descending
                    list.sort((a, b) ->
                            Long.compare(b.getCreatedAt(), a.getCreatedAt()));

                    callback.onSuccess(list);
                });
    }
}