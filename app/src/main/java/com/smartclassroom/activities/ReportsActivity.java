package com.smartclassroom.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.smartclassroom.databinding.ActivityReportsBinding;
import com.smartclassroom.models.Assessment;
import com.smartclassroom.models.AttendanceRecord;
import com.smartclassroom.models.Classroom;
import com.smartclassroom.models.ClassroomMember;
import com.smartclassroom.models.StudentMark;
import com.smartclassroom.pdf.PdfReportGenerator;
import com.smartclassroom.repositories.ClassroomRepository;
import com.smartclassroom.utils.Constants;
import com.smartclassroom.utils.SessionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReportsActivity extends AppCompatActivity {

    private ActivityReportsBinding binding;
    private ClassroomRepository classroomRepo;
    private SessionManager sessionManager;
    private PdfReportGenerator pdfGenerator;

    private String passedClassroomId;
    private String passedSection;
    private File lastGeneratedFile;

    private final List<Classroom> teacherClassrooms = new ArrayList<>();
    private final List<ClassroomMember> classroomStudents = new ArrayList<>();
    private final List<String> sectionsList = new ArrayList<>();

    private int selectedReportIndex = 0;
    private Classroom selectedClassroom;
    private String selectedSection = "All";
    private ClassroomMember selectedStudent;

    private static final String[] REPORT_TYPES = {
            "1. Complete Attendance Report",
            "2. Individual Student Attendance Report",
            "3. Complete Marks Report",
            "4. Individual Student Marks Report",
            "5. Section-wise Student List",
            "6. Complete Classroom Performance Report"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        classroomRepo = ClassroomRepository.getInstance();
        sessionManager = new SessionManager(this);
        pdfGenerator = new PdfReportGenerator(this);

        passedClassroomId = getIntent().getStringExtra(Constants.EXTRA_CLASSROOM_ID);
        passedSection = getIntent().getStringExtra(Constants.EXTRA_SECTION);

        binding.btnBack.setOnClickListener(v -> finish());

        setupReportTypeSpinner();
        loadClassrooms();
        setupActionButtons();
    }

    private void setupReportTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, REPORT_TYPES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerReportType.setAdapter(adapter);

        binding.spinnerReportType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedReportIndex = position;
                boolean isIndividual = (position == 1 || position == 3);
                binding.layoutStudentSelector.setVisibility(isIndividual ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadClassrooms() {
        String uid = sessionManager.getUid();
        classroomRepo.listenTeacherClassrooms(uid, new ClassroomRepository.Callback<List<Classroom>>() {
            @Override
            public void onSuccess(List<Classroom> list) {
                teacherClassrooms.clear();
                if (list != null) teacherClassrooms.addAll(list);

                List<String> names = new ArrayList<>();
                int initialSelect = 0;
                for (int i = 0; i < teacherClassrooms.size(); i++) {
                    Classroom c = teacherClassrooms.get(i);
                    names.add(c.getCourseName() + " (" + c.getCourseCode() + ")");
                    if (passedClassroomId != null && passedClassroomId.equals(c.getClassroomId())) {
                        initialSelect = i;
                    }
                }

                ArrayAdapter<String> classAdapter = new ArrayAdapter<>(ReportsActivity.this,
                        android.R.layout.simple_spinner_item, names);
                classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.spinnerReportClassroom.setAdapter(classAdapter);

                if (!teacherClassrooms.isEmpty()) {
                    binding.spinnerReportClassroom.setSelection(initialSelect);
                    selectedClassroom = teacherClassrooms.get(initialSelect);
                    loadClassroomStudents(selectedClassroom.getClassroomId());
                }

                binding.spinnerReportClassroom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        selectedClassroom = teacherClassrooms.get(position);
                        loadClassroomStudents(selectedClassroom.getClassroomId());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ReportsActivity.this, "Error loading classrooms: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadClassroomStudents(String cid) {
        classroomRepo.listenClassroomMembers(cid, new ClassroomRepository.Callback<List<ClassroomMember>>() {
            @Override
            public void onSuccess(List<ClassroomMember> list) {
                classroomStudents.clear();
                if (list != null) classroomStudents.addAll(list);

                sectionsList.clear();
                sectionsList.add("All");
                sectionsList.add("Section A");
                sectionsList.add("Section B");

                ArrayAdapter<String> secAdapter = new ArrayAdapter<>(ReportsActivity.this,
                        android.R.layout.simple_spinner_item, sectionsList);
                secAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.spinnerReportSection.setAdapter(secAdapter);

                binding.spinnerReportSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        selectedSection = sectionsList.get(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

                // Student list for individual selector
                List<String> studentNames = new ArrayList<>();
                for (ClassroomMember m : classroomStudents) {
                    studentNames.add(m.getStudentName() + " (Roll " + m.getRollNumber() + ")");
                }
                ArrayAdapter<String> studentAdapter = new ArrayAdapter<>(ReportsActivity.this,
                        android.R.layout.simple_spinner_item, studentNames);
                studentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.spinnerReportStudent.setAdapter(studentAdapter);

                if (!classroomStudents.isEmpty()) {
                    selectedStudent = classroomStudents.get(0);
                }

                binding.spinnerReportStudent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        selectedStudent = classroomStudents.get(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void setupActionButtons() {
        binding.btnGeneratePreview.setOnClickListener(v -> generateReport(true));
        binding.btnSharePdf.setOnClickListener(v -> {
            if (lastGeneratedFile != null && lastGeneratedFile.exists()) {
                pdfGenerator.sharePdf(lastGeneratedFile);
            } else {
                generateReportAndThen(file -> pdfGenerator.sharePdf(file));
            }
        });
        binding.btnDownloadPdf.setOnClickListener(v -> {
            if (lastGeneratedFile != null && lastGeneratedFile.exists()) {
                pdfGenerator.downloadToPublicStorage(lastGeneratedFile);
            } else {
                generateReportAndThen(file -> pdfGenerator.downloadToPublicStorage(file));
            }
        });
    }

    interface FileCallback {
        void onFileReady(File file);
    }

    private void generateReportAndThen(FileCallback callback) {
        generateReportInternal(callback);
    }

    private void generateReport(boolean autoOpen) {
        generateReportInternal(file -> {
            if (autoOpen) {
                pdfGenerator.openPdf(file);
            } else {
                Toast.makeText(ReportsActivity.this, "PDF Report Generated: " + file.getName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateReportInternal(FileCallback callback) {
        if (selectedClassroom == null) {
            Toast.makeText(this, "Please select a classroom", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch required data: attendance records, assessments, marks
        db.collection(Constants.COLLECTION_ATTENDANCE_RECORDS)
                .whereEqualTo("classroomId", selectedClassroom.getClassroomId())
                .get()
                .addOnSuccessListener(attSnaps -> {
                    List<AttendanceRecord> attendanceRecords = new ArrayList<>();
                    if (attSnaps != null) {
                        for (var doc : attSnaps.getDocuments()) {
                            AttendanceRecord r = doc.toObject(AttendanceRecord.class);
                            if (r != null) attendanceRecords.add(r);
                        }
                    }

                    db.collection(Constants.COLLECTION_ASSESSMENTS)
                            .whereEqualTo("classroomId", selectedClassroom.getClassroomId())
                            .get()
                            .addOnSuccessListener(assessSnaps -> {
                                List<Assessment> assessments = new ArrayList<>();
                                if (assessSnaps != null) {
                                    for (var doc : assessSnaps.getDocuments()) {
                                        Assessment a = doc.toObject(Assessment.class);
                                        if (a != null) assessments.add(a);
                                    }
                                }

                                db.collection(Constants.COLLECTION_STUDENT_MARKS)
                                        .whereEqualTo("classroomId", selectedClassroom.getClassroomId())
                                        .get()
                                        .addOnSuccessListener(markSnaps -> {
                                            List<StudentMark> marks = new ArrayList<>();
                                            if (markSnaps != null) {
                                                for (var doc : markSnaps.getDocuments()) {
                                                    StudentMark m = doc.toObject(StudentMark.class);
                                                    if (m != null) marks.add(m);
                                                }
                                            }

                                            try {
                                                File generated = null;
                                                switch (selectedReportIndex) {
                                                    case 0: // Complete Attendance
                                                        generated = pdfGenerator.generateCompleteAttendanceReport(
                                                                selectedClassroom, selectedSection, attendanceRecords, classroomStudents);
                                                        break;
                                                    case 1: // Individual Attendance
                                                        if (selectedStudent != null) {
                                                            List<AttendanceRecord> sRecs = new ArrayList<>();
                                                            for (AttendanceRecord ar : attendanceRecords) {
                                                                if (selectedStudent.getStudentUid().equals(ar.getStudentUid())) {
                                                                    sRecs.add(ar);
                                                                }
                                                            }
                                                            generated = pdfGenerator.generateIndividualAttendanceReport(
                                                                    selectedClassroom, selectedStudent, sRecs);
                                                        }
                                                        break;
                                                    case 2: // Complete Marks
                                                        generated = pdfGenerator.generateCompleteMarksReport(
                                                                selectedClassroom, selectedSection, assessments, classroomStudents, marks);
                                                        break;
                                                    case 3: // Individual Marks
                                                        if (selectedStudent != null) {
                                                            List<StudentMark> sMarks = new ArrayList<>();
                                                            for (StudentMark sm : marks) {
                                                                if (selectedStudent.getStudentUid().equals(sm.getStudentUid())) {
                                                                    sMarks.add(sm);
                                                                }
                                                            }
                                                            generated = pdfGenerator.generateIndividualMarksReport(
                                                                    selectedClassroom, selectedStudent, assessments, sMarks);
                                                        }
                                                        break;
                                                    case 4: // Section Student List
                                                        generated = pdfGenerator.generateSectionStudentListReport(
                                                                selectedClassroom, selectedSection, classroomStudents);
                                                        break;
                                                    case 5: // Performance Summary
                                                        generated = pdfGenerator.generatePerformanceSummaryReport(
                                                                selectedClassroom, selectedSection, classroomStudents, attendanceRecords, marks);
                                                        break;
                                                }

                                                binding.progressBar.setVisibility(View.GONE);
                                                if (generated != null) {
                                                    lastGeneratedFile = generated;
                                                    callback.onFileReady(generated);
                                                } else {
                                                    Toast.makeText(ReportsActivity.this, "Could not generate report. Please verify student selection.", Toast.LENGTH_SHORT).show();
                                                }
                                            } catch (Exception e) {
                                                binding.progressBar.setVisibility(View.GONE);
                                                Toast.makeText(ReportsActivity.this, "PDF Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            binding.progressBar.setVisibility(View.GONE);
                                            Toast.makeText(ReportsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(ReportsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(ReportsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
