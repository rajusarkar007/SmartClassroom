package com.smartclassroom.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.smartclassroom.adapters.AssessmentAdapter;
import com.smartclassroom.adapters.StudentMarksAdapter;
import com.smartclassroom.databinding.ActivityMarksManagementBinding;
import com.smartclassroom.models.Assessment;
import com.smartclassroom.models.ClassroomMember;
import com.smartclassroom.models.StudentMark;
import com.smartclassroom.repositories.ClassroomRepository;
import com.smartclassroom.repositories.MarksRepository;
import com.smartclassroom.utils.Constants;
import com.smartclassroom.utils.DateUtils;
import com.smartclassroom.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MarksManagementActivity extends AppCompatActivity {

    private ActivityMarksManagementBinding binding;
    private MarksRepository marksRepo;
    private ClassroomRepository classroomRepo;
    private SessionManager sessionManager;
    private AssessmentAdapter adapter;

    private String classroomId;
    private String currentSection = "All";
    private boolean isTeacher;
    private ListenerRegistration assessmentsListener;
    private ListenerRegistration studentMarksListener;

    private final List<Assessment> assessmentsList = new ArrayList<>();
    private final List<ClassroomMember> classroomStudents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMarksManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        marksRepo = MarksRepository.getInstance();
        classroomRepo = ClassroomRepository.getInstance();
        sessionManager = new SessionManager(this);

        classroomId = getIntent().getStringExtra(Constants.EXTRA_CLASSROOM_ID);
        isTeacher = getIntent().getBooleanExtra(Constants.EXTRA_IS_TEACHER, false);
        if (getIntent().getStringExtra(Constants.EXTRA_SECTION) != null) {
            currentSection = getIntent().getStringExtra(Constants.EXTRA_SECTION);
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.fabCreateAssessment.setVisibility(isTeacher ? View.VISIBLE : View.GONE);
        binding.fabCreateAssessment.setOnClickListener(v -> showCreateAssessmentDialog());

        setupRecyclerView();
        setupSpinner();
        loadClassroomMembers();

        if (isTeacher) {
            binding.cardStudentTotalMarks.setVisibility(View.GONE);
            listenAssessments();
        } else {
            binding.layoutSectionFilter.setVisibility(View.GONE);
            binding.cardStudentTotalMarks.setVisibility(View.VISIBLE);
            listenAssessments();
            listenStudentTotalMarks();
        }
    }

    private void setupRecyclerView() {
        adapter = new AssessmentAdapter(isTeacher, assessment -> showEnterMarksDialog(assessment));
        binding.rvAssessments.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAssessments.setAdapter(adapter);
    }

    private void setupSpinner() {
        List<String> list = new ArrayList<>();
        list.add("All");
        list.add("Section A");
        list.add("Section B");
        list.add("Section C");

        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMarksSection.setAdapter(spinAdapter);

        binding.spinnerMarksSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSection = list.get(position);
                listenAssessments();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadClassroomMembers() {
        if (classroomId == null) return;
        classroomRepo.listenClassroomMembers(classroomId, new ClassroomRepository.Callback<List<ClassroomMember>>() {
            @Override
            public void onSuccess(List<ClassroomMember> result) {
                classroomStudents.clear();
                if (result != null) classroomStudents.addAll(result);
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void listenAssessments() {
        if (assessmentsListener != null) assessmentsListener.remove();
        if (classroomId == null) return;

        assessmentsListener = marksRepo.listenAssessments(classroomId, currentSection, new MarksRepository.Callback<List<Assessment>>() {
            @Override
            public void onSuccess(List<Assessment> result) {
                assessmentsList.clear();
                if (result != null) assessmentsList.addAll(result);
                adapter.updateList(assessmentsList);

                boolean empty = assessmentsList.isEmpty();
                binding.layoutEmptyMarks.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.rvAssessments.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MarksManagementActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenStudentTotalMarks() {
        if (classroomId == null) return;
        String uid = sessionManager.getUid();

        studentMarksListener = marksRepo.listenStudentMarks(classroomId, uid, new MarksRepository.Callback<List<StudentMark>>() {
            @Override
            public void onSuccess(List<StudentMark> marks) {
                double obt = 0;
                double max = 0;
                if (marks != null) {
                    for (StudentMark m : marks) {
                        obt += m.getObtainedMarks();
                        max += m.getMaxMarks();
                    }
                }
                double pct = max > 0 ? (obt / max) * 100.0 : 0.0;
                binding.tvTotalObtainedAndMax.setText(String.format(Locale.getDefault(), "Obtained: %.1f / %.1f", obt, max));
                binding.tvTotalPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", pct));
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void showCreateAssessmentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Assessment");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Assessment Title (e.g. Class Test 1)");
        layout.addView(etTitle);

        Spinner spCategory = new Spinner(this);
        String[] cats = {Constants.CATEGORY_CT, Constants.CATEGORY_ATTENDANCE, Constants.CATEGORY_SPOT_TEST, Constants.CATEGORY_CLASS_PERFORMANCE};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cats);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);
        layout.addView(spCategory);

        EditText etMax = new EditText(this);
        etMax.setHint("Maximum Marks (e.g. 20)");
        etMax.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etMax);

        EditText etSection = new EditText(this);
        etSection.setHint("Section (e.g. Section A)");
        etSection.setText(!"All".equalsIgnoreCase(currentSection) ? currentSection : "Section A");
        layout.addView(etSection);

        EditText etRemarks = new EditText(this);
        etRemarks.setHint("Remarks (Optional)");
        layout.addView(etRemarks);

        builder.setView(layout);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String cat = cats[spCategory.getSelectedItemPosition()];
            String maxStr = etMax.getText().toString().trim();
            String sec = etSection.getText().toString().trim();
            String remarks = etRemarks.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(maxStr)) {
                Toast.makeText(this, "Title and Maximum Marks are required", Toast.LENGTH_SHORT).show();
                return;
            }

            double maxMarks = Double.parseDouble(maxStr);

            Assessment a = new Assessment();
            a.setClassroomId(classroomId);
            a.setTitle(title);
            a.setCategory(cat);
            a.setMaxMarks(maxMarks);
            a.setSection(sec);
            a.setDate(DateUtils.getCurrentDate());
            a.setRemarks(remarks);

            marksRepo.createAssessment(a, new MarksRepository.Callback<Assessment>() {
                @Override
                public void onSuccess(Assessment result) {
                    Toast.makeText(MarksManagementActivity.this, "Assessment created successfully!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(MarksManagementActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showEnterMarksDialog(Assessment assessment) {
        marksRepo.getMarksForAssessment(assessment.getAssessmentId(), new MarksRepository.Callback<List<StudentMark>>() {
            @Override
            public void onSuccess(List<StudentMark> existingMarks) {
                // Filter classroomStudents for assessment's section
                List<ClassroomMember> secStudents = new ArrayList<>();
                for (ClassroomMember s : classroomStudents) {
                    if (assessment.getSection() == null || "All".equalsIgnoreCase(assessment.getSection()) || assessment.getSection().equalsIgnoreCase(s.getSection())) {
                        secStudents.add(s);
                    }
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(MarksManagementActivity.this);
                builder.setTitle("Enter Marks: " + assessment.getTitle());

                RecyclerView rv = new RecyclerView(MarksManagementActivity.this);
                rv.setLayoutManager(new LinearLayoutManager(MarksManagementActivity.this));
                StudentMarksAdapter markAdapter = new StudentMarksAdapter(
                        classroomId, assessment.getAssessmentId(), assessment.getSection(), assessment.getMaxMarks());
                markAdapter.setStudentsAndMarks(secStudents, existingMarks);
                rv.setAdapter(markAdapter);

                builder.setView(rv);
                builder.setPositiveButton("Save All Marks", (dialog, which) -> {
                    List<StudentMark> updated = markAdapter.getUpdatedMarks();
                    marksRepo.saveStudentMarks(updated, new MarksRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(MarksManagementActivity.this, "Marks saved and updated for students!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(MarksManagementActivity.this, "Error saving marks: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MarksManagementActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (assessmentsListener != null) assessmentsListener.remove();
        if (studentMarksListener != null) studentMarksListener.remove();
    }
}
