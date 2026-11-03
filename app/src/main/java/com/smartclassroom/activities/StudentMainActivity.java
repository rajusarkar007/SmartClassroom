package com.smartclassroom.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.ListenerRegistration;
import com.smartclassroom.R;
import com.smartclassroom.adapters.ClassroomAdapter;
import com.smartclassroom.databinding.ActivityStudentMainBinding;
import com.smartclassroom.models.Classroom;
import com.smartclassroom.models.ClassroomMember;
import com.smartclassroom.models.JoinRequest;
import com.smartclassroom.repositories.AuthRepository;
import com.smartclassroom.repositories.ClassroomRepository;
import com.smartclassroom.utils.Constants;
import com.smartclassroom.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentMainActivity extends AppCompatActivity {

    private ActivityStudentMainBinding binding;
    private SessionManager sessionManager;
    private ClassroomRepository classroomRepo;
    private ClassroomAdapter classroomAdapter;

    private ListenerRegistration membershipsListener;
    private final Map<String, ClassroomMember> memberMap = new HashMap<>();
    private final List<Classroom> classroomsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        classroomRepo = ClassroomRepository.getInstance();

        setupHeader();
        setupRecyclerView();
        setupModuleClicks();
        listenStudentData();
    }

    private void setupHeader() {
        String name = sessionManager.getFullName();
        binding.tvWelcomeStudent.setText("Welcome, " + (!name.isEmpty() ? name : "Student"));
        binding.tvStudentSubInfo.setText(String.format("ID: %s • %s • %s",
                sessionManager.getIdentifier(),
                sessionManager.getDepartment(),
                sessionManager.getInstitution()));

        binding.btnStudentLogout.setOnClickListener(v -> showLogoutDialog());
        binding.btnStudentProfile.setOnClickListener(v -> {
            startActivity(new Intent(StudentMainActivity.this, ProfileActivity.class));
        });
    }

    private void setupRecyclerView() {
        classroomAdapter = new ClassroomAdapter(false, classroom -> {
            ClassroomMember member = memberMap.get(classroom.getClassroomId());
            Intent intent = new Intent(StudentMainActivity.this, ClassroomDetailActivity.class);
            intent.putExtra(Constants.EXTRA_CLASSROOM_ID, classroom.getClassroomId());
            intent.putExtra(Constants.EXTRA_IS_TEACHER, false);
            intent.putExtra(Constants.EXTRA_IS_CR, member != null && member.isClassRepresentative());
            intent.putExtra(Constants.EXTRA_SECTION, member != null ? member.getSection() : classroom.getSection());
            startActivity(intent);
        });

        binding.rvStudentClassrooms.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStudentClassrooms.setAdapter(classroomAdapter);
    }

    private void setupModuleClicks() {
        View.OnClickListener joinAction = v -> showJoinClassroomDialog();
        binding.fabJoinClassroom.setOnClickListener(joinAction);
        binding.btnJoinClassroomText.setOnClickListener(joinAction);
        binding.cardStudentJoin.setOnClickListener(joinAction);

        binding.cardStudentAttendance.setOnClickListener(v -> {
            if (classroomsList.isEmpty()) {
                Toast.makeText(this, "You have not joined any classrooms yet.", Toast.LENGTH_SHORT).show();
            } else {
                openModuleForFirst(AttendanceHistoryActivity.class);
            }
        });

        binding.cardStudentResults.setOnClickListener(v -> {
            if (classroomsList.isEmpty()) {
                Toast.makeText(this, "You have not joined any classrooms yet.", Toast.LENGTH_SHORT).show();
            } else {
                openModuleForFirst(MarksManagementActivity.class);
            }
        });

        binding.cardStudentSchedule.setOnClickListener(v -> {
            if (classroomsList.isEmpty()) {
                Toast.makeText(this, "You have not joined any classrooms yet.", Toast.LENGTH_SHORT).show();
            } else {
                openModuleForFirst(ScheduleActivity.class);
            }
        });

        binding.cardStudentNotices.setOnClickListener(v -> {
            if (classroomsList.isEmpty()) {
                Toast.makeText(this, "You have not joined any classrooms yet.", Toast.LENGTH_SHORT).show();
            } else {
                openModuleForFirst(NoticeActivity.class);
            }
        });

        binding.cardStudentNotes.setOnClickListener(v -> {
            startActivity(new Intent(StudentMainActivity.this, PersonalNotesActivity.class));
        });
    }

    private void openModuleForFirst(Class<?> target) {
        Classroom first = classroomsList.get(0);
        ClassroomMember member = memberMap.get(first.getClassroomId());
        Intent intent = new Intent(StudentMainActivity.this, target);
        intent.putExtra(Constants.EXTRA_CLASSROOM_ID, first.getClassroomId());
        intent.putExtra(Constants.EXTRA_CLASSROOM_NAME, first.getClassroomName());
        intent.putExtra(Constants.EXTRA_SECTION, member != null ? member.getSection() : first.getSection());
        intent.putExtra(Constants.EXTRA_IS_TEACHER, false);
        intent.putExtra(Constants.EXTRA_IS_CR, member != null && member.isClassRepresentative());
        startActivity(intent);
    }

    private void listenStudentData() {
        String uid = sessionManager.getUid();
        if (uid.isEmpty()) return;

        membershipsListener = classroomRepo.listenStudentMemberships(uid, new ClassroomRepository.Callback<List<ClassroomMember>>() {
            @Override
            public void onSuccess(List<ClassroomMember> members) {
                memberMap.clear();
                classroomsList.clear();

                if (members == null || members.isEmpty()) {
                    classroomAdapter.updateList(classroomsList);
                    binding.layoutEmptyStudentClassrooms.setVisibility(View.VISIBLE);
                    binding.rvStudentClassrooms.setVisibility(View.GONE);
                    return;
                }

                for (ClassroomMember m : members) {
                    memberMap.put(m.getClassroomId(), m);
                    // Fetch classroom details
                    classroomRepo.getClassroomById(m.getClassroomId(), new ClassroomRepository.Callback<Classroom>() {
                        @Override
                        public void onSuccess(Classroom classroom) {
                            if (!classroomsList.contains(classroom)) {
                                classroomsList.add(classroom);
                                classroomAdapter.updateList(classroomsList);
                                binding.layoutEmptyStudentClassrooms.setVisibility(View.GONE);
                                binding.rvStudentClassrooms.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onError(String message) {}
                    });
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(StudentMainActivity.this, "Error loading enrolled classes: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showJoinClassroomDialog() {
        EditText inputCode = new EditText(this);
        inputCode.setHint("Enter 6-character Code (e.g. SC8K2M)");
        inputCode.setInputType(android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        inputCode.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("Join Classroom")
                .setMessage("Enter the classroom code provided by your instructor:")
                .setView(inputCode)
                .setPositiveButton("Verify & Join", (dialog, which) -> {
                    String code = inputCode.getText().toString().trim().toUpperCase();
                    if (code.length() < 5) {
                        Toast.makeText(this, "Please enter a valid classroom code", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    classroomRepo.getClassroomByCode(code, new ClassroomRepository.Callback<Classroom>() {
                        @Override
                        public void onSuccess(Classroom classroom) {
                            showConfirmJoinDialog(classroom);
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(StudentMainActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showConfirmJoinDialog(Classroom classroom) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Classroom Information")
                .setMessage("Course: " + classroom.getCourseName() + " (" + classroom.getCourseCode() + ")\n" +
                        "Instructor: " + classroom.getTeacherName() + "\n" +
                        "Department: " + classroom.getDepartment() + "\n" +
                        "Section: " + classroom.getSection() + "\n\n" +
                        "Would you like to send a join request to the teacher?")
                .setPositiveButton("Send Request", (dialog, which) -> {
                    JoinRequest req = new JoinRequest();
                    req.setClassroomId(classroom.getClassroomId());
                    req.setClassroomCode(classroom.getClassroomCode());
                    req.setClassroomName(classroom.getClassroomName());
                    req.setTeacherId(classroom.getTeacherId());
                    req.setStudentUid(sessionManager.getUid());
                    req.setStudentName(sessionManager.getFullName());
                    req.setStudentIdNumber(sessionManager.getIdentifier());
                    req.setRequestedSection(classroom.getSection());

                    classroomRepo.requestToJoinClassroom(req, new ClassroomRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(StudentMainActivity.this, "Join request submitted! The classroom will appear once approved by the instructor.", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(StudentMainActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out of Smart Classroom?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    sessionManager.clearSession();
                    AuthRepository.getInstance().logout();
                    Intent intent = new Intent(StudentMainActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (membershipsListener != null) membershipsListener.remove();
    }
}
