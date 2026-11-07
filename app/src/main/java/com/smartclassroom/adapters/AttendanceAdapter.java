
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

    public interface OnAttendanceChangedListener {
        void onAttendanceChanged(int total, int present, int absent);
    }

    private final List<ClassroomMember> students = new ArrayList<>();
    private final Map<String, String> attendanceMap = new HashMap<>();

    private final OnAttendanceChangedListener changeListener;

    public AttendanceAdapter(OnAttendanceChangedListener listener) {
        this.changeListener = listener;
    }
    public AttendanceAdapter() {
        this(null);
    }

    public void updateList(List<ClassroomMember> list) {

        students.clear();

        if (list != null) {
            students.addAll(list);

            for (ClassroomMember member : list) {

                String uid = member.getStudentUid();

                if (uid != null && !attendanceMap.containsKey(uid)) {
                    attendanceMap.put(uid, Constants.STATUS_PRESENT);
                }
            }
        }

        notifyDataSetChanged();
        notifySummaryChanged();
    }

    public void resetAllPresent() {

        for (ClassroomMember member : students) {
            String uid = member.getStudentUid();

            if (uid != null) {
                attendanceMap.put(uid, Constants.STATUS_PRESENT);
            }
        }

        notifyDataSetChanged();
        notifySummaryChanged();
    }

    public List<AttendanceRecord> getRecords(
            String classroomId,
            String section) {

        List<AttendanceRecord> records = new ArrayList<>();

        for (ClassroomMember member : students) {

            String uid = member.getStudentUid();

            String status = attendanceMap.getOrDefault(
                    uid,
                    Constants.STATUS_PRESENT
            );

            AttendanceRecord record = new AttendanceRecord();

            record.setClassroomId(classroomId);
            record.setStudentUid(uid);
            record.setStudentRoll(member.getRollNumber());
            record.setStudentName(member.getStudentName());
            record.setSection(section);
            record.setStatus(status);

            records.add(record);
        }

        return records;
    }

    public List<String> getAbsentRollNumbers() {

        List<String> absentRolls = new ArrayList<>();

        for (ClassroomMember member : students) {

            String uid = member.getStudentUid();

            String status = attendanceMap.getOrDefault(
                    uid,
                    Constants.STATUS_PRESENT
            );

            if (Constants.STATUS_ABSENT.equalsIgnoreCase(status)) {

                absentRolls.add(
                        member.getRollNumber() == null
                                ? "NO ROLL"
                                : member.getRollNumber()
                );
            }
        }

        return absentRolls;
    }

    private void notifySummaryChanged() {

        if (changeListener == null) return;

        int present = 0;
        int absent = 0;

        for (ClassroomMember member : students) {

            String uid = member.getStudentUid();

            String status = attendanceMap.getOrDefault(
                    uid,
                    Constants.STATUS_PRESENT
            );

            if (Constants.STATUS_ABSENT.equalsIgnoreCase(status)) {
                absent++;
            } else {
                present++;
            }
        }

        changeListener.onAttendanceChanged(
                students.size(),
                present,
                absent
        );
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        ItemAttendanceStudentBinding binding =
                ItemAttendanceStudentBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        ClassroomMember student = students.get(position);

        String uid = student.getStudentUid();

        String status = attendanceMap.getOrDefault(
                uid,
                Constants.STATUS_PRESENT
        );

        holder.bind(student, status, newStatus -> {

            if (uid != null) {
                attendanceMap.put(uid, newStatus);
            }

            int currentPosition = holder.getBindingAdapterPosition();

            if (currentPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(currentPosition);
            }

            notifySummaryChanged();
        });
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    interface StatusChangeListener {
        void onStatusChanged(String status);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemAttendanceStudentBinding binding;

        ViewHolder(ItemAttendanceStudentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                ClassroomMember student,
                String status,
                StatusChangeListener listener) {

            String roll = student.getRollNumber();

            binding.tvRoll.setText(
                    roll == null || roll.trim().isEmpty()
                            ? "NO ROLL"
                            : roll
            );

            binding.tvStudentName.setText(
                    student.getStudentName() == null
                            ? "Unnamed Student"
                            : student.getStudentName()
            );

            binding.tvStudentId.setText(
                    "ID: " +
                            (student.getStudentIdNumber() == null
                                    ? "N/A"
                                    : student.getStudentIdNumber())
                            + "  •  " +
                            (student.getSection() == null
                                    ? ""
                                    : student.getSection())
            );

            boolean absent =
                    Constants.STATUS_ABSENT.equalsIgnoreCase(status);

            if (absent) {

                binding.btnAttendanceStatus.setText("ABSENT");
                binding.btnAttendanceStatus.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                Color.parseColor("#DC2626")
                        )
                );

                binding.tvRoll.setBackgroundResource(
                        R.drawable.bg_status_absent
                );

                binding.tvRoll.setTextColor(
                        Color.parseColor("#DC2626")
                );

            } else {

                binding.btnAttendanceStatus.setText("PRESENT");

                binding.btnAttendanceStatus.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                Color.parseColor("#16A34A")
                        )
                );

                binding.tvRoll.setBackgroundResource(
                        R.drawable.bg_status_present
                );

                binding.tvRoll.setTextColor(
                        Color.parseColor("#16A34A")
                );
            }

            binding.btnAttendanceStatus.setOnClickListener(v -> {

                String newStatus = absent
                        ? Constants.STATUS_PRESENT
                        : Constants.STATUS_ABSENT;

                listener.onStatusChanged(newStatus);
            });
        }
    }
}
