package com.smartclassroom.repositories;

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

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private NoticeRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized NoticeRepository getInstance() {
        if (instance == null) {
            instance = new NoticeRepository();
        }
        return instance;
    }

    public void createNotice(Notice notice, Callback<Void> callback) {
        DocumentReference doc = db.collection(Constants.COLLECTION_NOTICES).document();
        notice.setNoticeId(doc.getId());
        notice.setCreatedAt(System.currentTimeMillis());

        doc.set(notice)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteNotice(String noticeId, Callback<Void> callback) {
        db.collection(Constants.COLLECTION_NOTICES)
                .document(noticeId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenNotices(String classroomId, Callback<List<Notice>> callback) {
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
                            Notice n = doc.toObject(Notice.class);
                            if (n != null) list.add(n);
                        }
                    }
                    // Sort by createdAt descending
                    list.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    callback.onSuccess(list);
                });
    }
}
