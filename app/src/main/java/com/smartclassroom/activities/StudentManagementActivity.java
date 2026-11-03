package com.smartclassroom.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.ListenerRegistration;
import com.smartclassroom.R;
import com.smartclassroom.adapters.StudentAdapter;
import com.smartclassroom.databinding.ActivityStudentManagementBinding;
import com.smartclassroom.models.ClassroomMember;
import com.smartclassroom.repositories.ClassroomRepository;
import com.smartclassroom.utils.Constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StudentManagementActivity extends AppCompatActivity {

    private ActivityStudentManagementBinding binding;
    private ClassroomRepository classroomRepo;
    private StudentAdapter adapter;

    private String classroomId;
    private boolean isTeacher;
    private ListenerRegistration membersListener;

    private final List<ClassroomMember> allMembers = new ArrayList<>();
    private final List<ClassroomMember> sectionFilteredMembers = new ArrayList<>();
    private final List<String> sectionsList = new ArrayList<>();
    private String selectedSection = "All Sections";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        classroomRepo = ClassroomRepository.getInstance();

        classroomId = getIntent().getStringExtra(Constants.EXTRA_CLASSROOM_ID);
        isTeacher = getIntent().getBooleanExtra(Constants.EXTRA_IS_TEACHER, false);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.fabAddStudent.setVisibility(isTeacher ? View.VISIBLE : View.GONE);
        binding.fabAddStudent.setOnClickListener(v -> showAddStudentDialog(null));

        setupRecyclerView();
        setupSearch();
        listenMembers();
    }

    private void setupRecyclerView() {
        adapter = new StudentAdapter(isTeacher, new StudentAdapter.StudentActionListener() {
            @Override
            public void onEdit(ClassroomMember student) {
                showAddStudentDialog(student);
            }

            @Override
            public void onDelete(ClassroomMember student) {
                showDeleteConfirmDialog(student);
            }

            @Override
            public void onToggleCr(ClassroomMember student) {
                boolean newCR = !student.isClassRepresentative();
                classroomRepo.setRepresentativeStatus(student.getMemberId(), newCR, new ClassroomRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(StudentManagementActivity.this,
                                student.getStudentName() + (newCR ? " is now Class Representative (CR)" : " CR permission revoked"),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(StudentManagementActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onStudentClick(ClassroomMember student) {
                showStudentProfileDialog(student);
            }
        });

        binding.rvStudents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStudents.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void listenMembers() {
        if (classroomId == null) return;

        membersListener = classroomRepo.listenClassroomMembers(classroomId, new ClassroomRepository.Callback<List<ClassroomMember>>() {
            @Override
            public void onSuccess(List<ClassroomMember> result) {
                allMembers.clear();
                if (result != null) allMembers.addAll(result);
                updateSectionsSpinner();
                filterBySection(selectedSection);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(StudentManagementActivity.this, "Error loading students: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSectionsSpinner() {
        Set<String> set = new HashSet<>();
        set.add("All Sections");
        for (ClassroomMember m : allMembers) {
            if (m.getSection() != null && !m.getSection().trim().isEmpty()) {
                set.add(m.getSection());
            }
        }
        sectionsList.clear();
        sectionsList.addAll(set);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sectionsList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSection.setAdapter(spinnerAdapter);

        binding.spinnerSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSection = sectionsList.get(position);
                filterBySection(selectedSection);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void filterBySection(String sec) {
        sectionFilteredMembers.clear();
        if ("All Sections".equalsIgnoreCase(sec)) {
            sectionFilteredMembers.addAll(allMembers);
        } else {
            for (ClassroomMember m : allMembers) {
                if (sec.equalsIgnoreCase(m.getSection())) {
                    sectionFilteredMembers.add(m);
                }
            }
        }

        binding.tvStudentCount.setText(sectionFilteredMembers.size() + " Students");
        adapter.updateList(sectionFilteredMembers);
        boolean empty = sectionFilteredMembers.isEmpty();
        binding.layoutEmptyStudents.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvStudents.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showAddStudentDialog(ClassroomMember editTarget) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(editTarget == null ? "Add Student Manually" : "Edit Student Info");

        View view = getLayoutInflater().inflate(R.layout.item_student, null); // custom layout view
        // Create programmatically simple dialog view
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText etName = new EditText(this);
        etName.setHint("Student Full Name");
        if (editTarget != null) etName.setText(editTarget.getStudentName());
        layout.addView(etName);

        EditText etRoll = new EditText(this);
        etRoll.setHint("Class Roll Number (e.g. 01)");
        if (editTarget != null) etRoll.setText(editTarget.getRollNumber());
        layout.addView(etRoll);

        EditText etId = new EditText(this);
        etId.setHint("Student ID (e.g. 2021-001)");
        if (editTarget != null) etId.setText(editTarget.getStudentIdNumber());
        layout.addView(etId);

        EditText etSection = new EditText(this);
        etSection.setHint("Section (e.g. Section A)");
        if (editTarget != null) etSection.setText(editTarget.getSection());
        else etSection.setText("Section A");
        layout.addView(etSection);

        builder.setView(layout);
        builder.setPositiveButton(editTarget == null ? "Add" : "Save", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String roll = etRoll.getText().toString().trim();
            String id = etId.getText().toString().trim();
            String sec = etSection.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Student name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (editTarget == null) {
                ClassroomMember m = new ClassroomMember();
                m.setClassroomId(classroomId);
                m.setStudentUid("manual_" + System.currentTimeMillis());
                m.setStudentName(name);
                m.setRollNumber(roll);
                m.setStudentIdNumber(id);
                m.setSection(sec);
                m.setClassRepresentative(false);

                classroomRepo.addStudentManually(m, new ClassroomRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(StudentManagementActivity.this, "Student added successfully!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(StudentManagementActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                editTarget.setStudentName(name);
                editTarget.setRollNumber(roll);
                editTarget.setStudentIdNumber(id);
                editTarget.setSection(sec);

                classroomRepo.updateStudentMember(editTarget, new ClassroomRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(StudentManagementActivity.this, "Student updated successfully!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(StudentManagementActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteConfirmDialog(ClassroomMember student) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Student")
                .setMessage("Are you sure you want to remove " + student.getStudentName() + " from this classroom?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    classroomRepo.removeStudentMember(student.getMemberId(), new ClassroomRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(StudentManagementActivity.this, "Student removed.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(StudentManagementActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStudentProfileDialog(ClassroomMember student) {
        new AlertDialog.Builder(this)
                .setTitle(student.getStudentName())
                .setMessage("Roll Number: " + (student.getRollNumber() != null ? student.getRollNumber() : "N/A") + "\n" +
                        "Student ID: " + (student.getStudentIdNumber() != null ? student.getStudentIdNumber() : "N/A") + "\n" +
                        "Section: " + (student.getSection() != null ? student.getSection() : "N/A") + "\n" +
                        "Role: " + (student.isClassRepresentative() ? "Classroom Representative (CR)" : "Regular Student"))
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (membersListener != null) membersListener.remove();
    }
}
