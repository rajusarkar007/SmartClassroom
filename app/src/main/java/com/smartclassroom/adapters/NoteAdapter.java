package com.smartclassroom.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartclassroom.databinding.ItemNoteBinding;
import com.smartclassroom.models.PersonalNote;
import com.smartclassroom.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {

    public interface NoteActionListener {
        void onEditNote(PersonalNote note);
        void onDeleteNote(PersonalNote note);
    }

    private final List<PersonalNote> notes = new ArrayList<>();
    private final NoteActionListener listener;

    public NoteAdapter(NoteActionListener listener) {
        this.listener = listener;
    }

    public void updateList(List<PersonalNote> list) {
        notes.clear();
        if (list != null) notes.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoteBinding binding = ItemNoteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PersonalNote note = notes.get(position);
        holder.bind(note, listener);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNoteBinding binding;

        public ViewHolder(ItemNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(PersonalNote note, NoteActionListener listener) {
            binding.tvNoteTitle.setText(note.getTitle());
            binding.tvNoteContent.setText(note.getContent());
            binding.tvClassroomTag.setText(note.getClassroomName() != null ? note.getClassroomName() : "General");
            binding.tvNoteDate.setText("Updated " + DateUtils.formatDate(note.getUpdatedAt()));

            binding.btnEditNote.setOnClickListener(v -> {
                if (listener != null) listener.onEditNote(note);
            });
            binding.btnDeleteNote.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteNote(note);
            });
        }
    }
}
