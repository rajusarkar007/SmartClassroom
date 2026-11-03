package com.smartclassroom.repositories;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.smartclassroom.models.ClassSchedule;
import com.smartclassroom.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class ScheduleRepository {
    private static ScheduleRepository instance;
    private final FirebaseFirestore db;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private ScheduleRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized ScheduleRepository getInstance() {
        if (instance == null) {
            instance = new ScheduleRepository();
        }
        return instance;
    }

    public void createSchedule(ClassSchedule schedule, Callback<Void> callback) {
        DocumentReference doc = db.collection(Constants.COLLECTION_CLASS_SCHEDULES).document();
        schedule.setScheduleId(doc.getId());
        schedule.setCreatedAt(System.currentTimeMillis());

        doc.set(schedule)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateSchedule(ClassSchedule schedule, Callback<Void> callback) {
        db.collection(Constants.COLLECTION_CLASS_SCHEDULES)
                .document(schedule.getScheduleId())
                .set(schedule)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void toggleClassCancelled(String scheduleId, boolean isCancelled, Callback<Void> callback) {
        db.collection(Constants.COLLECTION_CLASS_SCHEDULES)
                .document(scheduleId)
                .update("cancelled", isCancelled)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenSchedules(String classroomId, String section, Callback<List<ClassSchedule>> callback) {
        Query query = db.collection(Constants.COLLECTION_CLASS_SCHEDULES)
                .whereEqualTo("classroomId", classroomId);
        if (section != null && !section.isEmpty() && !"All".equalsIgnoreCase(section)) {
            query = query.whereEqualTo("section", section);
        }

        return query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                callback.onError(error.getMessage());
                return;
            }
            List<ClassSchedule> list = new ArrayList<>();
            if (snapshots != null) {
                for (var doc : snapshots.getDocuments()) {
                    ClassSchedule s = doc.toObject(ClassSchedule.class);
                    if (s != null) list.add(s);
                }
            }
            // Sort by createdAt descending
            list.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
            callback.onSuccess(list);
        });
    }
}
