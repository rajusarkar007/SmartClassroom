
package com.smartclassroom.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import com.smartclassroom.models.ClassSchedule;
import com.smartclassroom.utils.Constants;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ScheduleRepository {

    private static ScheduleRepository instance;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private ScheduleRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized ScheduleRepository getInstance() {
        if (instance == null) {
            instance = new ScheduleRepository();
        }
        return instance;
    }

    private FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // Convert date and time into timestamp
    private long getTimestamp(String date, String time) {

        if (date == null || time == null) {
            return 0;
        }

        String[] datePatterns = {
                "dd/MM/yyyy",
                "dd-MM-yyyy",
                "yyyy-MM-dd",
                "dd MMM yyyy",
                "MMM dd, yyyy",
                "MM/dd/yyyy"
        };

        String[] timePatterns = {
                "hh:mm a",
                "h:mm a",
                "HH:mm",
                "hh:mm",
                "H:mm"
        };

        String combined = date.trim() + " " + time.trim();

        for (String datePattern : datePatterns) {
            for (String timePattern : timePatterns) {

                SimpleDateFormat format = new SimpleDateFormat(
                        datePattern + " " + timePattern,
                        Locale.ENGLISH
                );

                format.setLenient(false);
                format.setTimeZone(
                        TimeZone.getTimeZone("Asia/Dhaka")
                );

                ParsePosition position = new ParsePosition(0);
                Date parsedDate = format.parse(combined, position);

                if (parsedDate != null &&
                        position.getIndex() == combined.length()) {
                    return parsedDate.getTime();
                }
            }
        }

        return 0;
    }

    private void prepareTimestamps(ClassSchedule schedule) {

        long start = getTimestamp(
                schedule.getDate(),
                schedule.getStartTime()
        );

        long end = getTimestamp(
                schedule.getDate(),
                schedule.getEndTime()
        );

        if (start > 0) {
            schedule.setStartTimestamp(start);
        }

        if (end > 0) {
            schedule.setEndTimestamp(end);
        }
    }

    // Create Schedule
    public void createSchedule(
            ClassSchedule schedule,
            Callback<Void> callback) {

        FirebaseUser user = getCurrentUser();

        if (user == null) {
            callback.onError("User is not authenticated.");
            return;
        }

        if (schedule == null) {
            callback.onError("Schedule cannot be empty.");
            return;
        }

        if (schedule.getClassroomId() == null ||
                schedule.getClassroomId().trim().isEmpty()) {
            callback.onError("Classroom ID is required.");
            return;
        }

        if (schedule.getTitle() == null ||
                schedule.getTitle().trim().isEmpty()) {
            callback.onError("Schedule title is required.");
            return;
        }

        prepareTimestamps(schedule);

        if (schedule.getStartTimestamp() <= 0 ||
                schedule.getEndTimestamp() <= 0 ||
                schedule.getEndTimestamp() <= schedule.getStartTimestamp()) {
            callback.onError("Invalid date or class time.");
            return;
        }

        DocumentReference doc = db.collection(
                Constants.COLLECTION_CLASS_SCHEDULES
        ).document();

        schedule.setScheduleId(doc.getId());
        schedule.setCreatedByUid(user.getUid());
        schedule.setCreatedAt(System.currentTimeMillis());

        doc.set(schedule)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e ->
                        callback.onError(e.getMessage()));
    }

    // Update Schedule
    public void updateSchedule(
            ClassSchedule schedule,
            Callback<Void> callback) {

        FirebaseUser user = getCurrentUser();

        if (user == null) {
            callback.onError("User is not authenticated.");
            return;
        }

        if (schedule == null ||
                schedule.getScheduleId() == null ||
                schedule.getScheduleId().trim().isEmpty()) {
            callback.onError("Invalid schedule.");
            return;
        }

        if (schedule.getClassroomId() == null ||
                schedule.getClassroomId().trim().isEmpty()) {
            callback.onError("Classroom ID is required.");
            return;
        }

        prepareTimestamps(schedule);

        if (schedule.getStartTimestamp() <= 0 ||
                schedule.getEndTimestamp() <= 0 ||
                schedule.getEndTimestamp() <= schedule.getStartTimestamp()) {
            callback.onError("Invalid date or class time.");
            return;
        }

        DocumentReference doc = db.collection(
                Constants.COLLECTION_CLASS_SCHEDULES
        ).document(schedule.getScheduleId());

        doc.get().addOnSuccessListener(snapshot -> {

            if (!snapshot.exists()) {
                callback.onError("Schedule not found.");
                return;
            }

            schedule.setCreatedByUid(
                    snapshot.getString("createdByUid")
            );

            Long createdAt = snapshot.getLong("createdAt");

            if (createdAt != null) {
                schedule.setCreatedAt(createdAt);
            }

            doc.set(schedule)
                    .addOnSuccessListener(unused ->
                            callback.onSuccess(null))
                    .addOnFailureListener(e ->
                            callback.onError(e.getMessage()));

        }).addOnFailureListener(e ->
                callback.onError(e.getMessage()));
    }

    // Delete Schedule
    public void deleteSchedule(
            String scheduleId,
            Callback<Void> callback) {

        if (getCurrentUser() == null) {
            callback.onError("User is not authenticated.");
            return;
        }

        if (scheduleId == null || scheduleId.trim().isEmpty()) {
            callback.onError("Invalid schedule ID.");
            return;
        }

        db.collection(Constants.COLLECTION_CLASS_SCHEDULES)
                .document(scheduleId)
                .delete()
                .addOnSuccessListener(unused ->
                        callback.onSuccess(null))
                .addOnFailureListener(e ->
                        callback.onError(e.getMessage()));
    }

    // Toggle Cancelled Status
    public void toggleClassCancelled(
            String scheduleId,
            boolean isCancelled,
            Callback<Void> callback) {

        if (getCurrentUser() == null) {
            callback.onError("User is not authenticated.");
            return;
        }

        if (scheduleId == null || scheduleId.trim().isEmpty()) {
            callback.onError("Invalid schedule ID.");
            return;
        }

        db.collection(Constants.COLLECTION_CLASS_SCHEDULES)
                .document(scheduleId)
                .update("cancelled", isCancelled)
                .addOnSuccessListener(unused ->
                        callback.onSuccess(null))
                .addOnFailureListener(e ->
                        callback.onError(e.getMessage()));
    }

    // Check Teacher or CR permission
    public void checkManagerPermission(
            String classroomId,
            Callback<Boolean> callback) {

        FirebaseUser user = getCurrentUser();

        if (user == null ||
                classroomId == null ||
                classroomId.trim().isEmpty()) {
            callback.onSuccess(false);
            return;
        }

        db.collection("classrooms")
                .document(classroomId)
                .get()
                .addOnSuccessListener(classroomSnapshot -> {

                    if (classroomSnapshot.exists()) {

                        String teacherId =
                                classroomSnapshot.getString("teacherId");

                        if (user.getUid().equals(teacherId)) {
                            callback.onSuccess(true);
                            return;
                        }
                    }

                    String memberId =
                            classroomId + "_" + user.getUid();

                    db.collection("classroom_members")
                            .document(memberId)
                            .get()
                            .addOnSuccessListener(memberSnapshot -> {

                                boolean isCR =
                                        memberSnapshot.exists()
                                                && classroomId.equals(
                                                memberSnapshot.getString("classroomId"))
                                                && user.getUid().equals(
                                                memberSnapshot.getString("studentUid"))
                                                && Boolean.TRUE.equals(
                                                memberSnapshot.getBoolean(
                                                        "classRepresentative"));

                                callback.onSuccess(isCR);

                            })
                            .addOnFailureListener(e ->
                                    callback.onSuccess(false));

                })
                .addOnFailureListener(e ->
                        callback.onSuccess(false));
    }

    // Delete expired schedules
    private void deleteExpiredSchedules(
            String classroomId,
            List<ClassSchedule> schedules) {

        checkManagerPermission(
                classroomId,
                new Callback<Boolean>() {

                    @Override
                    public void onSuccess(Boolean canManage) {

                        if (!Boolean.TRUE.equals(canManage)) {
                            return;
                        }

                        long currentTime = System.currentTimeMillis();

                        WriteBatch batch = db.batch();
                        int deleteCount = 0;

                        for (ClassSchedule schedule : schedules) {

                            long endTimestamp =
                                    schedule.getEndTimestamp();

                            if (endTimestamp > 0 &&
                                    endTimestamp <= currentTime &&
                                    schedule.getScheduleId() != null &&
                                    !schedule.getScheduleId().trim().isEmpty()) {

                                DocumentReference ref = db.collection(
                                        Constants.COLLECTION_CLASS_SCHEDULES
                                ).document(schedule.getScheduleId());

                                batch.delete(ref);
                                deleteCount++;
                            }
                        }

                        if (deleteCount > 0) {
                            batch.commit();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        // Permission check failed.
                    }
                }
        );
    }

    // Listen to Classroom Schedules
    public ListenerRegistration listenSchedules(
            String classroomId,
            String section,
            Callback<List<ClassSchedule>> callback) {

        if (getCurrentUser() == null) {
            callback.onError("User is not authenticated.");
            return null;
        }

        if (classroomId == null || classroomId.trim().isEmpty()) {
            callback.onError("Invalid classroom ID.");
            return null;
        }

        Query query = db.collection(
                Constants.COLLECTION_CLASS_SCHEDULES
        ).whereEqualTo("classroomId", classroomId);

        if (section != null &&
                !section.trim().isEmpty() &&
                !"All".equalsIgnoreCase(section)) {
            query = query.whereEqualTo("section", section);
        }

        return query.addSnapshotListener((snapshots, error) -> {

            if (error != null) {
                callback.onError(error.getMessage());
                return;
            }

            List<ClassSchedule> allSchedules = new ArrayList<>();
            List<ClassSchedule> activeSchedules = new ArrayList<>();

            if (snapshots != null) {

                long currentTime = System.currentTimeMillis();

                for (DocumentSnapshot doc : snapshots.getDocuments()) {

                    ClassSchedule schedule =
                            doc.toObject(ClassSchedule.class);

                    if (schedule == null) {
                        continue;
                    }

                    if (schedule.getScheduleId() == null) {
                        schedule.setScheduleId(doc.getId());
                    }

                    if (schedule.getStartTimestamp() <= 0 ||
                            schedule.getEndTimestamp() <= 0) {
                        prepareTimestamps(schedule);
                    }

                    allSchedules.add(schedule);

                    long endTimestamp = schedule.getEndTimestamp();

                    if (endTimestamp <= 0 ||
                            endTimestamp > currentTime) {
                        activeSchedules.add(schedule);
                    }
                }
            }

            // Earliest class first
            activeSchedules.sort((a, b) -> {

                long timeA = a.getStartTimestamp();
                long timeB = b.getStartTimestamp();

                if (timeA <= 0) {
                    timeA = Long.MAX_VALUE;
                }

                if (timeB <= 0) {
                    timeB = Long.MAX_VALUE;
                }

                return Long.compare(timeA, timeB);
            });

            deleteExpiredSchedules(classroomId, allSchedules);

            callback.onSuccess(activeSchedules);
        });
    }
}
