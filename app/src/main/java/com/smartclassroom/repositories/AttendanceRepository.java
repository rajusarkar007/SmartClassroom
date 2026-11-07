
package com.smartclassroom.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.smartclassroom.models.AttendanceRecord;
import com.smartclassroom.models.AttendanceSession;
import com.smartclassroom.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class AttendanceRepository {

    private static AttendanceRepository instance;
    private final FirebaseFirestore db;

    public static class AttendanceStats {
        public int totalConducted = 0;
        public int totalPresent = 0;
        public int totalAbsent = 0;
        public int totalLate = 0;
        public double percentage = 0.0;
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private AttendanceRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized AttendanceRepository getInstance() {
        if (instance == null) {
            instance = new AttendanceRepository();
        }
        return instance;
    }

    // Check duplicate attendance session
    public void checkDuplicateSession(
            String classroomId,
            String section,
            String date,
            int classNumber,
            Callback<Boolean> callback
    ) {
        db.collection(Constants.COLLECTION_ATTENDANCE_SESSIONS)
                .whereEqualTo("classroomId", classroomId)
                .whereEqualTo("section", section)
                .whereEqualTo("date", date)
                .whereEqualTo("classNumber", classNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(snaps -> {
                    boolean exists = snaps != null && !snaps.isEmpty();
                    callback.onSuccess(exists);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Submit attendance
    public void submitAttendance(
            AttendanceSession session,
            List<AttendanceRecord> records,
            Callback<Void> callback
    ) {

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            callback.onError("User is not authenticated.");
            return;
        }

        if (session == null || records == null || records.isEmpty()) {
            callback.onError("Invalid attendance data.");
            return;
        }

        if (records.size() + 1 > 500) {
            callback.onError("Too many attendance records for one batch.");
            return;
        }

        session.setTeacherId(currentUser.getUid());

        checkDuplicateSession(
                session.getClassroomId(),
                session.getSection(),
                session.getDate(),
                session.getClassNumber(),
                new Callback<Boolean>() {

                    @Override
                    public void onSuccess(Boolean duplicate) {

                        if (duplicate) {
                            callback.onError(
                                    "Attendance for Class #" +
                                            session.getClassNumber() +
                                            " in section " +
                                            session.getSection() +
                                            " has already been submitted."
                            );
                            return;
                        }

                        WriteBatch batch = db.batch();

                        DocumentReference sessionRef =
                                db.collection(Constants.COLLECTION_ATTENDANCE_SESSIONS)
                                        .document();

                        session.setSessionId(sessionRef.getId());
                        session.setTimestamp(System.currentTimeMillis());

                        int present = 0;
                        int absent = 0;
                        int late = 0;

                        for (AttendanceRecord record : records) {

                            if (record == null ||
                                    record.getStudentUid() == null ||
                                    record.getStudentUid().trim().isEmpty()) {

                                callback.onError("Invalid student attendance record.");
                                return;
                            }

                            DocumentReference recordRef =
                                    db.collection(Constants.COLLECTION_ATTENDANCE_RECORDS)
                                            .document();

                            record.setRecordId(recordRef.getId());
                            record.setSessionId(sessionRef.getId());
                            record.setClassroomId(session.getClassroomId());
                            record.setSection(session.getSection());
                            record.setDate(session.getDate());
                            record.setClassNumber(session.getClassNumber());
                            record.setTimestamp(session.getTimestamp());

                            String status = record.getStatus();

                            if (Constants.STATUS_PRESENT.equalsIgnoreCase(status)) {
                                present++;

                            } else if (Constants.STATUS_LATE.equalsIgnoreCase(status)) {
                                late++;

                            } else if (Constants.STATUS_ABSENT.equalsIgnoreCase(status)) {
                                absent++;

                            } else {
                                callback.onError("Invalid attendance status.");
                                return;
                            }

                            batch.set(recordRef, record);
                        }

                        session.setTotalPresent(present);
                        session.setTotalAbsent(absent);
                        session.setTotalLate(late);

                        batch.set(sessionRef, session);

                        batch.commit()
                                .addOnSuccessListener(unused ->
                                        callback.onSuccess(null))
                                .addOnFailureListener(e ->
                                        callback.onError(e.getMessage()));
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                }
        );
    }

    // Listen to classroom attendance sessions
    public ListenerRegistration listenAttendanceSessions(
            String classroomId,
            String section,
            Callback<List<AttendanceSession>> callback
    ) {

        Query query = db.collection(Constants.COLLECTION_ATTENDANCE_SESSIONS)
                .whereEqualTo("classroomId", classroomId);

        if (section != null &&
                !section.isEmpty() &&
                !"All".equalsIgnoreCase(section)) {

            query = query.whereEqualTo("section", section);
        }

        return query.addSnapshotListener((snapshots, error) -> {

            if (error != null) {
                callback.onError(error.getMessage());
                return;
            }

            List<AttendanceSession> list = new ArrayList<>();

            if (snapshots != null) {
                for (var doc : snapshots.getDocuments()) {

                    AttendanceSession session =
                            doc.toObject(AttendanceSession.class);

                    if (session != null) {
                        list.add(session);
                    }
                }
            }

            list.sort((a, b) ->
                    Integer.compare(
                            b.getClassNumber(),
                            a.getClassNumber()
                    )
            );

            callback.onSuccess(list);
        });
    }

    // Get attendance records for a session
    public void getAttendanceRecordsForSession(
            String sessionId,
            Callback<List<AttendanceRecord>> callback
    ) {

        db.collection(Constants.COLLECTION_ATTENDANCE_RECORDS)
                .whereEqualTo("sessionId", sessionId)
                .get()
                .addOnSuccessListener(snaps -> {

                    List<AttendanceRecord> list = new ArrayList<>();

                    if (snaps != null) {
                        for (var doc : snaps.getDocuments()) {

                            AttendanceRecord record =
                                    doc.toObject(AttendanceRecord.class);

                            if (record != null) {
                                list.add(record);
                            }
                        }
                    }

                    callback.onSuccess(list);
                })
                .addOnFailureListener(e ->
                        callback.onError(e.getMessage()));
    }

    // Listen to individual student attendance
    public ListenerRegistration listenStudentAttendance(
            String classroomId,
            String studentUid,
            Callback<List<AttendanceRecord>> callback
    ) {

        return db.collection(Constants.COLLECTION_ATTENDANCE_RECORDS)
                .whereEqualTo("classroomId", classroomId)
                .whereEqualTo("studentUid", studentUid)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }

                    List<AttendanceRecord> list = new ArrayList<>();

                    if (snapshots != null) {
                        for (var doc : snapshots.getDocuments()) {

                            AttendanceRecord record =
                                    doc.toObject(AttendanceRecord.class);

                            if (record != null) {
                                list.add(record);
                            }
                        }
                    }

                    callback.onSuccess(list);
                });
    }

    // Calculate individual student attendance statistics
    public void calculateStudentStats(
            String classroomId,
            String studentUid,
            Callback<AttendanceStats> callback
    ) {

        db.collection(Constants.COLLECTION_ATTENDANCE_RECORDS)
                .whereEqualTo("classroomId", classroomId)
                .whereEqualTo("studentUid", studentUid)
                .get()
                .addOnSuccessListener(snaps -> {

                    AttendanceStats stats = new AttendanceStats();

                    if (snaps != null) {

                        for (var doc : snaps.getDocuments()) {

                            AttendanceRecord record =
                                    doc.toObject(AttendanceRecord.class);

                            if (record != null) {

                                stats.totalConducted++;

                                if (Constants.STATUS_PRESENT.equalsIgnoreCase(
                                        record.getStatus())) {

                                    stats.totalPresent++;

                                } else if (Constants.STATUS_LATE.equalsIgnoreCase(
                                        record.getStatus())) {

                                    stats.totalLate++;

                                } else {
                                    stats.totalAbsent++;
                                }
                            }
                        }
                    }

                    if (stats.totalConducted > 0) {

                        stats.percentage =
                                ((double) stats.totalPresent /
                                        stats.totalConducted) * 100.0;

                    } else {
                        stats.percentage = 0.0;
                    }

                    callback.onSuccess(stats);
                })
                .addOnFailureListener(e ->
                        callback.onError(e.getMessage()));
    }
}