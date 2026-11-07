package com.smartclassroom.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

    // --------------------------------------------------
    // CREATE ASSESSMENT
    // --------------------------------------------------

    public void createAssessment(
            Assessment assessment,
            Callback<Assessment> callback
    ) {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onError("User is not authenticated.");
            return;
        }

        if (assessment == null
                || isBlank(assessment.getClassroomId())) {

            callback.onError("Invalid assessment data.");
            return;
        }

        DocumentReference doc = db
                .collection(Constants.COLLECTION_ASSESSMENTS)
                .document();

        assessment.setAssessmentId(doc.getId());
        assessment.setCreatedAt(System.currentTimeMillis());

        doc.set(assessment)
                .addOnSuccessListener(unused ->
                        callback.onSuccess(assessment))
                .addOnFailureListener(error ->
                        callback.onError(error.getMessage()));
    }

    // --------------------------------------------------
    // LISTEN TO ASSESSMENTS
    // --------------------------------------------------

    public ListenerRegistration listenAssessments(
            String classroomId,
            String section,
            Callback<List<Assessment>> callback
    ) {

        Query query = db
                .collection(Constants.COLLECTION_ASSESSMENTS)
                .whereEqualTo("classroomId", classroomId);

        if (!isBlank(section)
                && !"All".equalsIgnoreCase(section)) {

            query = query.whereEqualTo("section", section.trim());
        }

        return query.addSnapshotListener((snapshots, error) -> {

            if (error != null) {
                callback.onError(error.getMessage());
                return;
            }

            List<Assessment> list = new ArrayList<>();

            if (snapshots != null) {

                for (var doc : snapshots.getDocuments()) {

                    Assessment assessment =
                            doc.toObject(Assessment.class);

                    if (assessment != null) {

                        assessment.setAssessmentId(doc.getId());

                        list.add(assessment);
                    }
                }
            }

            list.sort((a, b) ->
                    Long.compare(
                            b.getCreatedAt(),
                            a.getCreatedAt()
                    )
            );

            callback.onSuccess(list);
        });
    }

    // --------------------------------------------------
    // SAVE OR UPDATE STUDENT MARKS
    // --------------------------------------------------

    public void saveStudentMarks(
            List<StudentMark> marksList,
            Callback<Void> callback
    ) {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onError("User is not authenticated.");
            return;
        }

        if (marksList == null || marksList.isEmpty()) {
            callback.onError("No marks data provided.");
            return;
        }

        if (marksList.size() > 499) {
            callback.onError(
                    "Maximum 499 marks can be saved in one batch."
            );
            return;
        }

        WriteBatch batch = db.batch();

        long now = System.currentTimeMillis();

        for (StudentMark mark : marksList) {

            if (mark == null
                    || isBlank(mark.getClassroomId())
                    || isBlank(mark.getAssessmentId())
                    || isBlank(mark.getStudentUid())) {

                callback.onError("Invalid student marks data.");
                return;
            }

            DocumentReference doc;

            if (!isBlank(mark.getMarkId())) {

                doc = db
                        .collection(Constants.COLLECTION_STUDENT_MARKS)
                        .document(mark.getMarkId());

            } else {

                doc = db
                        .collection(Constants.COLLECTION_STUDENT_MARKS)
                        .document();

                mark.setMarkId(doc.getId());
            }

            mark.setUpdatedAt(now);

            batch.set(doc, mark);
        }

        batch.commit()
                .addOnSuccessListener(unused ->
                        callback.onSuccess(null))
                .addOnFailureListener(error ->
                        callback.onError(error.getMessage()));
    }

    // --------------------------------------------------
    // GET MARKS FOR ONE ASSESSMENT
    // --------------------------------------------------

    public void getMarksForAssessment(
            String classroomId,
            String assessmentId,
            Callback<List<StudentMark>> callback
    ) {

        if (isBlank(classroomId) || isBlank(assessmentId)) {

            callback.onError(
                    "Classroom ID or Assessment ID is missing."
            );

            return;
        }

        db.collection(Constants.COLLECTION_STUDENT_MARKS)

                // Important Firestore security condition
                .whereEqualTo("classroomId", classroomId)

                .whereEqualTo("assessmentId", assessmentId)

                .get()

                .addOnSuccessListener(snapshots -> {

                    List<StudentMark> list = new ArrayList<>();

                    if (snapshots != null) {

                        for (var doc : snapshots.getDocuments()) {

                            StudentMark mark =
                                    doc.toObject(StudentMark.class);

                            if (mark != null) {

                                mark.setMarkId(doc.getId());

                                list.add(mark);
                            }
                        }
                    }

                    callback.onSuccess(list);
                })

                .addOnFailureListener(error ->
                        callback.onError(error.getMessage()));
    }

    // --------------------------------------------------
    // LISTEN TO INDIVIDUAL STUDENT MARKS
    // --------------------------------------------------

    public ListenerRegistration listenStudentMarks(
            String classroomId,
            String studentUid,
            Callback<List<StudentMark>> callback
    ) {

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

                            StudentMark mark =
                                    doc.toObject(StudentMark.class);

                            if (mark != null) {

                                mark.setMarkId(doc.getId());

                                list.add(mark);
                            }
                        }
                    }

                    callback.onSuccess(list);
                });
    }

    // --------------------------------------------------
    // VALIDATION
    // --------------------------------------------------

    private boolean isBlank(String value) {

        return value == null || value.trim().isEmpty();
    }
}