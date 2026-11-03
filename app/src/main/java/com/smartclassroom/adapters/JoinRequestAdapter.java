package com.smartclassroom.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartclassroom.databinding.ItemJoinRequestBinding;
import com.smartclassroom.models.JoinRequest;

import java.util.ArrayList;
import java.util.List;

public class JoinRequestAdapter extends RecyclerView.Adapter<JoinRequestAdapter.ViewHolder> {

    public interface RequestActionListener {
        void onApprove(JoinRequest request);
        void onReject(JoinRequest request);
    }

    private final List<JoinRequest> requests = new ArrayList<>();
    private final RequestActionListener listener;

    public JoinRequestAdapter(RequestActionListener listener) {
        this.listener = listener;
    }

    public void updateList(List<JoinRequest> list) {
        requests.clear();
        if (list != null) requests.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemJoinRequestBinding binding = ItemJoinRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JoinRequest req = requests.get(position);
        holder.bind(req, listener);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemJoinRequestBinding binding;

        public ViewHolder(ItemJoinRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(JoinRequest r, RequestActionListener listener) {
            binding.tvStudentName.setText(r.getStudentName());
            binding.tvStudentDetails.setText(String.format("Student ID: %s • Section: %s",
                    r.getStudentIdNumber() != null ? r.getStudentIdNumber() : "N/A",
                    r.getRequestedSection() != null ? r.getRequestedSection() : "Section A"));
            binding.tvClassroomName.setText(r.getClassroomName());

            binding.btnApprove.setOnClickListener(v -> {
                if (listener != null) listener.onApprove(r);
            });
            binding.btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onReject(r);
            });
        }
    }
}
