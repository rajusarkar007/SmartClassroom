package com.smartclassroom.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.smartclassroom.databinding.ActivityClassroomDetailBinding;
import com.smartclassroom.models.Classroom;
import com.smartclassroom.repositories.ClassroomRepository;
import com.smartclassroom.utils.Constants;

public class ClassroomDetailActivity extends AppCompatActivity {

    private ActivityClassroomDetailBinding binding;
    private ClassroomRepository classroomRepo;
    private String classroomId;
    private boolean isTeacher;
    private boolean isCR;
    private String userSection;
    private Classroom currentClassroom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClassroomDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        classroomRepo = ClassroomRepository.getInstance();

        classroomId = getIntent().getStringExtra(Constants.EXTRA_CLASSROOM_ID);
        isTeacher = getIntent().getBooleanExtra(Constants.EXTRA_IS_TEACHER, false);
        isCR = getIntent().getBooleanExtra(Constants.EXTRA_IS_CR, false);
        userSection = getIntent().getStringExtra(Constants.EXTRA_SECTION);

        binding.btnBack.setOnClickListener(v -> finish());

        loadClassroomDetails();
        setupModuleClicks();
    }

    private void loadClassroomDetails() {
        if (classroomId == null) {
            Toast.makeText(this, "Classroom ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        classroomRepo.getClassroomById(classroomId, new ClassroomRepository.Callback<Classroom>() {
            @Override
            public void onSuccess(Classroom c) {
                currentClassroom = c;
                binding.tvCourseNameHeader.setText(c.getCourseName());
                binding.tvCourseDetails.setText(String.format("%s • %.1f Credits • %s",
                        c.getCourseCode(), c.getCourseCredit(), c.getDepartment()));
                binding.tvTeacherName.setText("Instructor: " + c.getTeacherName());
                binding.btnCopyCode.setText("Code: " + c.getClassroomCode() + " 📋");

                if (c.getDescription() != null && !c.getDescription().isEmpty()) {
                    binding.tvDescription.setVisibility(View.VISIBLE);
                    binding.tvDescription.setText(c.getDescription());
                } else {
                    binding.tvDescription.setVisibility(View.GONE);
                }

                binding.btnCopyCode.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Classroom Code", c.getClassroomCode());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(ClassroomDetailActivity.this, "Classroom Code copied to clipboard!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ClassroomDetailActivity.this, "Error loading details: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupModuleClicks() {
        binding.cardActionAttendance.setOnClickListener(v -> {
            Class<?> target = isTeacher ? AttendanceActivity.class : AttendanceHistoryActivity.class;
            openModule(target);
        });

        binding.cardActionMarks.setOnClickListener(v -> openModule(MarksManagementActivity.class));
        binding.cardActionStudents.setOnClickListener(v -> openModule(StudentManagementActivity.class));
        binding.cardActionSchedule.setOnClickListener(v -> openModule(ScheduleActivity.class));
        binding.cardActionNotices.setOnClickListener(v -> openModule(NoticeActivity.class));
        binding.cardActionReports.setOnClickListener(v -> openModule(ReportsActivity.class));
    }

    private void openModule(Class<?> target) {
        if (currentClassroom == null) return;
        Intent intent = new Intent(ClassroomDetailActivity.this, target);
        intent.putExtra(Constants.EXTRA_CLASSROOM_ID, currentClassroom.getClassroomId());
        intent.putExtra(Constants.EXTRA_CLASSROOM_NAME, currentClassroom.getClassroomName());
        intent.putExtra(Constants.EXTRA_COURSE_CODE, currentClassroom.getCourseCode());
        intent.putExtra(Constants.EXTRA_SECTION, userSection != null ? userSection : currentClassroom.getSection());
        intent.putExtra(Constants.EXTRA_IS_TEACHER, isTeacher);
        intent.putExtra(Constants.EXTRA_IS_CR, isCR);
        startActivity(intent);
    }
}
