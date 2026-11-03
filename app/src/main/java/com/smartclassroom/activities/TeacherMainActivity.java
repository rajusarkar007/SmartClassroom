package com.smartclassroom.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.smartclassroom.adapters.ClassroomAdapter;
import com.smartclassroom.adapters.JoinRequestAdapter;
import com.smartclassroom.databinding.ActivityTeacherMainBinding;
import com.smartclassroom.models.Classroom;
import com.smartclassroom.models.JoinRequest;
import com.smartclassroom.repositories.AuthRepository;
import com.smartclassroom.repositories.ClassroomRepository;
import com.smartclassroom.utils.Constants;
import com.smartclassroom.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class TeacherMainActivity extends AppCompatActivity {

    private ActivityTeacherMainBinding binding;
    private SessionManager sessionManager;
    private ClassroomRepository classroomRepo;
    private ClassroomAdapter classroomAdapter;

    private ListenerRegistration classroomsListener;
    private ListenerRegistration pendingRequestsListener;
    private final List<JoinRequest> pendingRequests = new ArrayList<>();
    private final List<Classroom> currentClassrooms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTeacherMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        classroomRepo = ClassroomRepository.getInstance();

        setupHeader();
        setupRecyclerView();
        setupModuleClicks();
        listenData();
    }

    private void setupHeader() {
        String name = sessionManager.getFullName();
        binding.tvWelcomeTeacher.setText("Welcome, " + (!name.isEmpty() ? name : "Professor"));
        binding.tvTeacherInstitution.setText(sessionManager.getDepartment() + " • " + sessionManager.getInstitution());

        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());
        binding.btnTeacherProfile.setOnClickListener(v -> {
            startActivity(new Intent(TeacherMainActivity.this, ProfileActivity.class));
        });

        binding.btnPendingRequests.setOnClickListener(v -> showPendingRequestsDialog());
        binding.cardPendingBanner.setOnClickListener(v -> showPendingRequestsDialog());
    }

    private void setupRecyclerView() {
        classroomAdapter = new ClassroomAdapter(true, classroom -> {
            Intent intent = new Intent(TeacherMainActivity.this, ClassroomDetailActivity.class);
            intent.putExtra(Constants.EXTRA_CLASSROOM_ID, classroom.getClassroomId());
            intent.putExtra(Constants.EXTRA_IS_TEACHER, true);
            startActivity(intent);
        });

        binding.rvClassrooms.setLayoutManager(new LinearLayoutManager(this));
        binding.rvClassrooms.setAdapter(classroomAdapter);
    }

    private void setupModuleClicks() {
        View.OnClickListener createAction = v -> {
            startActivity(new Intent(TeacherMainActivity.this, CreateClassroomActivity.class));
        };
        binding.fabCreateClassroom.setOnClickListener(createAction);
        binding.btnCreateClassroomText.setOnClickListener(createAction);

        binding.cardAttendance.setOnClickListener(v -> {
            if (currentClassrooms.isEmpty()) {
                Toast.makeText(this, "Please create a classroom first.", Toast.LENGTH_SHORT).show();
            } else {
                openModuleForFirstClassroom(AttendanceActivity.class);
            }
        });

        binding.cardMarks.setOnClickListener(v -> {
            if (currentClassrooms.isEmpty()) {
                Toast.makeText(this, "Please create a classroom first.", Toast.LENGTH_SHORT).show();
            } else {
                openModuleForFirstClassroom(MarksManagementActivity.class);
            }
        });

        binding.cardSchedule.setOnClickListener(v -> {
            if (currentClassrooms.isEmpty()) {
                Toast.makeText(this, "Please create a classroom first.", Toast.LENGTH_SHORT).show();
            } else {
                openModuleForFirstClassroom(ScheduleActivity.class);
            }
        });

        binding.cardNotices.setOnClickListener(v -> {
            if (currentClassrooms.isEmpty()) {
                Toast.makeText(this, "Please create a classroom first.", Toast.LENGTH_SHORT).show();
            } else {
                openModuleForFirstClassroom(NoticeActivity.class);
            }
        });

        binding.cardStudents.setOnClickListener(v -> {
            if (currentClassrooms.isEmpty()) {
                Toast.makeText(this, "Please create a classroom first.", Toast.LENGTH_SHORT).show();
            } else {
                openModuleForFirstClassroom(StudentManagementActivity.class);
            }
        });

        binding.cardReports.setOnClickListener(v -> {
            if (currentClassrooms.isEmpty()) {
                Toast.makeText(this, "Please create a classroom first.", Toast.LENGTH_SHORT).show();
            } else {
                openModuleForFirstClassroom(ReportsActivity.class);
            }
        });
    }

    private void openModuleForFirstClassroom(Class<?> targetActivity) {
        Classroom first = currentClassrooms.get(0);
        Intent intent = new Intent(TeacherMainActivity.this, targetActivity);
        intent.putExtra(Constants.EXTRA_CLASSROOM_ID, first.getClassroomId());
        intent.putExtra(Constants.EXTRA_CLASSROOM_NAME, first.getClassroomName());
        intent.putExtra(Constants.EXTRA_SECTION, first.getSection());
        intent.putExtra(Constants.EXTRA_IS_TEACHER, true);
        startActivity(intent);
    }

    private void listenData() {
        String uid = sessionManager.getUid();
        if (uid.isEmpty()) return;

        classroomsListener = classroomRepo.listenTeacherClassrooms(uid, new ClassroomRepository.Callback<List<Classroom>>() {
            @Override
            public void onSuccess(List<Classroom> result) {
                currentClassrooms.clear();
                if (result != null) currentClassrooms.addAll(result);

                classroomAdapter.updateList(result);
                boolean empty = result == null || result.isEmpty();
                binding.layoutEmptyClassrooms.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.rvClassrooms.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(TeacherMainActivity.this, "Error loading classrooms: " + message, Toast.LENGTH_SHORT).show();
            }
        });

        pendingRequestsListener = classroomRepo.listenPendingJoinRequests(uid, new ClassroomRepository.Callback<List<JoinRequest>>() {
            @Override
            public void onSuccess(List<JoinRequest> result) {
                pendingRequests.clear();
                if (result != null) pendingRequests.addAll(result);

                if (!pendingRequests.isEmpty()) {
                    binding.cardPendingBanner.setVisibility(View.VISIBLE);
                    binding.tvPendingCount.setText("🔔 " + pendingRequests.size() + " student join request(s) waiting for approval");
                } else {
                    binding.cardPendingBanner.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void showPendingRequestsDialog() {
        if (pendingRequests.isEmpty()) {
            Toast.makeText(this, "No pending join requests.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pending Student Join Requests");

        RecyclerView rv = new RecyclerView(this);
        rv.setLayoutManager(new LinearLayoutManager(this));

        JoinRequestAdapter adapter = new JoinRequestAdapter(new JoinRequestAdapter.RequestActionListener() {
            @Override
            public void onApprove(JoinRequest request) {
                promptRollNumberAndApprove(request);
            }

            @Override
            public void onReject(JoinRequest request) {
                classroomRepo.respondToJoinRequest(request, false, null, null, new ClassroomRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(TeacherMainActivity.this, "Request rejected.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(TeacherMainActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        adapter.updateList(pendingRequests);
        rv.setAdapter(adapter);
        builder.setView(rv);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void promptRollNumberAndApprove(JoinRequest request) {
        EditText inputRoll = new EditText(this);
        inputRoll.setHint("Assign Class Roll Number (e.g. 01)");
        inputRoll.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        inputRoll.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("Approve " + request.getStudentName())
                .setMessage("Assign roll number and confirm approval for section: " + request.getRequestedSection())
                .setView(inputRoll)
                .setPositiveButton("Approve", (dialog, which) -> {
                    String roll = inputRoll.getText().toString().trim();
                    classroomRepo.respondToJoinRequest(request, true, roll, request.getRequestedSection(),
                            new ClassroomRepository.Callback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    Toast.makeText(TeacherMainActivity.this, "Student approved and enrolled!", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(TeacherMainActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
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
                    Intent intent = new Intent(TeacherMainActivity.this, LoginActivity.class);
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
        if (classroomsListener != null) classroomsListener.remove();
        if (pendingRequestsListener != null) pendingRequestsListener.remove();
    }
}
