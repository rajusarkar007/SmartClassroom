package com.smartclassroom.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.smartclassroom.adapters.AttendanceSessionAdapter;
import com.smartclassroom.databinding.ActivityAttendanceHistoryBinding;
import com.smartclassroom.models.AttendanceRecord;
import com.smartclassroom.models.AttendanceSession;
import com.smartclassroom.repositories.AttendanceRepository;
import com.smartclassroom.utils.Constants;
import com.smartclassroom.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AttendanceHistoryActivity extends AppCompatActivity {

    private ActivityAttendanceHistoryBinding binding;
    private AttendanceRepository attendanceRepo;
    private SessionManager sessionManager;
    private AttendanceSessionAdapter adapter;

    private String classroomId;
    private String currentSection = "All";
    private boolean isTeacher;
    private ListenerRegistration sessionsListener;
    private ListenerRegistration studentRecordsListener;

    private final List<AttendanceSession> sessionsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAttendanceHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        attendanceRepo = AttendanceRepository.getInstance();
        sessionManager = new SessionManager(this);

        classroomId = getIntent().getStringExtra(Constants.EXTRA_CLASSROOM_ID);
        isTeacher = getIntent().getBooleanExtra(Constants.EXTRA_IS_TEACHER, false);
        if (getIntent().getStringExtra(Constants.EXTRA_SECTION) != null) {
            currentSection = getIntent().getStringExtra(Constants.EXTRA_SECTION);
        }

        binding.btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        setupSpinner();

        if (isTeacher) {
            listenTeacherHistory();
        } else {
            listenStudentAttendanceHistory();
        }
    }

    private void setupRecyclerView() {
        adapter = new AttendanceSessionAdapter(session -> showSessionRecordsDialog(session));
        binding.rvHistorySessions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHistorySessions.setAdapter(adapter);
    }

    private void setupSpinner() {
        List<String> list = new ArrayList<>();
        list.add("All");
        list.add("Section A");
        list.add("Section B");
        list.add("Section C");

        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerHistorySection.setAdapter(spinAdapter);

        binding.spinnerHistorySection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSection = list.get(position);
                if (isTeacher) {
                    listenTeacherHistory();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void listenTeacherHistory() {
        if (sessionsListener != null) sessionsListener.remove();
        if (classroomId == null) return;

        sessionsListener = attendanceRepo.listenAttendanceSessions(classroomId, currentSection,
                new AttendanceRepository.Callback<List<AttendanceSession>>() {
                    @Override
                    public void onSuccess(List<AttendanceSession> result) {
                        sessionsList.clear();
                        if (result != null) sessionsList.addAll(result);
                        adapter.updateList(sessionsList);

                        boolean empty = sessionsList.isEmpty();
                        binding.layoutEmptyHistory.setVisibility(empty ? View.VISIBLE : View.GONE);
                        binding.rvHistorySessions.setVisibility(empty ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(AttendanceHistoryActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void listenStudentAttendanceHistory() {
        if (classroomId == null) return;
        String uid = sessionManager.getUid();

        attendanceRepo.calculateStudentStats(classroomId, uid, new AttendanceRepository.Callback<AttendanceRepository.AttendanceStats>() {
            @Override
            public void onSuccess(AttendanceRepository.AttendanceStats stats) {
                // Show overall student percentage in top alert or toast
                Toast.makeText(AttendanceHistoryActivity.this,
                        String.format(Locale.getDefault(), "Your Attendance: %.1f%% (%d Present / %d Conducted)",
                                stats.percentage, stats.totalPresent, stats.totalConducted), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {}
        });

        // Also display the past sessions
        listenTeacherHistory();
    }

    private void showSessionRecordsDialog(AttendanceSession session) {
        attendanceRepo.getAttendanceRecordsForSession(session.getSessionId(), new AttendanceRepository.Callback<List<AttendanceRecord>>() {
            @Override
            public void onSuccess(List<AttendanceRecord> records) {
                StringBuilder sb = new StringBuilder();
                sb.append("Date: ").append(session.getDate()).append("\n");
                sb.append("Topic: ").append(session.getTopic() != null ? session.getTopic() : "N/A").append("\n");
                sb.append("Section: ").append(session.getSection()).append("\n\n");
                sb.append("Present: ").append(session.getTotalPresent()).append(" | Absent: ").append(session.getTotalAbsent()).append(" | Late: ").append(session.getTotalLate()).append("\n");
                sb.append("--------------------------------------\n");

                for (AttendanceRecord r : records) {
                    sb.append(String.format("Roll: %-4s | %-16s | %s\n",
                            r.getStudentRoll() != null ? r.getStudentRoll() : "-",
                            r.getStudentName(),
                            r.getStatus()));
                }

                new AlertDialog.Builder(AttendanceHistoryActivity.this)
                        .setTitle("Class #" + session.getClassNumber() + " Details")
                        .setMessage(sb.toString())
                        .setPositiveButton("Close", null)
                        .show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AttendanceHistoryActivity.this, "Error loading session details: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sessionsListener != null) sessionsListener.remove();
        if (studentRecordsListener != null) studentRecordsListener.remove();
    }
}
