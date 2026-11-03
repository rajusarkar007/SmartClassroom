package com.smartclassroom.repositories;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.smartclassroom.models.Classroom;
import com.smartclassroom.models.ClassroomMember;
import com.smartclassroom.models.JoinRequest;
import com.smartclassroom.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class ClassroomRepository {
    private static ClassroomRepository instance;
    private final FirebaseFirestore db;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private ClassroomRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized ClassroomRepository getInstance() {
        if (instance == null) {
            instance = new ClassroomRepository();
        }
        return instance;
    }

    public void createClassroom(Classroom classroom, Callback<Classroom> callback) {
        DocumentReference docRef = db.collection(Constants.COLLECTION_CLASSROOMS).document();
        classroom.setClassroomId(docRef.getId());
        classroom.setCreatedAt(System.currentTimeMillis());

        docRef.set(classroom)
                .addOnSuccessListener(unused -> callback.onSuccess(classroom))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getClassroomById(String classroomId, Callback<Classroom> callback) {
        db.collection(Constants.COLLECTION_CLASSROOMS)
                .document(classroomId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        Classroom c = doc.toObject(Classroom.class);
                        callback.onSuccess(c);
                    } else {
                        callback.onError("Classroom not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getClassroomByCode(String code, Callback<Classroom> callback) {
        db.collection(Constants.COLLECTION_CLASSROOMS)
                .whereEqualTo("classroomCode", code.trim().toUpperCase())
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        Classroom c = snapshots.getDocuments().get(0).toObject(Classroom.class);
                        callback.onSuccess(c);
                    } else {
                        callback.onError("No classroom found with code: " + code);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenTeacherClassrooms(String teacherId, Callback<List<Classroom>> callback) {
        return db.collection(Constants.COLLECTION_CLASSROOMS)
                .whereEqualTo("teacherId", teacherId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    List<Classroom> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (var doc : snapshots.getDocuments()) {
                            Classroom c = doc.toObject(Classroom.class);
                            if (c != null) list.add(c);
                        }
                    }
                    callback.onSuccess(list);
                });
    }

    public ListenerRegistration listenStudentMemberships(String studentUid, Callback<List<ClassroomMember>> callback) {
        return db.collection(Constants.COLLECTION_MEMBERS)
                .whereEqualTo("studentUid", studentUid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    List<ClassroomMember> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (var doc : snapshots.getDocuments()) {
                            ClassroomMember m = doc.toObject(ClassroomMember.class);
                            if (m != null) list.add(m);
                        }
                    }
                    callback.onSuccess(list);
                });
    }

    public void requestToJoinClassroom(JoinRequest request, Callback<Void> callback) {
        // First check if already member
        db.collection(Constants.COLLECTION_MEMBERS)
                .whereEqualTo("classroomId", request.getClassroomId())
                .whereEqualTo("studentUid", request.getStudentUid())
                .limit(1)
                .get()
                .addOnSuccessListener(memberSnaps -> {
                    if (memberSnaps != null && !memberSnaps.isEmpty()) {
                        callback.onError("You are already a member of this classroom.");
                        return;
                    }

                    // Check if already requested and pending
                    db.collection(Constants.COLLECTION_JOIN_REQUESTS)
                            .whereEqualTo("classroomId", request.getClassroomId())
                            .whereEqualTo("studentUid", request.getStudentUid())
                            .whereEqualTo("status", Constants.REQ_PENDING)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(reqSnaps -> {
                                if (reqSnaps != null && !reqSnaps.isEmpty()) {
                                    callback.onError("You have already submitted a join request. Please wait for approval.");
                                    return;
                                }

                                DocumentReference doc = db.collection(Constants.COLLECTION_JOIN_REQUESTS).document();
                                request.setRequestId(doc.getId());
                                request.setStatus(Constants.REQ_PENDING);
                                request.setRequestedAt(System.currentTimeMillis());

                                doc.set(request)
                                        .addOnSuccessListener(unused -> callback.onSuccess(null))
                                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenPendingJoinRequests(String teacherId, Callback<List<JoinRequest>> callback) {
        return db.collection(Constants.COLLECTION_JOIN_REQUESTS)
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("status", Constants.REQ_PENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    List<JoinRequest> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (var doc : snapshots.getDocuments()) {
                            JoinRequest r = doc.toObject(JoinRequest.class);
                            if (r != null) list.add(r);
                        }
                    }
                    callback.onSuccess(list);
                });
    }

    public void respondToJoinRequest(JoinRequest request, boolean approve, String rollNumber, String section, Callback<Void> callback) {
        WriteBatch batch = db.batch();
        DocumentReference reqRef = db.collection(Constants.COLLECTION_JOIN_REQUESTS).document(request.getRequestId());

        if (approve) {
            batch.update(reqRef, "status", Constants.REQ_APPROVED);
            DocumentReference memberRef = db.collection(Constants.COLLECTION_MEMBERS).document();
            String sec = (section != null && !section.trim().isEmpty()) ? section : request.getRequestedSection();
            ClassroomMember member = new ClassroomMember(
                    memberRef.getId(),
                    request.getClassroomId(),
                    request.getStudentUid(),
                    request.getStudentName(),
                    request.getStudentIdNumber(),
                    rollNumber,
                    sec,
                    false,
                    System.currentTimeMillis()
            );
            batch.set(memberRef, member);
        } else {
            batch.update(reqRef, "status", Constants.REQ_REJECTED);
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenClassroomMembers(String classroomId, Callback<List<ClassroomMember>> callback) {
        return db.collection(Constants.COLLECTION_MEMBERS)
                .whereEqualTo("classroomId", classroomId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    List<ClassroomMember> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (var doc : snapshots.getDocuments()) {
                            ClassroomMember m = doc.toObject(ClassroomMember.class);
                            if (m != null) list.add(m);
                        }
                    }
                    callback.onSuccess(list);
                });
    }

    public void addStudentManually(ClassroomMember member, Callback<Void> callback) {
        DocumentReference doc = db.collection(Constants.COLLECTION_MEMBERS).document();
        member.setMemberId(doc.getId());
        member.setJoinedAt(System.currentTimeMillis());

        doc.set(member)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateStudentMember(ClassroomMember member, Callback<Void> callback) {
        db.collection(Constants.COLLECTION_MEMBERS)
                .document(member.getMemberId())
                .set(member)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void removeStudentMember(String memberId, Callback<Void> callback) {
        db.collection(Constants.COLLECTION_MEMBERS)
                .document(memberId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void setRepresentativeStatus(String memberId, boolean isCR, Callback<Void> callback) {
        db.collection(Constants.COLLECTION_MEMBERS)
                .document(memberId)
                .update("classRepresentative", isCR)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
