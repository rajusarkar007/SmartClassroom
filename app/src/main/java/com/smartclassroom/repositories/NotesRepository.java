package com.smartclassroom.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.smartclassroom.models.PersonalNote;
import com.smartclassroom.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class NotesRepository {

    private static NotesRepository instance;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private NotesRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized NotesRepository getInstance() {
        if (instance == null) {
            instance = new NotesRepository();
        }
        return instance;
    }

    private FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // Save or Update Personal Note
    public void saveNote(
            PersonalNote note,
            Callback<Void> callback) {

        FirebaseUser user = getCurrentUser();

        if (user == null) {
            callback.onError("User is not authenticated.");
            return;
        }

        if (note == null) {
            callback.onError("Note cannot be empty.");
            return;
        }

        if (note.getTitle() == null
                || note.getTitle().trim().isEmpty()) {
            callback.onError("Note title is required.");
            return;
        }

        if (note.getContent() == null
                || note.getContent().trim().isEmpty()) {
            callback.onError("Note content is required.");
            return;
        }

        long now = System.currentTimeMillis();

        DocumentReference doc;

        if (note.getNoteId() != null
                && !note.getNoteId().trim().isEmpty()) {

            // Existing note
            doc = db.collection(Constants.COLLECTION_PERSONAL_NOTES)
                    .document(note.getNoteId());

            note.setUpdatedAt(now);

        } else {

            // New note
            doc = db.collection(Constants.COLLECTION_PERSONAL_NOTES)
                    .document();

            note.setNoteId(doc.getId());
            note.setCreatedAt(now);
            note.setUpdatedAt(now);
        }

        // Always use authenticated user's UID
        note.setStudentUid(user.getUid());

        doc.set(note)
                .addOnSuccessListener(unused ->
                        callback.onSuccess(null))
                .addOnFailureListener(e ->
                        callback.onError(e.getMessage()));
    }

    // Delete Personal Note
    public void deleteNote(
            String noteId,
            Callback<Void> callback) {

        FirebaseUser user = getCurrentUser();

        if (user == null) {
            callback.onError("User is not authenticated.");
            return;
        }

        if (noteId == null || noteId.trim().isEmpty()) {
            callback.onError("Invalid note ID.");
            return;
        }

        db.collection(Constants.COLLECTION_PERSONAL_NOTES)
                .document(noteId)
                .delete()
                .addOnSuccessListener(unused ->
                        callback.onSuccess(null))
                .addOnFailureListener(e ->
                        callback.onError(e.getMessage()));
    }

    // Listen to Current Student's Notes
    public ListenerRegistration listenStudentNotes(
            String studentUid,
            Callback<List<PersonalNote>> callback) {

        FirebaseUser user = getCurrentUser();

        if (user == null) {
            callback.onError("User is not authenticated.");
            return null;
        }

        // Students can only listen to their own notes
        if (studentUid == null
                || !user.getUid().equals(studentUid)) {

            callback.onError("You can only access your own notes.");
            return null;
        }

        return db.collection(Constants.COLLECTION_PERSONAL_NOTES)
                .whereEqualTo("studentUid", user.getUid())
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }

                    List<PersonalNote> list = new ArrayList<>();

                    if (snapshots != null) {
                        for (var doc : snapshots.getDocuments()) {

                            PersonalNote note =
                                    doc.toObject(PersonalNote.class);

                            if (note != null) {
                                list.add(note);
                            }
                        }
                    }

                    // Sort by updatedAt descending
                    list.sort((a, b) ->
                            Long.compare(
                                    b.getUpdatedAt(),
                                    a.getUpdatedAt()
                            )
                    );

                    callback.onSuccess(list);
                });
    }
}