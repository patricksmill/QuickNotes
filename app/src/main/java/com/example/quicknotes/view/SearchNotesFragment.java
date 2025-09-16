package com.example.quicknotes.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quicknotes.R;
import com.example.quicknotes.databinding.FragmentSearchNotesBinding;
import com.example.quicknotes.databinding.ListNoteBinding;
import com.example.quicknotes.databinding.TagChipBinding;
import com.example.quicknotes.model.Note;
import com.example.quicknotes.model.Tag;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fragment responsible for displaying and managing the main notes view.
 * Provides functionality for searching, filtering, sorting, and managing notes.
 * Follows MVC pattern by delegating all business logic to the controller.
 */
public class SearchNotesFragment extends Fragment implements NotesUI {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
    
    private FragmentSearchNotesBinding binding;
    private Listener listener;
    private final TagListAdapter tagAdapter = new TagListAdapter();
    private final NotesListAdapter notesListAdapter = new NotesListAdapter();
    private final Set<String> activeTagFilters = new LinkedHashSet<>();
    private List<Note> baseNotes = new ArrayList<>();
    private String currentSortBy = "date";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchNotesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerViews();
        setupSearchView();
        setupClickListeners();
        setupSwipeToDelete();
        if (listener != null) updateView(listener.onGetNotes());
    }

    private void setupRecyclerViews() {
        binding.notesRecyclerView.setAdapter(notesListAdapter);
        binding.notesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.tagRecyclerView.setAdapter(tagAdapter);
        binding.tagRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
    }

    private void setupSearchView() {
        SearchView searchView = binding.searchBar;
        searchView.setOnCloseListener(() -> {
            searchView.setQuery("", false);
            if (listener != null) listener.onSearchNotes("", true, true, true);
            return false;
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return handleSearch(query);
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return handleSearch(newText);
            }

            private boolean handleSearch(String query) {
                if (listener != null) listener.onSearchNotes(query, true, true, true);
                return true;
            }
        });
    }

    private void setupClickListeners() {
        binding.sortButton.setOnClickListener(v -> showSortDialog());
        binding.addNoteFab.setOnClickListener(v -> { if (listener != null) listener.onNewNote(); });
        binding.addNoteFab.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onAddDemoNotes();
                updateView(listener.onGetNotes());
            }
            return true;
        });
        binding.settingsButton.setOnClickListener(v -> { if (listener != null) listener.onOpenSettings(); });
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION || listener == null) return;

                Note noteToDelete = notesListAdapter.listNotes.get(position);
                listener.onDeleteNote(noteToDelete);
                Snackbar.make(binding.getRoot(), "Note deleted", Snackbar.LENGTH_LONG)
                        .setAction("Undo", v -> listener.onUndoDelete())
                        .show();
            }
        }).attachToRecyclerView(binding.notesRecyclerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listener != null) updateView(listener.onGetNotes());
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
        if (listener != null && binding != null) updateView(listener.onGetNotes());
    }

    @Override
    public void updateView(List<Note> notes) {
        baseNotes = notes != null ? new ArrayList<>(notes) : new ArrayList<>();
        if (binding != null && listener != null) {
            tagAdapter.updateData(listener.onManageTags().getAllTags());
            tagAdapter.setSelectedTags(activeTagFilters);
            displayNotes();
        }
    }

    private void displayNotes() {
        if (binding == null || listener == null) return;

        List<Note> notes = activeTagFilters.isEmpty() ? new ArrayList<>(baseNotes)
                : baseNotes.stream()
                .filter(n -> listener.onManageTags().filterNotesByTags(activeTagFilters).contains(n))
                .collect(Collectors.toList());

        notes.sort((a, b) -> {
            int pinCmp = Boolean.compare(b.isPinned(), a.isPinned());
            if (pinCmp != 0) return pinCmp;
            return "title".equals(currentSortBy) 
                    ? a.getTitle().compareToIgnoreCase(b.getTitle())
                    : b.getLastModified().compareTo(a.getLastModified());
        });

        notesListAdapter.updateData(notes);
        boolean isEmpty = notes.isEmpty();
        binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.notesRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setupPins(ImageView pinIcon, Note note) {
        pinIcon.setSelected(note.isPinned());
        pinIcon.setOnClickListener(v -> {
            if (listener == null) return;
            listener.onTogglePin(note);
            v.setSelected(note.isPinned());
            Snackbar.make(binding.getRoot(), "Note " + (note.isPinned() ? "pinned" : "unpinned"), Snackbar.LENGTH_SHORT).show();
        });
    }

    private void showSortDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Sort Notes")
                .setItems(new String[]{"Sort by Date", "Sort by Title"}, (dialog, which) -> {
                    currentSortBy = (which == 1) ? "title" : "date";
                    displayNotes();
                })
                .show();
    }

    private class TagListAdapter extends RecyclerView.Adapter<TagListAdapter.ViewHolder> {
        private List<Tag> listTags = new LinkedList<>();
        private final Set<String> selectedTags = new LinkedHashSet<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(TagChipBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(listTags.get(position), selectedTags.contains(listTags.get(position).getName()));
        }

        @Override
        public int getItemCount() { return listTags.size(); }

        public void updateData(@NonNull Collection<Tag> tags) {
            this.listTags = new ArrayList<>(tags);
            notifyDataSetChanged();
        }

        public void setSelectedTags(@NonNull Set<String> names) {
            selectedTags.clear();
            selectedTags.addAll(names);
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final TagChipBinding binding;

            public ViewHolder(@NonNull TagChipBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Tag tag, boolean isSelected) {
                Chip chip = binding.tagChip;
                chip.setText(tag.getName());
                chip.setChecked(isSelected);
                chip.setChipStrokeColor(ContextCompat.getColorStateList(requireContext(), tag.getColorResId()));
                chip.setChipStrokeWidth(getResources().getDimensionPixelSize(R.dimen.chip_stroke_width));
                chip.setOnClickListener(v -> {
                    selectedTags.clear();
                    if (!isSelected) selectedTags.add(tag.getName());
                    activeTagFilters.clear();
                    activeTagFilters.addAll(selectedTags);
                    displayNotes();
                    notifyDataSetChanged();
                });
            }
        }
    }

    private class NotesListAdapter extends RecyclerView.Adapter<NotesListAdapter.ViewHolder> {
        List<Note> listNotes = new LinkedList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ListNoteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(listNotes.get(position));
        }

        @Override
        public int getItemCount() { return listNotes.size(); }

        public void updateData(@NonNull List<Note> notes) {
            this.listNotes = new ArrayList<>(notes);
            notifyDataSetChanged();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final ListNoteBinding binding;

            public ViewHolder(@NonNull final ListNoteBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Note note) {
                binding.noteNameText.setText(note.getTitle());
                binding.noteContentText.setText(note.getContent());
                binding.noteDateText.setText(DATE_FORMAT.format(note.getLastModified()));
                binding.noteTagsText.setText(TextUtils.join(", ", note.getTagNames()));
                setupPins(binding.notePinIcon, note);
                binding.noteNotificationIcon.setVisibility(
                        (listener != null && listener.onShouldShowNotificationIcon(note)) ? View.VISIBLE : View.GONE);
                binding.getRoot().setOnClickListener(v -> { if (listener != null) listener.onManageNotes(note); });
            }
        }
    }
}