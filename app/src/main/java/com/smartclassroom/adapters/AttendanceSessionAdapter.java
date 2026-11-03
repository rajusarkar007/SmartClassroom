package com.smartclassroom.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartclassroom.databinding.ItemAttendanceSessionBinding;
import com.smartclassroom.models.AttendanceSession;

import java.util.ArrayList;
import java.util.List;

public class AttendanceSessionAdapter extends RecyclerView.Adapter<AttendanceSessionAdapter.ViewHolder> {

    public interface SessionClickListener {
        void onSessionClick(AttendanceSession session);
    }

    private final List<AttendanceSession> sessions = new ArrayList<>();
    private final SessionClickListener listener;

    public AttendanceSessionAdapter(SessionClickListener listener) {
        this.listener = listener;
    }

    public void updateList(List<AttendanceSession> list) {
        sessions.clear();
        if (list != null) sessions.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAttendanceSessionBinding binding = ItemAttendanceSessionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceSession session = sessions.get(position);
        holder.bind(session, listener);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAttendanceSessionBinding binding;

        public ViewHolder(ItemAttendanceSessionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(AttendanceSession session, SessionClickListener listener) {
            binding.tvClassNumber.setText(String.format("Class #%02d", session.getClassNumber()));
            binding.tvDate.setText(session.getDate());
            binding.tvTopic.setText(session.getTopic() != null && !session.getTopic().isEmpty()
                    ? "Topic: " + session.getTopic() : "Topic: Regular Class Session");
            binding.tvPresentBadge.setText(String.format("Present: %d", session.getTotalPresent()));
            binding.tvLateBadge.setText(String.format("Late: %d", session.getTotalLate()));
            binding.tvAbsentBadge.setText(String.format("Absent: %d", session.getTotalAbsent()));
            binding.tvSection.setText(session.getSection() != null ? session.getSection() : "Section A");

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onSessionClick(session);
            });
        }
    }
}
