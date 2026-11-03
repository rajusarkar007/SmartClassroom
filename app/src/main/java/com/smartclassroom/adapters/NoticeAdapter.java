package com.smartclassroom.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartclassroom.databinding.ItemNoticeBinding;
import com.smartclassroom.models.Notice;
import com.smartclassroom.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.ViewHolder> {

    public interface NoticeActionListener {
        void onDeleteNotice(Notice notice);
    }

    private final List<Notice> notices = new ArrayList<>();
    private final String currentUserId;
    private final boolean isTeacher;
    private final NoticeActionListener listener;

    public NoticeAdapter(String currentUserId, boolean isTeacher, NoticeActionListener listener) {
        this.currentUserId = currentUserId;
        this.isTeacher = isTeacher;
        this.listener = listener;
    }

    public void updateList(List<Notice> list) {
        notices.clear();
        if (list != null) notices.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoticeBinding binding = ItemNoticeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notice notice = notices.get(position);
        holder.bind(notice, currentUserId, isTeacher, listener);
    }

    @Override
    public int getItemCount() {
        return notices.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNoticeBinding binding;

        public ViewHolder(ItemNoticeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Notice n, String currentUserId, boolean isTeacher, NoticeActionListener listener) {
            binding.tvNoticeCategory.setText(n.getCategory() != null ? n.getCategory() : "General");
            binding.tvNoticeDate.setText(DateUtils.formatDateTime(n.getCreatedAt()));
            binding.tvNoticeTitle.setText(n.getTitle());
            binding.tvNoticeContent.setText(n.getContent());
            binding.tvNoticeAuthor.setText(String.format("Posted by: %s (%s)",
                    n.getAuthorName() != null ? n.getAuthorName() : "Faculty",
                    n.getAuthorRole() != null ? n.getAuthorRole() : "Member"));

            boolean canDelete = isTeacher || (currentUserId != null && currentUserId.equals(n.getAuthorUid()));
            if (canDelete) {
                binding.btnDeleteNotice.setVisibility(View.VISIBLE);
                binding.btnDeleteNotice.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteNotice(n);
                });
            } else {
                binding.btnDeleteNotice.setVisibility(View.GONE);
            }
        }
    }
}
