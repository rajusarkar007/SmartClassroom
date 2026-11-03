package com.smartclassroom.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartclassroom.R;
import com.smartclassroom.databinding.ItemStudentBinding;
import com.smartclassroom.models.ClassroomMember;

import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    public interface StudentActionListener {
        void onEdit(ClassroomMember student);
        void onDelete(ClassroomMember student);
        void onToggleCr(ClassroomMember student);
        void onStudentClick(ClassroomMember student);
    }

    private final List<ClassroomMember> fullList = new ArrayList<>();
    private final List<ClassroomMember> displayedList = new ArrayList<>();
    private final boolean canManage;
    private final StudentActionListener listener;

    public StudentAdapter(boolean canManage, StudentActionListener listener) {
        this.canManage = canManage;
        this.listener = listener;
    }

    public void updateList(List<ClassroomMember> list) {
        fullList.clear();
        displayedList.clear();
        if (list != null) {
            fullList.addAll(list);
            displayedList.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void filter(String query) {
        displayedList.clear();
        if (query == null || query.trim().isEmpty()) {
            displayedList.addAll(fullList);
        } else {
            String lower = query.toLowerCase().trim();
            for (ClassroomMember m : fullList) {
                boolean matchName = m.getStudentName() != null && m.getStudentName().toLowerCase().contains(lower);
                boolean matchRoll = m.getRollNumber() != null && m.getRollNumber().toLowerCase().contains(lower);
                boolean matchId = m.getStudentIdNumber() != null && m.getStudentIdNumber().toLowerCase().contains(lower);
                if (matchName || matchRoll || matchId) {
                    displayedList.add(m);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentBinding binding = ItemStudentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassroomMember student = displayedList.get(position);
        holder.bind(student, canManage, listener);
    }

    @Override
    public int getItemCount() {
        return displayedList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentBinding binding;

        public ViewHolder(ItemStudentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ClassroomMember student, boolean canManage, StudentActionListener listener) {
            binding.tvRoll.setText(student.getRollNumber() != null && !student.getRollNumber().isEmpty() ? student.getRollNumber() : "•");
            binding.tvStudentName.setText(student.getStudentName());
            binding.tvStudentSubInfo.setText(String.format("ID: %s • %s",
                    student.getStudentIdNumber() != null ? student.getStudentIdNumber() : "N/A",
                    student.getSection() != null ? student.getSection() : "No Section"));

            if (student.isClassRepresentative()) {
                binding.tvCrBadge.setVisibility(View.VISIBLE);
                binding.btnToggleCr.setImageResource(R.drawable.ic_check);
            } else {
                binding.tvCrBadge.setVisibility(View.GONE);
                binding.btnToggleCr.setImageResource(R.drawable.ic_person);
            }

            if (canManage) {
                binding.btnToggleCr.setVisibility(View.VISIBLE);
                binding.btnEdit.setVisibility(View.VISIBLE);
                binding.btnDelete.setVisibility(View.VISIBLE);

                binding.btnToggleCr.setOnClickListener(v -> {
                    if (listener != null) listener.onToggleCr(student);
                });
                binding.btnEdit.setOnClickListener(v -> {
                    if (listener != null) listener.onEdit(student);
                });
                binding.btnDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onDelete(student);
                });
            } else {
                binding.btnToggleCr.setVisibility(View.GONE);
                binding.btnEdit.setVisibility(View.GONE);
                binding.btnDelete.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onStudentClick(student);
            });
        }
    }
}
