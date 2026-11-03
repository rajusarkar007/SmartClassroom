package com.smartclassroom.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartclassroom.databinding.ItemStudentMarkEntryBinding;
import com.smartclassroom.models.ClassroomMember;
import com.smartclassroom.models.StudentMark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentMarksAdapter extends RecyclerView.Adapter<StudentMarksAdapter.ViewHolder> {

    private final List<ClassroomMember> students = new ArrayList<>();
    private final Map<String, StudentMark> marksMap = new HashMap<>();
    private final double maxMarks;
    private final String assessmentId;
    private final String classroomId;
    private final String section;

    public StudentMarksAdapter(String classroomId, String assessmentId, String section, double maxMarks) {
        this.classroomId = classroomId;
        this.assessmentId = assessmentId;
        this.section = section;
        this.maxMarks = maxMarks;
    }

    public void setStudentsAndMarks(List<ClassroomMember> studentList, List<StudentMark> existingMarks) {
        students.clear();
        marksMap.clear();

        if (existingMarks != null) {
            for (StudentMark m : existingMarks) {
                marksMap.put(m.getStudentUid(), m);
            }
        }

        if (studentList != null) {
            students.addAll(studentList);
            for (ClassroomMember s : studentList) {
                if (!marksMap.containsKey(s.getStudentUid())) {
                    StudentMark newMark = new StudentMark();
                    newMark.setClassroomId(classroomId);
                    newMark.setAssessmentId(assessmentId);
                    newMark.setStudentUid(s.getStudentUid());
                    newMark.setStudentName(s.getStudentName());
                    newMark.setStudentRoll(s.getRollNumber());
                    newMark.setSection(s.getSection() != null ? s.getSection() : section);
                    newMark.setMaxMarks(maxMarks);
                    newMark.setObtainedMarks(0.0);
                    marksMap.put(s.getStudentUid(), newMark);
                }
            }
        }
        notifyDataSetChanged();
    }

    public List<StudentMark> getUpdatedMarks() {
        return new ArrayList<>(marksMap.values());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentMarkEntryBinding binding = ItemStudentMarkEntryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassroomMember student = students.get(position);
        StudentMark mark = marksMap.get(student.getStudentUid());
        holder.bind(student, mark, maxMarks);
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentMarkEntryBinding binding;
        private TextWatcher currentWatcher;

        public ViewHolder(ItemStudentMarkEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ClassroomMember student, StudentMark mark, double maxMarks) {
            binding.tvRoll.setText(student.getRollNumber() != null ? student.getRollNumber() : "•");
            binding.tvStudentName.setText(student.getStudentName());
            binding.tvStudentId.setText(String.format("ID: %s • Max: %.1f",
                    student.getStudentIdNumber() != null ? student.getStudentIdNumber() : "N/A", maxMarks));

            if (currentWatcher != null) {
                binding.etObtainedMarks.removeTextChangedListener(currentWatcher);
            }

            if (mark != null && mark.getObtainedMarks() > 0) {
                binding.etObtainedMarks.setText(String.valueOf(mark.getObtainedMarks()));
            } else {
                binding.etObtainedMarks.setText("");
            }

            currentWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (mark != null) {
                        try {
                            String val = s.toString().trim();
                            if (!val.isEmpty()) {
                                double obt = Double.parseDouble(val);
                                if (obt > maxMarks) {
                                    binding.tilObtainedMarks.setError("Max " + maxMarks);
                                    mark.setObtainedMarks(maxMarks);
                                } else if (obt < 0) {
                                    binding.tilObtainedMarks.setError("Min 0");
                                    mark.setObtainedMarks(0);
                                } else {
                                    binding.tilObtainedMarks.setError(null);
                                    mark.setObtainedMarks(obt);
                                }
                            } else {
                                binding.tilObtainedMarks.setError(null);
                                mark.setObtainedMarks(0.0);
                            }
                        } catch (NumberFormatException e) {
                            binding.tilObtainedMarks.setError("Invalid");
                        }
                    }
                }
            };

            binding.etObtainedMarks.addTextChangedListener(currentWatcher);
        }
    }
}
