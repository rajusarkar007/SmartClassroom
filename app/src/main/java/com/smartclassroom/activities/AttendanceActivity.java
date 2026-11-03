package com.smartclassroom.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.smartclassroom.adapters.AttendanceAdapter;
import com.smartclassroom.databinding.ActivityAttendanceBinding;
import com.smartclassroom.models.AttendanceRecord;
import com.smartclassroom.models.AttendanceSession;
import com.smartclassroom.models.ClassroomMember;
import com.smartclassroom.repositories.AttendanceRepository;
import com.smartclassroom.repositories.ClassroomRepository;
import com.smartclassroom.utils.Constants;
import com.smartclassroom.utils.DateUtils;
import com.smartclassroom.utils.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AttendanceActivity extends AppCompatActivity {

    private ActivityAttendanceBinding binding;
    private ClassroomRepository classroomRepo;
    private AttendanceRepository attendanceRepo;
    private SessionManager sessionManager;
    private AttendanceAdapter adapter;

    private String classroomId;
    private String classroomName;
    private String currentSection = "Section A";

    private final List<ClassroomMember> allMembers = new ArrayList<>();
    private final List<ClassroomMember> sectionMembers = new ArrayList<>();
    private final List<String> sectionsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAttendanceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        classroomRepo = ClassroomRepository.getInstance();
        attendanceRepo = AttendanceRepository.getInstance();
        sessionManager = new SessionManager(this);

        classroomId = getIntent().getStringExtra(Constants.EXTRA_CLASSROOM_ID);
        classroomName = getIntent().getStringExtra(Constants.EXTRA_CLASSROOM_NAME);
        if (getIntent().getStringExtra(Constants.EXTRA_SECTION) != null) {
            currentSection = getIntent().getStringExtra(Constants.EXTRA_SECTION);
        }

        binding.tvAttendanceTitle.setText(classroomName != null ? classroomName + " - Attendance" : "Attendance Management");
        binding.btnBack.setOnClickListener(v -> finish());

        binding.etDate.setText(DateUtils.getCurrentDate());
        binding.etClassNumber.setText("1");

        setupDatePicker();
        setupRecyclerView();
        setupButtons();
        loadStudentsAndSessions();
    }

    private void setupDatePicker() {
        binding.etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                c.set(year, month, dayOfMonth);
                binding.etDate.setText(DateUtils.formatDate(c.getTimeInMillis()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupRecyclerView() {
        adapter = new AttendanceAdapter();
        binding.rvAttendanceStudents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAttendanceStudents.setAdapter(adapter);
    }

    private void setupButtons() {
        binding.btnMarkAllPresent.setOnClickListener(v -> adapter.markAll(Constants.STATUS_PRESENT));
        binding.btnMarkAllAbsent.setOnClickListener(v -> adapter.markAll(Constants.STATUS_ABSENT));

        binding.btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(AttendanceActivity.this, AttendanceHistoryActivity.class);
            intent.putExtra(Constants.EXTRA_CLASSROOM_ID, classroomId);
            intent.putExtra(Constants.EXTRA_CLASSROOM_NAME, classroomName);
            intent.putExtra(Constants.EXTRA_SECTION, currentSection);
            intent.putExtra(Constants.EXTRA_IS_TEACHER, true);
            startActivity(intent);
        });

        binding.btnSubmitAttendance.setOnClickListener(v -> showSubmitConfirmDialog());
    }

    private void loadStudentsAndSessions() {
        if (classroomId == null) return;

        classroomRepo.listenClassroomMembers(classroomId, new ClassroomRepository.Callback<List<ClassroomMember>>() {
            @Override
            public void onSuccess(List<ClassroomMember> result) {
                allMembers.clear();
                if (result != null) allMembers.addAll(result);

                Set<String> set = new HashSet<>();
                set.add(currentSection);
                for (ClassroomMember m : allMembers) {
                    if (m.getSection() != null && !m.getSection().trim().isEmpty()) {
                        set.add(m.getSection());
                    }
                }
                sectionsList.clear();
                sectionsList.addAll(set);

                ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(AttendanceActivity.this,
                        android.R.layout.simple_spinner_item, sectionsList);
                spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.spinnerAttendanceSection.setAdapter(spinAdapter);

                binding.spinnerAttendanceSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        currentSection = sectionsList.get(position);
                        filterStudentsBySection();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AttendanceActivity.this, "Error loading students: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterStudentsBySection() {
        sectionMembers.clear();
        for (ClassroomMember m : allMembers) {
            if (currentSection.equalsIgnoreCase(m.getSection()) || m.getSection() == null) {
                sectionMembers.add(m);
            }
        }
        adapter.updateList(sectionMembers);
    }

    private void showSubmitConfirmDialog() {
        String classNumStr = binding.etClassNumber.getText() != null ? binding.etClassNumber.getText().toString().trim() : "";
        if (TextUtils.isEmpty(classNumStr)) {
            binding.tilClassNumber.setError("Class # is required");
            return;
        }
        binding.tilClassNumber.setError(null);

        int classNum = Integer.parseInt(classNumStr);
        String date = binding.etDate.getText().toString().trim();
        String topic = binding.etTopic.getText() != null ? binding.etTopic.getText().toString().trim() : "";

        if (sectionMembers.isEmpty()) {
            Toast.makeText(this, "No students enrolled in " + currentSection, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Submit Attendance Session")
                .setMessage("Submit attendance for Class #" + classNum + " on " + date + " for " + currentSection + " (" + sectionMembers.size() + " students)?")
                .setPositiveButton("Submit", (dialog, which) -> {
                    submitAttendance(classNum, date, topic);
                })
                .setNegativeButton("Review", null)
                .show();
    }

    private void submitAttendance(int classNum, String date, String topic) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSubmitAttendance.setEnabled(false);

        AttendanceSession session = new AttendanceSession();
        session.setClassroomId(classroomId);
        session.setSection(currentSection);
        session.setClassNumber(classNum);
        session.setDate(date);
        session.setTopic(topic);
        session.setTeacherId(sessionManager.getUid());

        List<AttendanceRecord> records = adapter.getRecords(classroomId, currentSection);

        attendanceRepo.submitAttendance(session, records, new AttendanceRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSubmitAttendance.setEnabled(true);
                Toast.makeText(AttendanceActivity.this, "Attendance submitted successfully!", Toast.LENGTH_LONG).show();

                // Increment class number for next class convenience
                binding.etClassNumber.setText(String.valueOf(classNum + 1));
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSubmitAttendance.setEnabled(true);
                Toast.makeText(AttendanceActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
