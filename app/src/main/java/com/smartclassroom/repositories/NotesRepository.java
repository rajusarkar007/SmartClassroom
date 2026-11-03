package com.smartclassroom.repositories;

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

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private NotesRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized NotesRepository getInstance() {
        if (instance == null) {
            instance = new NotesRepository();
        }
        return instance;
    }

    public void saveNote(PersonalNote note, Callback<Void> callback) {
        DocumentReference doc;
        long now = System.currentTimeMillis();
        if (note.getNoteId() != null && !note.getNoteId().isEmpty()) {
            doc = db.collection(Constants.COLLECTION_PERSONAL_NOTES).document(note.getNoteId());
            note.setUpdatedAt(now);
        } else {
            doc = db.collection(Constants.COLLECTION_PERSONAL_NOTES).document();
            note.setNoteId(doc.getId());
            note.setCreatedAt(now);
            note.setUpdatedAt(now);
        }

        doc.set(note)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteNote(String noteId, Callback<Void> callback) {
        db.collection(Constants.COLLECTION_PERSONAL_NOTES)
                .document(noteId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration listenStudentNotes(String studentUid, Callback<List<PersonalNote>> callback) {
        return db.collection(Constants.COLLECTION_PERSONAL_NOTES)
                .whereEqualTo("studentUid", studentUid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    List<PersonalNote> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (var doc : snapshots.getDocuments()) {
                            PersonalNote n = doc.toObject(PersonalNote.class);
                            if (n != null) list.add(n);
                        }
                    }
                    list.sort((a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
                    callback.onSuccess(list);
                });
    }
}
