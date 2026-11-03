package com.smartclassroom.repositories;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.smartclassroom.models.Assessment;
import com.smartclassroom.models.StudentMark;
import com.smartclassroom.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class MarksRepository {
    private static MarksRepository instance;
    private final FirebaseFirestore db;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private MarksRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized MarksRepository getInstance() {
        if (instance == null) {
            instance = new MarksRepository();
        }
        return instance;
    }

    public void createAssessment(Assessment assessment, Callback<Assessment> callback) {
        DocumentReference doc = db.collection(Constants.COLLECTION_ASSESSMENTS).document();
        assessment.setAssessmentId(doc.getId());
        assessment.setCreatedAt(System.currentTimeMillis());

        doc.set(assessment)
                .addOnSuccessListener(unused -> callback.onSuccess(assessment))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenAssessments(String classroomId, String section, Callback<List<Assessment>> callback) {
        Query query = db.collection(Constants.COLLECTION_ASSESSMENTS)
                .whereEqualTo("classroomId", classroomId);
        if (section != null && !section.isEmpty() && !"All".equalsIgnoreCase(section)) {
            query = query.whereEqualTo("section", section);
        }

        return query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                callback.onError(error.getMessage());
                return;
            }
            List<Assessment> list = new ArrayList<>();
            if (snapshots != null) {
                for (var doc : snapshots.getDocuments()) {
                    Assessment a = doc.toObject(Assessment.class);
                    if (a != null) list.add(a);
                }
            }
            // Sort by createdAt descending
            list.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
            callback.onSuccess(list);
        });
    }

    public void saveStudentMarks(List<StudentMark> marksList, Callback<Void> callback) {
        WriteBatch batch = db.batch();
        long now = System.currentTimeMillis();

        for (StudentMark mark : marksList) {
            DocumentReference doc;
            if (mark.getMarkId() != null && !mark.getMarkId().isEmpty()) {
                doc = db.collection(Constants.COLLECTION_STUDENT_MARKS).document(mark.getMarkId());
            } else {
                doc = db.collection(Constants.COLLECTION_STUDENT_MARKS).document();
                mark.setMarkId(doc.getId());
            }
            mark.setUpdatedAt(now);
            batch.set(doc, mark);
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getMarksForAssessment(String assessmentId, Callback<List<StudentMark>> callback) {
        db.collection(Constants.COLLECTION_STUDENT_MARKS)
                .whereEqualTo("assessmentId", assessmentId)
                .get()
                .addOnSuccessListener(snaps -> {
                    List<StudentMark> list = new ArrayList<>();
                    if (snaps != null) {
                        for (var doc : snaps.getDocuments()) {
                            StudentMark m = doc.toObject(StudentMark.class);
                            if (m != null) list.add(m);
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenStudentMarks(String classroomId, String studentUid, Callback<List<StudentMark>> callback) {
        return db.collection(Constants.COLLECTION_STUDENT_MARKS)
                .whereEqualTo("classroomId", classroomId)
                .whereEqualTo("studentUid", studentUid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    List<StudentMark> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (var doc : snapshots.getDocuments()) {
                            StudentMark m = doc.toObject(StudentMark.class);
                            if (m != null) list.add(m);
                        }
                    }
                    callback.onSuccess(list);
                });
    }
}
