package com.smartclassroom.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.smartclassroom.databinding.ActivityCreateClassroomBinding;
import com.smartclassroom.models.Classroom;
import com.smartclassroom.repositories.ClassroomRepository;
import com.smartclassroom.utils.CodeGenerator;
import com.smartclassroom.utils.SessionManager;

public class CreateClassroomActivity extends AppCompatActivity {

    private ActivityCreateClassroomBinding binding;
    private ClassroomRepository classroomRepo;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateClassroomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        classroomRepo = ClassroomRepository.getInstance();
        sessionManager = new SessionManager(this);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSubmitCreate.setOnClickListener(v -> handleCreateClassroom());
    }

    private void handleCreateClassroom() {
        String classroomName = getText(binding.etClassroomName);
        String courseName = getText(binding.etCourseName);
        String courseCode = getText(binding.etCourseCode);
        String creditStr = getText(binding.etCourseCredit);
        String department = getText(binding.etDepartment);
        String semester = getText(binding.etSemester);
        String section = getText(binding.etSection);
        String plannedClassesStr = getText(binding.etPlannedClasses);
        String totalStudentsStr = getText(binding.etTotalStudents);
        String description = getText(binding.etDescription);

        if (TextUtils.isEmpty(classroomName)) {
            binding.tilClassroomName.setError("Classroom name is required");
            binding.etClassroomName.requestFocus();
            return;
        }
        binding.tilClassroomName.setError(null);

        if (TextUtils.isEmpty(courseName)) {
            binding.tilCourseName.setError("Course name is required");
            binding.etCourseName.requestFocus();
            return;
        }
        binding.tilCourseName.setError(null);

        if (TextUtils.isEmpty(courseCode)) {
            binding.tilCourseCode.setError("Course code is required");
            binding.etCourseCode.requestFocus();
            return;
        }
        binding.tilCourseCode.setError(null);

        double credit = 3.0;
        try {
            if (!TextUtils.isEmpty(creditStr)) {
                credit = Double.parseDouble(creditStr);
            }
        } catch (NumberFormatException e) {
            binding.tilCourseCredit.setError("Invalid credits");
            return;
        }
        binding.tilCourseCredit.setError(null);

        if (TextUtils.isEmpty(department)) {
            binding.tilDepartment.setError("Department is required");
            binding.etDepartment.requestFocus();
            return;
        }
        binding.tilDepartment.setError(null);

        if (TextUtils.isEmpty(semester)) {
            binding.tilSemester.setError("Semester is required");
            binding.etSemester.requestFocus();
            return;
        }
        binding.tilSemester.setError(null);

        if (TextUtils.isEmpty(section)) {
            section = "Section A";
        }

        int plannedClasses = 30;
        try {
            if (!TextUtils.isEmpty(plannedClassesStr)) {
                plannedClasses = Integer.parseInt(plannedClassesStr);
            }
        } catch (NumberFormatException ignored) {}

        int totalStudents = 50;
        try {
            if (!TextUtils.isEmpty(totalStudentsStr)) {
                totalStudents = Integer.parseInt(totalStudentsStr);
            }
        } catch (NumberFormatException ignored) {}

        setLoading(true);

        String code = CodeGenerator.generateClassroomCode();

        Classroom classroom = new Classroom();
        classroom.setClassroomCode(code);
        classroom.setClassroomName(classroomName);
        classroom.setCourseName(courseName);
        classroom.setCourseCode(courseCode);
        classroom.setCourseCredit(credit);
        classroom.setDepartment(department);
        classroom.setSemester(semester);
        classroom.setSection(section);
        classroom.setTotalPlannedClasses(plannedClasses);
        classroom.setTotalStudents(totalStudents);
        classroom.setDescription(description);
        classroom.setTeacherId(sessionManager.getUid());
        classroom.setTeacherName(sessionManager.getFullName());

        classroomRepo.createClassroom(classroom, new ClassroomRepository.Callback<Classroom>() {
            @Override
            public void onSuccess(Classroom result) {
                setLoading(false);
                Toast.makeText(CreateClassroomActivity.this, "Classroom Created! Code: " + result.getClassroomCode(), Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(CreateClassroomActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getText(android.widget.EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnSubmitCreate.setEnabled(!isLoading);
        binding.btnBack.setEnabled(!isLoading);
    }
}
