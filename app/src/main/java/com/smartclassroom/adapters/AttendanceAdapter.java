package com.smartclassroom.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartclassroom.R;
import com.smartclassroom.databinding.ItemAttendanceStudentBinding;
import com.smartclassroom.models.AttendanceRecord;
import com.smartclassroom.models.ClassroomMember;
import com.smartclassroom.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private final List<ClassroomMember> students = new ArrayList<>();
    private final Map<String, String> attendanceMap = new HashMap<>();

    public void updateList(List<ClassroomMember> list) {
        students.clear();
        if (list != null) {
            students.addAll(list);
            for (ClassroomMember m : list) {
                if (!attendanceMap.containsKey(m.getStudentUid())) {
                    attendanceMap.put(m.getStudentUid(), Constants.STATUS_PRESENT); // Default to Present
                }
            }
        }
        notifyDataSetChanged();
    }

    public void markAll(String status) {
        for (ClassroomMember m : students) {
            attendanceMap.put(m.getStudentUid(), status);
        }
        notifyDataSetChanged();
    }

    public List<AttendanceRecord> getRecords(String classroomId, String section) {
        List<AttendanceRecord> records = new ArrayList<>();
        for (ClassroomMember m : students) {
            String status = attendanceMap.getOrDefault(m.getStudentUid(), Constants.STATUS_PRESENT);
            AttendanceRecord r = new AttendanceRecord();
            r.setClassroomId(classroomId);
            r.setStudentUid(m.getStudentUid());
            r.setStudentRoll(m.getRollNumber());
            r.setStudentName(m.getStudentName());
            r.setSection(m.getSection() != null ? m.getSection() : section);
            r.setStatus(status);
            records.add(r);
        }
        return records;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAttendanceStudentBinding binding = ItemAttendanceStudentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassroomMember student = students.get(position);
        String currentStatus = attendanceMap.getOrDefault(student.getStudentUid(), Constants.STATUS_PRESENT);
        holder.bind(student, currentStatus, (uid, newStatus) -> {
            attendanceMap.put(uid, newStatus);
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    interface StatusChangeListener {
        void onStatusChanged(String studentUid, String newStatus);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAttendanceStudentBinding binding;

        public ViewHolder(ItemAttendanceStudentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ClassroomMember student, String currentStatus, StatusChangeListener listener) {
            binding.tvRoll.setText(student.getRollNumber() != null ? student.getRollNumber() : "•");
            binding.tvStudentName.setText(student.getStudentName());
            binding.tvStudentId.setText(String.format("ID: %s • %s",
                    student.getStudentIdNumber() != null ? student.getStudentIdNumber() : "N/A",
                    student.getSection() != null ? student.getSection() : ""));

            // Clear checked listener to prevent recycling trigger
            binding.toggleAttendanceGroup.clearOnButtonCheckedListeners();

            // Set button styles based on currentStatus
            if (Constants.STATUS_PRESENT.equalsIgnoreCase(currentStatus)) {
                binding.btnPresent.setBackgroundColor(Color.parseColor("#16A34A"));
                binding.btnPresent.setTextColor(Color.WHITE);
                binding.btnLate.setBackgroundColor(Color.TRANSPARENT);
                binding.btnLate.setTextColor(Color.parseColor("#D97706"));
                binding.btnAbsent.setBackgroundColor(Color.TRANSPARENT);
                binding.btnAbsent.setTextColor(Color.parseColor("#DC2626"));
                binding.toggleAttendanceGroup.check(R.id.btnPresent);
            } else if (Constants.STATUS_LATE.equalsIgnoreCase(currentStatus)) {
                binding.btnPresent.setBackgroundColor(Color.TRANSPARENT);
                binding.btnPresent.setTextColor(Color.parseColor("#16A34A"));
                binding.btnLate.setBackgroundColor(Color.parseColor("#D97706"));
                binding.btnLate.setTextColor(Color.WHITE);
                binding.btnAbsent.setBackgroundColor(Color.TRANSPARENT);
                binding.btnAbsent.setTextColor(Color.parseColor("#DC2626"));
                binding.toggleAttendanceGroup.check(R.id.btnLate);
            } else {
                binding.btnPresent.setBackgroundColor(Color.TRANSPARENT);
                binding.btnPresent.setTextColor(Color.parseColor("#16A34A"));
                binding.btnLate.setBackgroundColor(Color.TRANSPARENT);
                binding.btnLate.setTextColor(Color.parseColor("#D97706"));
                binding.btnAbsent.setBackgroundColor(Color.parseColor("#DC2626"));
                binding.btnAbsent.setTextColor(Color.WHITE);
                binding.toggleAttendanceGroup.check(R.id.btnAbsent);
            }

            binding.btnPresent.setOnClickListener(v -> listener.onStatusChanged(student.getStudentUid(), Constants.STATUS_PRESENT));
            binding.btnLate.setOnClickListener(v -> listener.onStatusChanged(student.getStudentUid(), Constants.STATUS_LATE));
            binding.btnAbsent.setOnClickListener(v -> listener.onStatusChanged(student.getStudentUid(), Constants.STATUS_ABSENT));
        }
    }
}
