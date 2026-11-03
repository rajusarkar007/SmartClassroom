package com.smartclassroom.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartclassroom.databinding.ItemAssessmentBinding;
import com.smartclassroom.models.Assessment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AssessmentAdapter extends RecyclerView.Adapter<AssessmentAdapter.ViewHolder> {

    public interface AssessmentClickListener {
        void onEvaluate(Assessment assessment);
    }

    private final List<Assessment> assessments = new ArrayList<>();
    private final boolean isTeacher;
    private final AssessmentClickListener listener;

    public AssessmentAdapter(boolean isTeacher, AssessmentClickListener listener) {
        this.isTeacher = isTeacher;
        this.listener = listener;
    }

    public void updateList(List<Assessment> list) {
        assessments.clear();
        if (list != null) assessments.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAssessmentBinding binding = ItemAssessmentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Assessment assessment = assessments.get(position);
        holder.bind(assessment, isTeacher, listener);
    }

    @Override
    public int getItemCount() {
        return assessments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAssessmentBinding binding;

        public ViewHolder(ItemAssessmentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Assessment a, boolean isTeacher, AssessmentClickListener listener) {
            binding.tvAssessmentTitle.setText(a.getTitle());
            binding.tvCategoryDate.setText(String.format("%s • %s • %s",
                    a.getCategory(),
                    a.getDate() != null ? a.getDate() : "",
                    a.getSection() != null ? a.getSection() : "All Sections"));
            binding.tvMaxMarks.setText(String.format(Locale.getDefault(), "Max: %.1f", a.getMaxMarks()));
            binding.tvRemarks.setText(a.getRemarks() != null && !a.getRemarks().isEmpty() ? a.getRemarks() : "No remarks provided");

            if (isTeacher) {
                binding.btnEvaluate.setVisibility(View.VISIBLE);
                binding.btnEvaluate.setOnClickListener(v -> {
                    if (listener != null) listener.onEvaluate(a);
                });
            } else {
                binding.btnEvaluate.setVisibility(View.GONE);
            }
        }
    }
}
