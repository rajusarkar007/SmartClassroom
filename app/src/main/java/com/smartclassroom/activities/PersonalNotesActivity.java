package com.smartclassroom.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.ListenerRegistration;
import com.smartclassroom.adapters.NoteAdapter;
import com.smartclassroom.databinding.ActivityPersonalNotesBinding;
import com.smartclassroom.models.PersonalNote;
import com.smartclassroom.repositories.NotesRepository;
import com.smartclassroom.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class PersonalNotesActivity extends AppCompatActivity {

    private ActivityPersonalNotesBinding binding;
    private NotesRepository notesRepo;
    private SessionManager sessionManager;
    private NoteAdapter adapter;

    private ListenerRegistration notesListener;
    private final List<PersonalNote> notesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPersonalNotesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        notesRepo = NotesRepository.getInstance();
        sessionManager = new SessionManager(this);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.fabAddNote.setOnClickListener(v -> showNoteEditorDialog(null));

        setupRecyclerView();
        listenPersonalNotes();
    }

    private void setupRecyclerView() {
        adapter = new NoteAdapter(new NoteAdapter.NoteActionListener() {
            @Override
            public void onEditNote(PersonalNote note) {
                showNoteEditorDialog(note);
            }

            @Override
            public void onDeleteNote(PersonalNote note) {
                new AlertDialog.Builder(PersonalNotesActivity.this)
                        .setTitle("Delete Note")
                        .setMessage("Are you sure you want to delete this personal note?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            notesRepo.deleteNote(note.getNoteId(), new NotesRepository.Callback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    Toast.makeText(PersonalNotesActivity.this, "Note deleted.", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(PersonalNotesActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        binding.rvNotes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotes.setAdapter(adapter);
    }

    private void listenPersonalNotes() {
        String uid = sessionManager.getUid();
        if (uid.isEmpty()) return;

        notesListener = notesRepo.listenStudentNotes(uid, new NotesRepository.Callback<List<PersonalNote>>() {
            @Override
            public void onSuccess(List<PersonalNote> result) {
                notesList.clear();
                if (result != null) notesList.addAll(result);
                adapter.updateList(notesList);

                boolean empty = notesList.isEmpty();
                binding.layoutEmptyNotes.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.rvNotes.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(PersonalNotesActivity.this, "Error loading notes: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showNoteEditorDialog(PersonalNote editNote) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(editNote == null ? "New Personal Note" : "Edit Personal Note");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Note Title");
        if (editNote != null) etTitle.setText(editNote.getTitle());
        layout.addView(etTitle);

        EditText etClassroomTag = new EditText(this);
        etClassroomTag.setHint("Course / Subject Tag (e.g. CSE 211)");
        if (editNote != null) etClassroomTag.setText(editNote.getClassroomName());
        layout.addView(etClassroomTag);

        EditText etContent = new EditText(this);
        etContent.setHint("Write private study notes or reminders here...");
        etContent.setLines(5);
        if (editNote != null) etContent.setText(editNote.getContent());
        layout.addView(etContent);

        builder.setView(layout);
        builder.setPositiveButton(editNote == null ? "Save Note" : "Update Note", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String tag = etClassroomTag.getText().toString().trim();
            String content = etContent.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            PersonalNote note = editNote != null ? editNote : new PersonalNote();
            note.setStudentUid(sessionManager.getUid());
            note.setTitle(title);
            note.setClassroomName(tag);
            note.setContent(content);

            notesRepo.saveNote(note, new NotesRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(PersonalNotesActivity.this, "Note saved privately!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(PersonalNotesActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notesListener != null) notesListener.remove();
    }
}
