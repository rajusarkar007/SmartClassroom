package com.smartclassroom.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartclassroom.databinding.ItemScheduleBinding;
import com.smartclassroom.models.ClassSchedule;

import java.util.ArrayList;
import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    public interface ScheduleActionListener {
        void onToggleCancel(ClassSchedule schedule);
    }

    private final List<ClassSchedule> schedules = new ArrayList<>();
    private final boolean canManage; // Teacher or CR
    private final ScheduleActionListener listener;

    public ScheduleAdapter(boolean canManage, ScheduleActionListener listener) {
        this.canManage = canManage;
        this.listener = listener;
    }

    public void updateList(List<ClassSchedule> list) {
        schedules.clear();
        if (list != null) schedules.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemScheduleBinding binding = ItemScheduleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassSchedule schedule = schedules.get(position);
        holder.bind(schedule, canManage, listener);
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemScheduleBinding binding;

        public ViewHolder(ItemScheduleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ClassSchedule s, boolean canManage, ScheduleActionListener listener) {
            binding.tvScheduleTitle.setText(s.getTitle());
            binding.tvDateTime.setText(String.format("📅 %s • %s - %s", s.getDate(), s.getStartTime(), s.getEndTime()));
            binding.tvRoom.setText("Room: " + (s.getRoomNumber() != null ? s.getRoomNumber() : "TBD"));
            binding.tvCreatedBy.setText(String.format("Scheduled by: %s • %s",
                    s.getCreatedByName() != null ? s.getCreatedByName() : "Staff",
                    s.getSection() != null ? s.getSection() : "All"));

            binding.tvCancelledBadge.setVisibility(s.isCancelled() ? View.VISIBLE : View.GONE);
            binding.tvExtraBadge.setVisibility(s.isExtraClass() ? View.VISIBLE : View.GONE);

            if (canManage) {
                binding.btnCancelToggle.setVisibility(View.VISIBLE);
                binding.btnCancelToggle.setText(s.isCancelled() ? "Resume Class" : "Cancel Class");
                binding.btnCancelToggle.setOnClickListener(v -> {
                    if (listener != null) listener.onToggleCancel(s);
                });
            } else {
                binding.btnCancelToggle.setVisibility(View.GONE);
            }
        }
    }
}
