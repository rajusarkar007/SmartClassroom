
package com.smartclassroom.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartclassroom.databinding.ItemClassroomBinding;
import com.smartclassroom.models.Classroom;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClassroomAdapter
        extends RecyclerView.Adapter<ClassroomAdapter.ViewHolder> {

    public interface OnClassroomClickListener {
        void onClassroomClick(Classroom classroom);
    }

    public interface OnDeleteClassroomListener {
        void onDeleteClassroom(Classroom classroom);
    }

    private final List<Classroom> classrooms = new ArrayList<>();
    private final boolean isTeacher;
    private final OnClassroomClickListener listener;

    private OnDeleteClassroomListener deleteListener;

    public ClassroomAdapter(
            boolean isTeacher,
            OnClassroomClickListener listener
    ) {
        this.isTeacher = isTeacher;
        this.listener = listener;
    }

    public void setOnDeleteClassroomListener(
            OnDeleteClassroomListener listener
    ) {
        this.deleteListener = listener;
    }

    public void updateList(List<Classroom> list) {
        classrooms.clear();

        if (list != null) {
            classrooms.addAll(list);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemClassroomBinding binding =
                ItemClassroomBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        holder.bind(
                classrooms.get(position),
                isTeacher,
                listener,
                deleteListener
        );
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

        public void bind(
                Classroom classroom,
                boolean isTeacher,
                OnClassroomClickListener listener,
                OnDeleteClassroomListener deleteListener
        ) {
            binding.tvCourseName.setText(classroom.getCourseName());

            binding.tvCourseCode.setText(
                    String.format(
                            Locale.getDefault(),
                            "%s • %.1f Credits",
                            classroom.getCourseCode(),
                            classroom.getCourseCredit()
                    )
            );

            binding.tvSectionBadge.setText(
                    classroom.getSection() != null
                            ? classroom.getSection()
                            : "Section A"
            );

            binding.tvDepartment.setText(
                    String.format(
                            Locale.getDefault(),
                            "Dept: %s • %s",
                            classroom.getDepartment(),
                            classroom.getSemester()
                    )
            );

            binding.tvClassroomCode.setText(
                    String.format(
                            Locale.getDefault(),
                            "Code: %s",
                            classroom.getClassroomCode()
                    )
            );

            if (isTeacher) {
                binding.tvTeacherOrStudents.setText(
                        String.format(
                                Locale.getDefault(),
                                "Planned: %d Classes",
                                classroom.getTotalPlannedClasses()
                        )
                );

                binding.tvStudentCount.setText(
                        String.format(
                                Locale.getDefault(),
                                "%d Enrolled",
                                classroom.getTotalStudents()
                        )
                );

                binding.btnDeleteClassroom.setVisibility(View.VISIBLE);

                binding.btnDeleteClassroom.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onDeleteClassroom(classroom);
                    }
                });

            } else {
                binding.tvTeacherOrStudents.setText(
                        String.format(
                                Locale.getDefault(),
                                "Teacher: %s",
                                classroom.getTeacherName()
                        )
                );

                binding.tvStudentCount.setText(
                        classroom.getClassroomName()
                );

                binding.btnDeleteClassroom.setVisibility(View.GONE);
                binding.btnDeleteClassroom.setOnClickListener(null);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClassroomClick(classroom);
                }
            });
        }
    }
}
