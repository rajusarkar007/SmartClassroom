package com.smartclassroom.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.ListenerRegistration;

import com.smartclassroom.adapters.ScheduleAdapter;
import com.smartclassroom.databinding.ActivityScheduleBinding;
import com.smartclassroom.models.ClassSchedule;
import com.smartclassroom.repositories.ScheduleRepository;
import com.smartclassroom.utils.Constants;
import com.smartclassroom.utils.DateUtils;
import com.smartclassroom.utils.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ScheduleActivity extends AppCompatActivity {

    private ActivityScheduleBinding binding;
    private ScheduleRepository scheduleRepo;
    private SessionManager sessionManager;
    private ScheduleAdapter adapter;

    private String classroomId;
    private String currentSection = "All";

    private boolean isTeacher;
    private boolean canManage = false;

    private ListenerRegistration schedulesListener;

    private final List<ClassSchedule> schedulesList =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityScheduleBinding.inflate(
                getLayoutInflater()
        );

        setContentView(binding.getRoot());

        scheduleRepo = ScheduleRepository.getInstance();
        sessionManager = new SessionManager(this);

        classroomId = getIntent().getStringExtra(
                Constants.EXTRA_CLASSROOM_ID
        );

        isTeacher = getIntent().getBooleanExtra(
                Constants.EXTRA_IS_TEACHER,
                false
        );

        String section = getIntent().getStringExtra(
                Constants.EXTRA_SECTION
        );

        if (section != null && !section.trim().isEmpty()) {
            currentSection = section;
        }

        binding.btnBack.setOnClickListener(v -> finish());

        binding.fabAddSchedule.setVisibility(View.GONE);

        if (classroomId == null || classroomId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Invalid classroom.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        // Verify actual Teacher / CR permission from Firestore
        scheduleRepo.checkManagerPermission(
                classroomId,
                new ScheduleRepository.Callback<Boolean>() {

                    @Override
                    public void onSuccess(Boolean result) {

                        canManage = Boolean.TRUE.equals(result);

                        setupRecyclerView();

                        binding.fabAddSchedule.setVisibility(
                                canManage ? View.VISIBLE : View.GONE
                        );

                        if (canManage) {
                            binding.fabAddSchedule.setOnClickListener(
                                    v -> showScheduleDialog(null)
                            );
                        }

                        listenSchedules();
                    }

                    @Override
                    public void onError(String message) {

                        canManage = false;

                        setupRecyclerView();

                        binding.fabAddSchedule.setVisibility(View.GONE);

                        listenSchedules();
                    }
                }
        );
    }

    // Setup RecyclerView
    private void setupRecyclerView() {

        adapter = new ScheduleAdapter(
                canManage,
                new ScheduleAdapter.ScheduleActionListener() {

                    @Override
                    public void onToggleCancel(ClassSchedule schedule) {

                        boolean newCancel = !schedule.isCancelled();

                        scheduleRepo.toggleClassCancelled(
                                schedule.getScheduleId(),
                                newCancel,
                                new ScheduleRepository.Callback<Void>() {

                                    @Override
                                    public void onSuccess(Void result) {

                                        Toast.makeText(
                                                ScheduleActivity.this,
                                                newCancel
                                                        ? "Class marked as CANCELLED."
                                                        : "Class schedule resumed.",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }

                                    @Override
                                    public void onError(String message) {

                                        showError(message);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onEditSchedule(ClassSchedule schedule) {
                        showScheduleDialog(schedule);
                    }

                    @Override
                    public void onDeleteSchedule(ClassSchedule schedule) {
                        confirmDeleteSchedule(schedule);
                    }
                }
        );

        binding.rvSchedules.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.rvSchedules.setAdapter(adapter);
    }

    // Listen to schedules
    private void listenSchedules() {

        schedulesListener = scheduleRepo.listenSchedules(
                classroomId,
                currentSection,
                new ScheduleRepository.Callback<List<ClassSchedule>>() {

                    @Override
                    public void onSuccess(List<ClassSchedule> result) {

                        schedulesList.clear();

                        if (result != null) {
                            schedulesList.addAll(result);
                        }

                        if (adapter != null) {
                            adapter.updateList(schedulesList);
                        }

                        boolean empty = schedulesList.isEmpty();

                        binding.layoutEmptySchedule.setVisibility(
                                empty ? View.VISIBLE : View.GONE
                        );

                        binding.rvSchedules.setVisibility(
                                empty ? View.GONE : View.VISIBLE
                        );
                    }

                    @Override
                    public void onError(String message) {

                        Toast.makeText(
                                ScheduleActivity.this,
                                "Error loading schedules: " + message,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    // Add or Edit Schedule Dialog
    private void showScheduleDialog(ClassSchedule existingSchedule) {

        boolean isEditing = existingSchedule != null;

        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);

        builder.setTitle(
                isEditing ? "Edit Schedule" : "Schedule Class"
        );

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        int pad = (int) (
                18 * getResources().getDisplayMetrics().density
        );

        layout.setPadding(pad, pad, pad, pad);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Class Title / Topic");

        if (isEditing) {
            etTitle.setText(existingSchedule.getTitle());
        }

        layout.addView(etTitle);

        EditText etRoom = new EditText(this);
        etRoom.setHint("Classroom / Room Number");

        if (isEditing) {
            etRoom.setText(existingSchedule.getRoomNumber());
        }

        layout.addView(etRoom);

        EditText etDate = new EditText(this);
        etDate.setHint("Date");

        etDate.setFocusable(false);
        etDate.setClickable(true);

        etDate.setText(
                isEditing
                        ? existingSchedule.getDate()
                        : DateUtils.getCurrentDate()
        );

        etDate.setOnClickListener(v -> {

            Calendar c = Calendar.getInstance();

            new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {

                        c.set(year, month, dayOfMonth);

                        etDate.setText(
                                DateUtils.formatDate(
                                        c.getTimeInMillis()
                                )
                        );
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        layout.addView(etDate);

        EditText etStartTime = new EditText(this);
        etStartTime.setHint("Starting Time (e.g. 10:00 AM)");

        etStartTime.setText(
                isEditing
                        ? existingSchedule.getStartTime()
                        : "10:00 AM"
        );

        layout.addView(etStartTime);

        EditText etEndTime = new EditText(this);
        etEndTime.setHint("Ending Time (e.g. 11:30 AM)");

        etEndTime.setText(
                isEditing
                        ? existingSchedule.getEndTime()
                        : "11:30 AM"
        );

        layout.addView(etEndTime);

        EditText etSection = new EditText(this);
        etSection.setHint("Section (e.g. Section A)");

        etSection.setText(
                isEditing
                        ? existingSchedule.getSection()
                        : (!"All".equalsIgnoreCase(currentSection)
                        ? currentSection
                        : "Section A")
        );

        layout.addView(etSection);

        CheckBox cbExtraClass = new CheckBox(this);
        cbExtraClass.setText("This is an Extra Class");

        cbExtraClass.setChecked(
                isEditing && existingSchedule.isExtraClass()
        );

        layout.addView(cbExtraClass);

        builder.setView(layout);

        builder.setPositiveButton(
                isEditing ? "Update" : "Schedule",
                (dialog, which) -> {

                    String title = etTitle.getText()
                            .toString().trim();

                    String room = etRoom.getText()
                            .toString().trim();

                    String date = etDate.getText()
                            .toString().trim();

                    String start = etStartTime.getText()
                            .toString().trim();

                    String end = etEndTime.getText()
                            .toString().trim();

                    String sec = etSection.getText()
                            .toString().trim();

                    boolean isExtra = cbExtraClass.isChecked();

                    if (TextUtils.isEmpty(title) ||
                            TextUtils.isEmpty(room) ||
                            TextUtils.isEmpty(date) ||
                            TextUtils.isEmpty(start) ||
                            TextUtils.isEmpty(end) ||
                            TextUtils.isEmpty(sec)) {

                        Toast.makeText(
                                this,
                                "Please fill in all required fields.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    ClassSchedule schedule;

                    if (isEditing) {

                        schedule = existingSchedule;

                    } else {

                        schedule = new ClassSchedule();

                        schedule.setClassroomId(classroomId);
                        schedule.setCancelled(false);
                        schedule.setCreatedByUid(
                                sessionManager.getUid()
                        );

                        schedule.setCreatedByName(
                                sessionManager.getFullName()
                        );
                    }

                    schedule.setSection(sec);
                    schedule.setTitle(title);
                    schedule.setRoomNumber(room);
                    schedule.setDate(date);
                    schedule.setStartTime(start);
                    schedule.setEndTime(end);
                    schedule.setExtraClass(isExtra);

                    ScheduleRepository.Callback<Void> callback =
                            new ScheduleRepository.Callback<Void>() {

                                @Override
                                public void onSuccess(Void result) {

                                    Toast.makeText(
                                            ScheduleActivity.this,
                                            isEditing
                                                    ? "Schedule updated successfully!"
                                                    : "Class scheduled successfully!",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }

                                @Override
                                public void onError(String message) {
                                    showError(message);
                                }
                            };

                    if (isEditing) {

                        scheduleRepo.updateSchedule(
                                schedule,
                                callback
                        );

                    } else {

                        scheduleRepo.createSchedule(
                                schedule,
                                callback
                        );
                    }
                }
        );

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    // Confirm before deleting
    private void confirmDeleteSchedule(ClassSchedule schedule) {

        new AlertDialog.Builder(this)
                .setTitle("Delete Schedule")
                .setMessage(
                        "Are you sure you want to permanently delete this schedule?"
                )
                .setPositiveButton("Delete", (dialog, which) -> {

                    scheduleRepo.deleteSchedule(
                            schedule.getScheduleId(),
                            new ScheduleRepository.Callback<Void>() {

                                @Override
                                public void onSuccess(Void result) {

                                    Toast.makeText(
                                            ScheduleActivity.this,
                                            "Schedule deleted successfully.",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }

                                @Override
                                public void onError(String message) {
                                    showError(message);
                                }
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showError(String message) {

        Toast.makeText(
                this,
                "Error: " + message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (schedulesListener != null) {
            schedulesListener.remove();
        }
    }
}