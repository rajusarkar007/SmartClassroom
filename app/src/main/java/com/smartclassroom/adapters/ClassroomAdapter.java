package com.smartclassroom.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartclassroom.databinding.ItemClassroomBinding;
import com.smartclassroom.models.Classroom;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClassroomAdapter extends RecyclerView.Adapter<ClassroomAdapter.ViewHolder> {

    public interface OnClassroomClickListener {
        void onClassroomClick(Classroom classroom);
    }

    private final List<Classroom> classrooms = new ArrayList<>();
    private final boolean isTeacher;
    private final OnClassroomClickListener listener;

    public ClassroomAdapter(boolean isTeacher, OnClassroomClickListener listener) {
        this.isTeacher = isTeacher;
        this.listener = listener;
    }

    public void updateList(List<Classroom> list) {
        classrooms.clear();
        if (list != null) classrooms.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemClassroomBinding binding = ItemClassroomBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Classroom c = classrooms.get(position);
        holder.bind(c, isTeacher, listener);
    }

    @Override
    public int getItemCount() {
        return classrooms.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemClassroomBinding binding;

        public ViewHolder(ItemClassroomBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Classroom c, boolean isTeacher, OnClassroomClickListener listener) {
            binding.tvCourseName.setText(c.getCourseName());
            binding.tvCourseCode.setText(String.format(Locale.getDefault(), "%s • %.1f Credits", c.getCourseCode(), c.getCourseCredit()));
            binding.tvSectionBadge.setText(c.getSection() != null ? c.getSection() : "Section A");
            binding.tvDepartment.setText(String.format("Dept: %s • %s", c.getDepartment(), c.getSemester()));
            binding.tvClassroomCode.setText(String.format("Code: %s", c.getClassroomCode()));

            if (isTeacher) {
                binding.tvTeacherOrStudents.setText(String.format(Locale.getDefault(), "Planned: %d Classes", c.getTotalPlannedClasses()));
                binding.tvStudentCount.setText(String.format(Locale.getDefault(), "%d Enrolled", c.getTotalStudents()));
            } else {
                binding.tvTeacherOrStudents.setText(String.format("Teacher: %s", c.getTeacherName()));
                binding.tvStudentCount.setText(c.getClassroomName());
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClassroomClick(c);
            });
        }
    }
}
