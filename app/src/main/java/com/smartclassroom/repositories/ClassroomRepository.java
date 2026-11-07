
package com.smartclassroom.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import com.smartclassroom.models.Classroom;
import com.smartclassroom.models.ClassroomMember;
import com.smartclassroom.models.JoinRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassroomRepository {

    private static ClassroomRepository instance;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private static final String CLASSROOMS = "classrooms";
    private static final String MEMBERS = "classroom_members";
    private static final String JOIN_REQUESTS = "join_requests";
    private static final String CLASSROOM_LOOKUP = "classroom_lookup";

    // Personal notes are intentionally excluded.
    private static final String[] DELETE_COLLECTIONS = {
            MEMBERS,
            JOIN_REQUESTS,
            "attendance_sessions",
            "attendance_records",
            "assessments",
            "student_marks",
            "class_schedules",
            "notices"
    };

    private ClassroomRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized ClassroomRepository getInstance() {
        if (instance == null) {
            instance = new ClassroomRepository();
        }
        return instance;
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private String getCurrentUid() {
        if (auth.getCurrentUser() == null) {
            return null;
        }

        return auth.getCurrentUser().getUid();
    }

    private void sendError(Callback<?> callback, String message) {
        if (callback != null) {
            callback.onError(
                    message == null ? "Something went wrong." : message
            );
        }
    }

    private String getMemberDocumentId(
            String classroomId,
            String studentUid
    ) {
        return classroomId + "_" + studentUid;
    }

    // --------------------------------------------------
    // CREATE CLASSROOM
    // --------------------------------------------------

    public void createClassroom(
            Classroom classroom,
            Callback<Classroom> callback
    ) {
        String uid = getCurrentUid();

        if (uid == null) {
            sendError(callback, "Please login first.");
            return;
        }

        if (classroom == null) {
            sendError(callback, "Classroom cannot be empty.");
            return;
        }

        DocumentReference classroomRef =
                db.collection(CLASSROOMS).document();

        String classroomId = classroomRef.getId();

        classroom.setClassroomId(classroomId);
        classroom.setTeacherId(uid);
        classroom.setCreatedAt(System.currentTimeMillis());

        if (classroom.getClassroomCode() != null) {
            classroom.setClassroomCode(
                    classroom.getClassroomCode().trim().toUpperCase()
            );
        }

        Map<String, Object> lookupData = new HashMap<>();

        lookupData.put("classroomId", classroomId);
        lookupData.put("classroomCode", classroom.getClassroomCode());
        lookupData.put("classroomName", classroom.getClassroomName());
        lookupData.put("courseName", classroom.getCourseName());
        lookupData.put("courseCode", classroom.getCourseCode());
        lookupData.put("department", classroom.getDepartment());
        lookupData.put("semester", classroom.getSemester());
        lookupData.put("section", classroom.getSection());
        lookupData.put("teacherName", classroom.getTeacherName());
        lookupData.put("teacherId", uid);

        WriteBatch batch = db.batch();

        batch.set(classroomRef, classroom);

        DocumentReference lookupRef =
                db.collection(CLASSROOM_LOOKUP).document(classroomId);

        batch.set(lookupRef, lookupData);

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(classroom);
                    }
                })
                .addOnFailureListener(e ->
                        sendError(callback, e.getMessage()));
    }

    // --------------------------------------------------
    // GET CLASSROOM BY ID
    // --------------------------------------------------

    public void getClassroomById(
            String classroomId,
            Callback<Classroom> callback
    ) {
        if (classroomId == null || classroomId.trim().isEmpty()) {
            sendError(callback, "Invalid classroom ID.");
            return;
        }

        db.collection(CLASSROOMS)
                .document(classroomId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        sendError(callback, "Classroom not found.");
                        return;
                    }

                    Classroom classroom =
                            document.toObject(Classroom.class);

                    if (classroom == null) {
                        sendError(callback, "Unable to load classroom.");
                        return;
                    }

                    classroom.setClassroomId(document.getId());

                    if (callback != null) {
                        callback.onSuccess(classroom);
                    }
                })
                .addOnFailureListener(e ->
                        sendError(callback, e.getMessage()));
    }

    // --------------------------------------------------
    // GET CLASSROOM BY CODE
    // --------------------------------------------------

    public void getClassroomByCode(
            String code,
            Callback<Classroom> callback
    ) {
        if (code == null || code.trim().isEmpty()) {
            sendError(callback, "Enter a valid classroom code.");
            return;
        }

        String normalizedCode = code.trim().toUpperCase();

        db.collection(CLASSROOM_LOOKUP)
                .whereEqualTo("classroomCode", normalizedCode)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        sendError(callback, "No classroom found.");
                        return;
                    }

                    DocumentSnapshot document =
                            snapshot.getDocuments().get(0);

                    Classroom classroom = new Classroom();

                    classroom.setClassroomId(
                            document.getString("classroomId")
                    );

                    classroom.setClassroomCode(
                            document.getString("classroomCode")
                    );

                    classroom.setClassroomName(
                            document.getString("classroomName")
                    );

                    classroom.setCourseName(
                            document.getString("courseName")
                    );

                    classroom.setCourseCode(
                            document.getString("courseCode")
                    );

                    classroom.setDepartment(
                            document.getString("department")
                    );

                    classroom.setSemester(
                            document.getString("semester")
                    );

                    classroom.setSection(
                            document.getString("section")
                    );

                    classroom.setTeacherName(
                            document.getString("teacherName")
                    );

                    classroom.setTeacherId(
                            document.getString("teacherId")
                    );

                    if (callback != null) {
                        callback.onSuccess(classroom);
                    }
                })
                .addOnFailureListener(e ->
                        sendError(callback, e.getMessage()));
    }

    // --------------------------------------------------
    // LIST TEACHER CLASSROOMS
    // --------------------------------------------------

    public ListenerRegistration listenTeacherClassrooms(
            String teacherId,
            Callback<List<Classroom>> callback
    ) {
        return db.collection(CLASSROOMS)
                .whereEqualTo("teacherId", teacherId)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        sendError(callback, error.getMessage());
                        return;
                    }

                    List<Classroom> list = new ArrayList<>();

                    if (snapshot != null) {
                        for (DocumentSnapshot document :
                                snapshot.getDocuments()) {

                            Classroom classroom =
                                    document.toObject(Classroom.class);

                            if (classroom != null) {
                                classroom.setClassroomId(document.getId());
                                list.add(classroom);
                            }
                        }
                    }

                    if (callback != null) {
                        callback.onSuccess(list);
                    }
                });
    }

    // --------------------------------------------------
    // LIST STUDENT MEMBERSHIPS
    // --------------------------------------------------

    public ListenerRegistration listenStudentMemberships(
            String studentUid,
            Callback<List<ClassroomMember>> callback
    ) {
        return db.collection(MEMBERS)
                .whereEqualTo("studentUid", studentUid)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        sendError(callback, error.getMessage());
                        return;
                    }

                    List<ClassroomMember> list = new ArrayList<>();

                    if (snapshot != null) {
                        for (DocumentSnapshot document :
                                snapshot.getDocuments()) {

                            ClassroomMember member =
                                    document.toObject(ClassroomMember.class);

                            if (member != null) {
                                member.setMemberId(document.getId());
                                list.add(member);
                            }
                        }
                    }

                    if (callback != null) {
                        callback.onSuccess(list);
                    }
                });
    }

    // --------------------------------------------------
    // REQUEST TO JOIN CLASSROOM
    // --------------------------------------------------

    public void requestToJoinClassroom(
            JoinRequest request,
            Callback<Void> callback
    ) {
        String uid = getCurrentUid();

        if (uid == null) {
            sendError(callback, "Please login first.");
            return;
        }

        if (request == null ||
                request.getClassroomId() == null ||
                request.getClassroomId().trim().isEmpty()) {

            sendError(callback, "Invalid join request.");
            return;
        }

        String classroomId = request.getClassroomId();
        request.setStudentUid(uid);

        String memberId = getMemberDocumentId(classroomId, uid);

        DocumentReference memberRef =
                db.collection(MEMBERS).document(memberId);

        memberRef.get()
                .addOnSuccessListener(memberDocument -> {

                    if (memberDocument.exists()) {
                        sendError(callback, "You are already a member.");
                        return;
                    }

                    db.collection(JOIN_REQUESTS)
                            .whereEqualTo("classroomId", classroomId)
                            .whereEqualTo("studentUid", uid)
                            .whereEqualTo("status", "PENDING")
                            .limit(1)
                            .get()
                            .addOnSuccessListener(snapshot -> {

                                if (!snapshot.isEmpty()) {
                                    sendError(
                                            callback,
                                            "You already have a pending request."
                                    );
                                    return;
                                }

                                DocumentReference requestRef =
                                        db.collection(JOIN_REQUESTS).document();

                                request.setRequestId(requestRef.getId());
                                request.setStatus("PENDING");
                                request.setRequestedAt(
                                        System.currentTimeMillis()
                                );

                                requestRef.set(request)
                                        .addOnSuccessListener(unused -> {
                                            if (callback != null) {
                                                callback.onSuccess(null);
                                            }
                                        })
                                        .addOnFailureListener(e ->
                                                sendError(callback, e.getMessage()));
                            })
                            .addOnFailureListener(e ->
                                    sendError(callback, e.getMessage()));
                })
                .addOnFailureListener(e ->
                        sendError(callback, e.getMessage()));
    }

    // --------------------------------------------------
    // LIST PENDING JOIN REQUESTS
    // --------------------------------------------------

    public ListenerRegistration listenPendingJoinRequests(
            String teacherId,
            Callback<List<JoinRequest>> callback
    ) {
        return db.collection(JOIN_REQUESTS)
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        sendError(callback, error.getMessage());
                        return;
                    }

                    List<JoinRequest> list = new ArrayList<>();

                    if (snapshot != null) {
                        for (DocumentSnapshot document :
                                snapshot.getDocuments()) {

                            JoinRequest request =
                                    document.toObject(JoinRequest.class);

                            if (request != null) {
                                request.setRequestId(document.getId());
                                list.add(request);
                            }
                        }
                    }

                    if (callback != null) {
                        callback.onSuccess(list);
                    }
                });
    }

    // --------------------------------------------------
    // RESPOND TO JOIN REQUEST
    // --------------------------------------------------

    public void respondToJoinRequest(
            JoinRequest request,
            boolean approved,
            String rollNumber,
            String section,
            Callback<Void> callback
    ) {
        String uid = getCurrentUid();

        if (uid == null) {
            sendError(callback, "Please login first.");
            return;
        }

        if (request == null ||
                request.getRequestId() == null ||
                request.getClassroomId() == null ||
                request.getStudentUid() == null) {

            sendError(callback, "Invalid join request.");
            return;
        }

        String classroomId = request.getClassroomId();
        String studentUid = request.getStudentUid();

        DocumentReference requestRef =
                db.collection(JOIN_REQUESTS)
                        .document(request.getRequestId());

        DocumentReference classroomRef =
                db.collection(CLASSROOMS).document(classroomId);

        DocumentReference memberRef =
                db.collection(MEMBERS)
                        .document(getMemberDocumentId(classroomId, studentUid));

        requestRef.get()
                .addOnSuccessListener(requestDocument -> {

                    if (!requestDocument.exists()) {
                        sendError(callback, "Join request not found.");
                        return;
                    }

                    if (!"PENDING".equals(
                            requestDocument.getString("status"))) {

                        sendError(
                                callback,
                                "This request has already been processed."
                        );
                        return;
                    }

                    classroomRef.get()
                            .addOnSuccessListener(classroomDocument -> {

                                if (!classroomDocument.exists()) {
                                    sendError(callback, "Classroom not found.");
                                    return;
                                }

                                String ownerId =
                                        classroomDocument.getString("teacherId");

                                if (!uid.equals(ownerId)) {
                                    sendError(
                                            callback,
                                            "Only the classroom teacher can respond."
                                    );
                                    return;
                                }

                                memberRef.get()
                                        .addOnSuccessListener(memberDocument -> {

                                            if (approved && memberDocument.exists()) {
                                                sendError(
                                                        callback,
                                                        "Student is already a member."
                                                );
                                                return;
                                            }

                                            WriteBatch batch = db.batch();

                                            batch.update(
                                                    requestRef,
                                                    "status",
                                                    approved ? "APPROVED" : "REJECTED"
                                            );

                                            if (approved) {

                                                ClassroomMember member =
                                                        new ClassroomMember();

                                                member.setMemberId(memberRef.getId());
                                                member.setClassroomId(classroomId);
                                                member.setStudentUid(studentUid);

                                                member.setStudentName(
                                                        requestDocument.getString("studentName")
                                                );

                                                member.setStudentIdNumber(
                                                        requestDocument.getString("studentIdNumber")
                                                );

                                                member.setRollNumber(rollNumber);
                                                member.setSection(section);
                                                member.setClassRepresentative(false);

                                                member.setJoinedAt(
                                                        System.currentTimeMillis()
                                                );

                                                batch.set(memberRef, member);
                                            }

                                            batch.commit()
                                                    .addOnSuccessListener(unused -> {
                                                        if (callback != null) {
                                                            callback.onSuccess(null);
                                                        }
                                                    })
                                                    .addOnFailureListener(e ->
                                                            sendError(callback, e.getMessage()));
                                        })
                                        .addOnFailureListener(e ->
                                                sendError(callback, e.getMessage()));
                            })
                            .addOnFailureListener(e ->
                                    sendError(callback, e.getMessage()));
                })
                .addOnFailureListener(e ->
                        sendError(callback, e.getMessage()));
    }

    // --------------------------------------------------
    // LIST CLASSROOM MEMBERS
    // --------------------------------------------------

    public ListenerRegistration listenClassroomMembers(
            String classroomId,
            Callback<List<ClassroomMember>> callback
    ) {
        return db.collection(MEMBERS)
                .whereEqualTo("classroomId", classroomId)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        sendError(callback, error.getMessage());
                        return;
                    }

                    List<ClassroomMember> list = new ArrayList<>();

                    if (snapshot != null) {
                        for (DocumentSnapshot document :
                                snapshot.getDocuments()) {

                            ClassroomMember member =
                                    document.toObject(ClassroomMember.class);

                            if (member != null) {
                                member.setMemberId(document.getId());
                                list.add(member);
                            }
                        }
                    }

                    if (callback != null) {
                        callback.onSuccess(list);
                    }
                });
    }

    // --------------------------------------------------
    // ADD STUDENT MANUALLY
    // --------------------------------------------------

    public void addStudentManually(
            ClassroomMember member,
            Callback<Void> callback
    ) {
        String uid = getCurrentUid();

        if (uid == null) {
            sendError(callback, "Please login first.");
            return;
        }

        if (member == null ||
                member.getClassroomId() == null ||
                member.getStudentUid() == null) {

            sendError(callback, "Invalid student information.");
            return;
        }

        String memberId = getMemberDocumentId(
                member.getClassroomId(),
                member.getStudentUid()
        );

        DocumentReference memberRef =
                db.collection(MEMBERS).document(memberId);

        memberRef.get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {
                        sendError(
                                callback,
                                "Student is already in this classroom."
                        );
                        return;
                    }

                    member.setMemberId(memberId);

                    if (member.getJoinedAt() == 0) {
                        member.setJoinedAt(System.currentTimeMillis());
                    }

                    memberRef.set(member)
                            .addOnSuccessListener(unused -> {
                                if (callback != null) {
                                    callback.onSuccess(null);
                                }
                            })
                            .addOnFailureListener(e ->
                                    sendError(callback, e.getMessage()));
                })
                .addOnFailureListener(e ->
                        sendError(callback, e.getMessage()));
    }

    // --------------------------------------------------
    // UPDATE STUDENT MEMBER
    // --------------------------------------------------

    public void updateStudentMember(
            ClassroomMember member,
            Callback<Void> callback
    ) {
        if (getCurrentUid() == null) {
            sendError(callback, "Please login first.");
            return;
        }

        if (member == null ||
                member.getClassroomId() == null ||
                member.getStudentUid() == null) {

            sendError(callback, "Invalid member information.");
            return;
        }

        String memberId = getMemberDocumentId(
                member.getClassroomId(),
                member.getStudentUid()
        );

        member.setMemberId(memberId);

        db.collection(MEMBERS)
                .document(memberId)
                .set(member)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e ->
                        sendError(callback, e.getMessage()));
    }

    // --------------------------------------------------
    // REMOVE STUDENT MEMBER
    // --------------------------------------------------

    public void removeStudentMember(
            String memberId,
            Callback<Void> callback
    ) {
        if (getCurrentUid() == null) {
            sendError(callback, "Please login first.");
            return;
        }

        if (memberId == null || memberId.trim().isEmpty()) {
            sendError(callback, "Invalid member ID.");
            return;
        }

        db.collection(MEMBERS)
                .document(memberId)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e ->
                        sendError(callback, e.getMessage()));
    }

    // --------------------------------------------------
    // SET REPRESENTATIVE STATUS
    // --------------------------------------------------

    public void setRepresentativeStatus(
            String memberId,
            boolean isRepresentative,
            Callback<Void> callback
    ) {
        if (getCurrentUid() == null) {
            sendError(callback, "Please login first.");
            return;
        }

        if (memberId == null || memberId.trim().isEmpty()) {
            sendError(callback, "Invalid member ID.");
            return;
        }

        db.collection(MEMBERS)
                .document(memberId)
                .update("classRepresentative", isRepresentative)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e ->
                        sendError(callback, e.getMessage()));
    }

    // --------------------------------------------------
    // DELETE CLASSROOM AND ASSOCIATED DATA
    // --------------------------------------------------

    public void deleteClassroom(
            String classroomId,
            Callback<Void> callback
    ) {
        String uid = getCurrentUid();

        if (uid == null) {
            sendError(callback, "Please login first.");
            return;
        }

        if (classroomId == null || classroomId.trim().isEmpty()) {
            sendError(callback, "Invalid classroom ID.");
            return;
        }

        String id = classroomId.trim();

        DocumentReference classroomRef =
                db.collection(CLASSROOMS).document(id);

        classroomRef.get()
                .addOnSuccessListener(classroomDocument -> {

                    if (!classroomDocument.exists()) {
                        sendError(callback, "Classroom not found.");
                        return;
                    }

                    String ownerId =
                            classroomDocument.getString("teacherId");

                    if (!uid.equals(ownerId)) {
                        sendError(
                                callback,
                                "You can only delete your own classroom."
                        );
                        return;
                    }

                    deleteNextCollection(0, id, callback);
                })
                .addOnFailureListener(error ->
                        sendError(callback, error.getMessage()));
    }

    private void deleteNextCollection(
            int index,
            String classroomId,
            Callback<Void> callback
    ) {
        if (index >= DELETE_COLLECTIONS.length) {
            deleteClassroomLookup(classroomId, callback);
            return;
        }

        String collectionName = DELETE_COLLECTIONS[index];

        deleteCollectionDocuments(
                collectionName,
                classroomId,
                () -> deleteNextCollection(
                        index + 1,
                        classroomId,
                        callback
                ),
                callback
        );
    }

    private void deleteCollectionDocuments(
            String collectionName,
            String classroomId,
            Runnable onComplete,
            Callback<Void> callback
    ) {
        db.collection(collectionName)
                .whereEqualTo("classroomId", classroomId)
                .limit(400)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        onComplete.run();
                        return;
                    }

                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot document :
                            snapshot.getDocuments()) {

                        batch.delete(document.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(unused ->
                                    deleteCollectionDocuments(
                                            collectionName,
                                            classroomId,
                                            onComplete,
                                            callback
                                    )
                            )
                            .addOnFailureListener(error ->
                                    sendError(
                                            callback,
                                            "Delete failed in "
                                                    + collectionName
                                                    + ": "
                                                    + error.getMessage()
                                    ));
                })
                .addOnFailureListener(error ->
                        sendError(
                                callback,
                                "Unable to read "
                                        + collectionName
                                        + ": "
                                        + error.getMessage()
                        ));
    }

    private void deleteClassroomLookup(
            String classroomId,
            Callback<Void> callback
    ) {
        DocumentReference lookupRef =
                db.collection(CLASSROOM_LOOKUP)
                        .document(classroomId);

        lookupRef.get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        deleteMainClassroom(classroomId, callback);
                        return;
                    }

                    lookupRef.delete()
                            .addOnSuccessListener(unused ->
                                    deleteMainClassroom(
                                            classroomId,
                                            callback
                                    )
                            )
                            .addOnFailureListener(error ->
                                    sendError(
                                            callback,
                                            "Lookup deletion failed: "
                                                    + error.getMessage()
                                    ));
                })
                .addOnFailureListener(error ->
                        sendError(callback, error.getMessage()));
    }

    private void deleteMainClassroom(
            String classroomId,
            Callback<Void> callback
    ) {
        db.collection(CLASSROOMS)
                .document(classroomId)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(error ->
                        sendError(
                                callback,
                                "Classroom deletion failed: "
                                        + error.getMessage()
                        ));
    }
}
