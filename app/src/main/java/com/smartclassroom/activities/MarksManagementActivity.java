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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MarksManagementActivity extends AppCompatActivity {

    private ActivityMarksManagementBinding binding;

    private MarksRepository marksRepo;
    private ClassroomRepository classroomRepo;
    private SessionManager sessionManager;
    private AssessmentAdapter adapter;

    private String classroomId;
    private String currentSection = "All";
    private boolean isTeacher;
    private boolean membersLoaded = false;

    private ListenerRegistration assessmentsListener;
    private ListenerRegistration studentMarksListener;
    private ListenerRegistration classroomMembersListener;

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

        classroomId = getIntent().getStringExtra(
                Constants.EXTRA_CLASSROOM_ID
        );

        isTeacher = getIntent().getBooleanExtra(
                Constants.EXTRA_IS_TEACHER,
                false
        );

        String passedSection = getIntent().getStringExtra(
                Constants.EXTRA_SECTION
        );

        if (!TextUtils.isEmpty(passedSection)) {
            currentSection = passedSection.trim();
        }

        binding.btnBack.setOnClickListener(v -> finish());

        binding.fabCreateAssessment.setVisibility(
                isTeacher ? View.VISIBLE : View.GONE
        );

        binding.fabCreateAssessment.setOnClickListener(
                v -> showCreateAssessmentDialog()
        );

        setupRecyclerView();

        if (isTeacher) {
            binding.cardStudentTotalMarks.setVisibility(View.GONE);
            binding.layoutSectionFilter.setVisibility(View.VISIBLE);

            setupSpinner();
            loadClassroomMembers();

        } else {
            binding.layoutSectionFilter.setVisibility(View.GONE);
            binding.cardStudentTotalMarks.setVisibility(View.VISIBLE);
        }

        listenAssessments();

        if (!isTeacher) {
            listenStudentTotalMarks();
        }
    }

    // --------------------------------------------------
    // ASSESSMENT RECYCLER VIEW
    // --------------------------------------------------

    private void setupRecyclerView() {

        adapter = new AssessmentAdapter(
                isTeacher,
                assessment -> {
                    if (isTeacher) {
                        showEnterMarksDialog(assessment);
                    }
                }
        );

        binding.rvAssessments.setLayoutManager(
                new LinearLayoutManager(this)
        );

        binding.rvAssessments.setAdapter(adapter);
    }

    // --------------------------------------------------
    // SECTION SPINNER
    // --------------------------------------------------

    private void setupSpinner() {

        List<String> sections = new ArrayList<>();
        sections.add("All");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                sections
        );

        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        binding.spinnerMarksSection.setAdapter(spinnerAdapter);

        binding.spinnerMarksSection.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        currentSection = String.valueOf(
                                parent.getItemAtPosition(position)
                        );

                        listenAssessments();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );
    }

    // --------------------------------------------------
    // LOAD CLASSROOM STUDENTS AND REAL SECTIONS
    // --------------------------------------------------

    private void loadClassroomMembers() {

        if (TextUtils.isEmpty(classroomId)) {
            Toast.makeText(
                    this,
                    "Classroom ID not found.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (classroomMembersListener != null) {
            classroomMembersListener.remove();
        }

        classroomMembersListener =
                classroomRepo.listenClassroomMembers(
                        classroomId,
                        new ClassroomRepository.Callback<List<ClassroomMember>>() {

                            @Override
                            public void onSuccess(List<ClassroomMember> result) {

                                classroomStudents.clear();

                                if (result != null) {
                                    classroomStudents.addAll(result);
                                }

                                membersLoaded = true;

                                populateSectionSpinner();
                            }

                            @Override
                            public void onError(String message) {

                                membersLoaded = false;

                                Toast.makeText(
                                        MarksManagementActivity.this,
                                        "Student loading error: " + message,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    private void populateSectionSpinner() {

        Set<String> sectionSet = new LinkedHashSet<>();

        for (ClassroomMember student : classroomStudents) {

            String section = student.getSection();

            if (!TextUtils.isEmpty(section)) {
                sectionSet.add(section.trim());
            }
        }

        List<String> sections = new ArrayList<>();
        sections.add("All");

        List<String> sortedSections = new ArrayList<>(sectionSet);

        Collections.sort(
                sortedSections,
                String.CASE_INSENSITIVE_ORDER
        );

        sections.addAll(sortedSections);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                sections
        );

        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        binding.spinnerMarksSection.setAdapter(spinnerAdapter);

        int position = sections.indexOf(currentSection);

        if (position < 0) {
            currentSection = "All";
            position = 0;
        }

        binding.spinnerMarksSection.setSelection(position);
    }

    // --------------------------------------------------
    // LOAD ASSESSMENTS
    // --------------------------------------------------

    private void listenAssessments() {

        if (assessmentsListener != null) {
            assessmentsListener.remove();
            assessmentsListener = null;
        }

        if (TextUtils.isEmpty(classroomId)) {
            return;
        }

        assessmentsListener = marksRepo.listenAssessments(
                classroomId,
                isTeacher ? currentSection : "All",
                new MarksRepository.Callback<List<Assessment>>() {

                    @Override
                    public void onSuccess(List<Assessment> result) {

                        assessmentsList.clear();

                        if (result != null) {
                            assessmentsList.addAll(result);
                        }

                        adapter.updateList(assessmentsList);

                        boolean empty = assessmentsList.isEmpty();

                        binding.layoutEmptyMarks.setVisibility(
                                empty ? View.VISIBLE : View.GONE
                        );

                        binding.rvAssessments.setVisibility(
                                empty ? View.GONE : View.VISIBLE
                        );
                    }

                    @Override
                    public void onError(String message) {

                        Toast.makeText(
                                MarksManagementActivity.this,
                                "Assessment error: " + message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // --------------------------------------------------
    // STUDENT TOTAL MARKS
    // --------------------------------------------------

    private void listenStudentTotalMarks() {

        if (TextUtils.isEmpty(classroomId)) {
            return;
        }

        String uid = sessionManager.getUid();

        if (TextUtils.isEmpty(uid)) {
            return;
        }

        if (studentMarksListener != null) {
            studentMarksListener.remove();
        }

        studentMarksListener = marksRepo.listenStudentMarks(
                classroomId,
                uid,
                new MarksRepository.Callback<List<StudentMark>>() {

                    @Override
                    public void onSuccess(List<StudentMark> marks) {

                        double obtained = 0;
                        double maximum = 0;

                        if (marks != null) {

                            for (StudentMark mark : marks) {
                                obtained += mark.getObtainedMarks();
                                maximum += mark.getMaxMarks();
                            }
                        }

                        double percentage = maximum > 0
                                ? (obtained / maximum) * 100.0
                                : 0.0;

                        binding.tvTotalObtainedAndMax.setText(
                                String.format(
                                        Locale.getDefault(),
                                        "Obtained: %.1f / %.1f",
                                        obtained,
                                        maximum
                                )
                        );

                        binding.tvTotalPercentage.setText(
                                String.format(
                                        Locale.getDefault(),
                                        "%.1f%%",
                                        percentage
                                )
                        );
                    }

                    @Override
                    public void onError(String message) {

                        Toast.makeText(
                                MarksManagementActivity.this,
                                "Marks error: " + message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // --------------------------------------------------
    // CREATE ASSESSMENT
    // --------------------------------------------------

    private void showCreateAssessmentDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Create New Assessment");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (
                18 * getResources().getDisplayMetrics().density
        );

        layout.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        EditText etTitle = new EditText(this);
        etTitle.setHint("Assessment Title (e.g. CT 1)");
        layout.addView(etTitle);

        Spinner spCategory = new Spinner(this);

        String[] categories = {
                Constants.CATEGORY_CT,
                Constants.CATEGORY_ATTENDANCE,
                Constants.CATEGORY_SPOT_TEST,
                Constants.CATEGORY_CLASS_PERFORMANCE
        };

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );

        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spCategory.setAdapter(categoryAdapter);
        layout.addView(spCategory);

        EditText etMax = new EditText(this);
        etMax.setHint("Maximum Marks (e.g. 20)");

        etMax.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        layout.addView(etMax);

        EditText etSection = new EditText(this);
        etSection.setHint("Section (e.g. Section A)");

        etSection.setText(
                !"All".equalsIgnoreCase(currentSection)
                        ? currentSection
                        : ""
        );

        layout.addView(etSection);

        EditText etRemarks = new EditText(this);
        etRemarks.setHint("Remarks (Optional)");
        layout.addView(etRemarks);

        builder.setView(layout);

        builder.setPositiveButton("Create", (dialog, which) -> {

            String title = etTitle.getText().toString().trim();
            String maxText = etMax.getText().toString().trim();
            String section = etSection.getText().toString().trim();
            String remarks = etRemarks.getText().toString().trim();

            String category = categories[
                    spCategory.getSelectedItemPosition()
                    ];

            if (TextUtils.isEmpty(title)
                    || TextUtils.isEmpty(maxText)
                    || TextUtils.isEmpty(section)) {

                Toast.makeText(
                        this,
                        "Title, maximum marks and section are required.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            double maxMarks;

            try {
                maxMarks = Double.parseDouble(maxText);
            } catch (NumberFormatException e) {

                Toast.makeText(
                        this,
                        "Enter a valid maximum mark.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (maxMarks <= 0) {

                Toast.makeText(
                        this,
                        "Maximum marks must be greater than zero.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Assessment assessment = new Assessment();

            assessment.setClassroomId(classroomId);
            assessment.setTitle(title);
            assessment.setCategory(category);
            assessment.setMaxMarks(maxMarks);
            assessment.setSection(section);
            assessment.setDate(DateUtils.getCurrentDate());
            assessment.setRemarks(remarks);

            marksRepo.createAssessment(
                    assessment,
                    new MarksRepository.Callback<Assessment>() {

                        @Override
                        public void onSuccess(Assessment result) {

                            Toast.makeText(
                                    MarksManagementActivity.this,
                                    "Assessment created successfully!",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        @Override
                        public void onError(String message) {

                            Toast.makeText(
                                    MarksManagementActivity.this,
                                    "Create error: " + message,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // --------------------------------------------------
    // OPEN MARK INPUT SCREEN
    // --------------------------------------------------

    private void showEnterMarksDialog(Assessment assessment) {

        if (!membersLoaded) {

            Toast.makeText(
                    this,
                    "Students are loading. Please try again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (classroomStudents.isEmpty()) {

            Toast.makeText(
                    this,
                    "No students found in this classroom.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        marksRepo.getMarksForAssessment(
                classroomId,
                assessment.getAssessmentId(),
                new MarksRepository.Callback<List<StudentMark>>() {

                    @Override
                    public void onSuccess(List<StudentMark> existingMarks) {

                        List<ClassroomMember> sectionStudents =
                                new ArrayList<>();

                        String assessmentSection = assessment.getSection();

                        for (ClassroomMember student : classroomStudents) {

                            String studentSection = student.getSection();

                            if (TextUtils.isEmpty(assessmentSection)
                                    || "All".equalsIgnoreCase(assessmentSection)
                                    || (!TextUtils.isEmpty(studentSection)
                                    && assessmentSection.trim().equalsIgnoreCase(
                                    studentSection.trim()
                            ))) {

                                sectionStudents.add(student);
                            }
                        }

                        if (sectionStudents.isEmpty()) {

                            Toast.makeText(
                                    MarksManagementActivity.this,
                                    "No students found in section: "
                                            + assessmentSection,
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        // Only pass marks belonging to this section.
                        List<StudentMark> sectionMarks = new ArrayList<>();

                        for (StudentMark mark : existingMarks) {

                            for (ClassroomMember student : sectionStudents) {

                                if (student.getStudentUid() != null
                                        && student.getStudentUid().equals(
                                        mark.getStudentUid()
                                )) {

                                    sectionMarks.add(mark);
                                    break;
                                }
                            }
                        }

                        showMarksEntryDialog(
                                assessment,
                                sectionStudents,
                                sectionMarks
                        );
                    }

                    @Override
                    public void onError(String message) {

                        Toast.makeText(
                                MarksManagementActivity.this,
                                "Unable to load marks: " + message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // --------------------------------------------------
    // MARKS INPUT DIALOG
    // --------------------------------------------------

    private void showMarksEntryDialog(
            Assessment assessment,
            List<ClassroomMember> students,
            List<StudentMark> existingMarks
    ) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(
                "Enter Marks: " + assessment.getTitle()
        );

        RecyclerView recyclerView = new RecyclerView(this);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        StudentMarksAdapter marksAdapter = new StudentMarksAdapter(
                classroomId,
                assessment.getAssessmentId(),
                assessment.getSection(),
                assessment.getMaxMarks()
        );

        marksAdapter.setStudentsAndMarks(
                students,
                existingMarks
        );

        recyclerView.setAdapter(marksAdapter);

        int padding = (int) (
                8 * getResources().getDisplayMetrics().density
        );

        recyclerView.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        builder.setView(recyclerView);

        builder.setPositiveButton(
                "Save All Marks",
                (dialog, which) -> {

                    List<StudentMark> updatedMarks =
                            marksAdapter.getUpdatedMarks();

                    if (updatedMarks.isEmpty()) {

                        Toast.makeText(
                                MarksManagementActivity.this,
                                "No marks to save.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    marksRepo.saveStudentMarks(
                            updatedMarks,
                            new MarksRepository.Callback<Void>() {

                                @Override
                                public void onSuccess(Void result) {

                                    Toast.makeText(
                                            MarksManagementActivity.this,
                                            "Marks saved successfully!",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }

                                @Override
                                public void onError(String message) {

                                    Toast.makeText(
                                            MarksManagementActivity.this,
                                            "Save error: " + message,
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }
                    );
                }
        );

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    // --------------------------------------------------
    // CLEANUP
    // --------------------------------------------------

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (assessmentsListener != null) {
            assessmentsListener.remove();
        }

        if (studentMarksListener != null) {
            studentMarksListener.remove();
        }

        if (classroomMembersListener != null) {
            classroomMembersListener.remove();
        }
    }
}