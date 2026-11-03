package com.smartclassroom.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.ListenerRegistration;
import com.smartclassroom.adapters.NoticeAdapter;
import com.smartclassroom.databinding.ActivityNoticeBinding;
import com.smartclassroom.models.Notice;
import com.smartclassroom.repositories.NoticeRepository;
import com.smartclassroom.utils.Constants;
import com.smartclassroom.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class NoticeActivity extends AppCompatActivity {

    private ActivityNoticeBinding binding;
    private NoticeRepository noticeRepo;
    private SessionManager sessionManager;
    private NoticeAdapter adapter;

    private String classroomId;
    private boolean isTeacher;
    private boolean isCR;
    private boolean canPublish;

    private ListenerRegistration noticesListener;
    private final List<Notice> noticesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNoticeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        noticeRepo = NoticeRepository.getInstance();
        sessionManager = new SessionManager(this);

        classroomId = getIntent().getStringExtra(Constants.EXTRA_CLASSROOM_ID);
        isTeacher = getIntent().getBooleanExtra(Constants.EXTRA_IS_TEACHER, false);
        isCR = getIntent().getBooleanExtra(Constants.EXTRA_IS_CR, false);
        canPublish = isTeacher || isCR; // Teacher & Classroom Representative can publish notices

        binding.btnBack.setOnClickListener(v -> finish());
        binding.fabAddNotice.setVisibility(canPublish ? View.VISIBLE : View.GONE);
        binding.fabAddNotice.setOnClickListener(v -> showPublishNoticeDialog());

        setupRecyclerView();
        listenNotices();
    }

    private void setupRecyclerView() {
        adapter = new NoticeAdapter(sessionManager.getUid(), isTeacher, notice -> {
            new AlertDialog.Builder(NoticeActivity.this)
                    .setTitle("Delete Notice")
                    .setMessage("Are you sure you want to delete this notice?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        noticeRepo.deleteNotice(notice.getNoticeId(), new NoticeRepository.Callback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Toast.makeText(NoticeActivity.this, "Notice deleted.", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String message) {
                                Toast.makeText(NoticeActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        binding.rvNotices.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotices.setAdapter(adapter);
    }

    private void listenNotices() {
        if (classroomId == null) return;
        noticesListener = noticeRepo.listenNotices(classroomId, new NoticeRepository.Callback<List<Notice>>() {
            @Override
            public void onSuccess(List<Notice> result) {
                noticesList.clear();
                if (result != null) noticesList.addAll(result);
                adapter.updateList(noticesList);

                boolean empty = noticesList.isEmpty();
                binding.layoutEmptyNotice.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.rvNotices.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(NoticeActivity.this, "Error loading notices: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPublishNoticeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Publish Classroom Notice");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        Spinner spCategory = new Spinner(this);
        String[] categories = {
                Constants.NOTICE_REGULAR,
                Constants.NOTICE_CANCELLATION,
                Constants.NOTICE_EXTRA,
                Constants.NOTICE_EXAM,
                Constants.NOTICE_GENERAL
        };
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);
        layout.addView(spCategory);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Notice Title (e.g. Exam Syllabus Announcement)");
        layout.addView(etTitle);

        EditText etContent = new EditText(this);
        etContent.setHint("Notice Details / Message");
        etContent.setLines(4);
        layout.addView(etContent);

        builder.setView(layout);
        builder.setPositiveButton("Publish", (dialog, which) -> {
            String cat = categories[spCategory.getSelectedItemPosition()];
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
                Toast.makeText(this, "Title and message content are required", Toast.LENGTH_SHORT).show();
                return;
            }

            Notice n = new Notice();
            n.setClassroomId(classroomId);
            n.setCategory(cat);
            n.setTitle(title);
            n.setContent(content);
            n.setAuthorUid(sessionManager.getUid());
            n.setAuthorName(sessionManager.getFullName());
            n.setAuthorRole(isTeacher ? "TEACHER" : "REPRESENTATIVE");

            noticeRepo.createNotice(n, new NoticeRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(NoticeActivity.this, "Notice published successfully!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(NoticeActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (noticesListener != null) noticesListener.remove();
    }
}
