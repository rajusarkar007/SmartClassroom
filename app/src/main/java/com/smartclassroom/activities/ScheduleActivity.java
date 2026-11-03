package com.smartclassroom.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import java.util.Locale;

public class ScheduleActivity extends AppCompatActivity {

    private ActivityScheduleBinding binding;
    private ScheduleRepository scheduleRepo;
    private SessionManager sessionManager;
    private ScheduleAdapter adapter;

    private String classroomId;
    private String currentSection = "All";
    private boolean isTeacher;
    private boolean isCR;
    private boolean canManage;

    private ListenerRegistration schedulesListener;
    private final List<ClassSchedule> schedulesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        scheduleRepo = ScheduleRepository.getInstance();
        sessionManager = new SessionManager(this);

        classroomId = getIntent().getStringExtra(Constants.EXTRA_CLASSROOM_ID);
        isTeacher = getIntent().getBooleanExtra(Constants.EXTRA_IS_TEACHER, false);
        isCR = getIntent().getBooleanExtra(Constants.EXTRA_IS_CR, false);
        canManage = isTeacher || isCR; // Teacher and Class Representative can manage schedule

        if (getIntent().getStringExtra(Constants.EXTRA_SECTION) != null) {
            currentSection = getIntent().getStringExtra(Constants.EXTRA_SECTION);
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.fabAddSchedule.setVisibility(canManage ? View.VISIBLE : View.GONE);
        binding.fabAddSchedule.setOnClickListener(v -> showAddScheduleDialog());

        setupRecyclerView();
        listenSchedules();
    }

    private void setupRecyclerView() {
        adapter = new ScheduleAdapter(canManage, schedule -> {
            boolean newCancel = !schedule.isCancelled();
            scheduleRepo.toggleClassCancelled(schedule.getScheduleId(), newCancel, new ScheduleRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(ScheduleActivity.this,
                            newCancel ? "Class marked as CANCELLED." : "Class schedule resumed.",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(ScheduleActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.rvSchedules.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSchedules.setAdapter(adapter);
    }

    private void listenSchedules() {
        if (classroomId == null) return;
        schedulesListener = scheduleRepo.listenSchedules(classroomId, currentSection, new ScheduleRepository.Callback<List<ClassSchedule>>() {
            @Override
            public void onSuccess(List<ClassSchedule> result) {
                schedulesList.clear();
                if (result != null) schedulesList.addAll(result);
                adapter.updateList(schedulesList);

                boolean empty = schedulesList.isEmpty();
                binding.layoutEmptySchedule.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.rvSchedules.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ScheduleActivity.this, "Error loading schedules: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddScheduleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Schedule Class");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Class Title / Topic");
        layout.addView(etTitle);

        EditText etRoom = new EditText(this);
        etRoom.setHint("Classroom / Room Number (e.g. Room 402)");
        layout.addView(etRoom);

        EditText etDate = new EditText(this);
        etDate.setHint("Date");
        etDate.setText(DateUtils.getCurrentDate());
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                c.set(year, month, dayOfMonth);
                etDate.setText(DateUtils.formatDate(c.getTimeInMillis()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        layout.addView(etDate);

        EditText etStartTime = new EditText(this);
        etStartTime.setHint("Starting Time (e.g. 10:00 AM)");
        etStartTime.setText("10:00 AM");
        layout.addView(etStartTime);

        EditText etEndTime = new EditText(this);
        etEndTime.setHint("Ending Time (e.g. 11:30 AM)");
        etEndTime.setText("11:30 AM");
        layout.addView(etEndTime);

        EditText etSection = new EditText(this);
        etSection.setHint("Section (e.g. Section A)");
        etSection.setText(!"All".equalsIgnoreCase(currentSection) ? currentSection : "Section A");
        layout.addView(etSection);

        CheckBox cbExtraClass = new CheckBox(this);
        cbExtraClass.setText("This is an Extra Class");
        layout.addView(cbExtraClass);

        builder.setView(layout);
        builder.setPositiveButton("Schedule", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String room = etRoom.getText().toString().trim();
            String date = etDate.getText().toString().trim();
            String start = etStartTime.getText().toString().trim();
            String end = etEndTime.getText().toString().trim();
            String sec = etSection.getText().toString().trim();
            boolean isExtra = cbExtraClass.isChecked();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(room)) {
                Toast.makeText(this, "Title and Room Number are required", Toast.LENGTH_SHORT).show();
                return;
            }

            ClassSchedule s = new ClassSchedule();
            s.setClassroomId(classroomId);
            s.setSection(sec);
            s.setTitle(title);
            s.setRoomNumber(room);
            s.setDate(date);
            s.setStartTime(start);
            s.setEndTime(end);
            s.setCancelled(false);
            s.setExtraClass(isExtra);
            s.setCreatedByUid(sessionManager.getUid());
            s.setCreatedByName(sessionManager.getFullName() + (isTeacher ? " (Teacher)" : " (CR)"));

            scheduleRepo.createSchedule(s, new ScheduleRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(ScheduleActivity.this, "Class scheduled successfully!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(ScheduleActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (schedulesListener != null) schedulesListener.remove();
    }
}
