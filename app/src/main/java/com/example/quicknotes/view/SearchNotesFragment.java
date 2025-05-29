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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchNotesFragment extends Fragment implements NotesUI {
    private FragmentSearchNotesBinding binding;
    private Listener listener;

    private final TagListAdapter tagAdapter = new TagListAdapter();
    private final NotesListAdapter notesListAdapter = new NotesListAdapter();
    private final Set<String> activeTagFilters = new LinkedHashSet<>();

    private List<Note> baseNotes;
    private String currentSortBy = "date"; // Default sort by date


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSearchNotesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup Notes RecyclerView.
        binding.notesRecyclerView.setAdapter(notesListAdapter);
        binding.notesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup Tag RecyclerView
        binding.tagRecyclerView.setAdapter(tagAdapter);
        binding.tagRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // Setup SearchView
        SearchView searchView = binding.searchBar;
        // When the close (X) button is pressed, clear the query and show all notes.
        searchView.setOnCloseListener(() -> {
            searchView.setQuery("", false);
            if (listener != null) {
                listener.onSearchNotes("", true, true, true);
            }
            return false;
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (listener != null) {
                    listener.onSearchNotes(query, true, true, true);
                }
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (listener != null) {
                    listener.onSearchNotes(newText, true, true, true);
                }
                return true;
            }
        });

        // Set up sort button click listener
        binding.sortButton.setOnClickListener(v -> showSortDialog());

        // Set up swipe to delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                Note noteToDelete = notesListAdapter.listNotes.get(position);
                listener.onDeleteNote(noteToDelete);
                Snackbar.make(binding.getRoot(), "Note deleted", Snackbar.LENGTH_LONG)
                        .setAction("Undo", v -> listener.onUndoDelete())
                        .show();
            }
        }).attachToRecyclerView(binding.notesRecyclerView);


        // Set up FAB click listener
        this.binding.addNoteFab.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNewNote();
            }
        });

        this.binding.addNoteFab.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onAddDemoNotes();
                updateView(listener.onGetNotes());
            }
            return true;
        });

        // Set up Settings Cog click listener
        this.binding.settingsButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpenSettings();
            }
        });

        // Initial load of notes
        if (listener != null) {
            updateView(listener.onGetNotes());

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView(listener.onGetNotes());
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
        if (listener != null && binding != null) {
            updateView(listener.onGetNotes());
        }
    }


    public void updateView(List<Note> notes) {
        baseNotes = notes != null
                ? new ArrayList<>(notes)
                : new ArrayList<>();
        
        if (binding != null && listener != null) {
            tagAdapter.updateData(listener.onManageTags().getAllTags());
            tagAdapter.setSelectedTags(activeTagFilters);
            displayNotes();
        }
    }

    private void displayNotes() {
        if (binding == null) return;

        List<Note> notes = activeTagFilters.isEmpty()
                ? new ArrayList<>(baseNotes)
                : baseNotes.stream()
                        .filter(n -> listener.onManageTags()
                                .filterNotesByTags(activeTagFilters)
                                .contains(n))
                        .collect(Collectors.toList());

        notes.sort((a, b) -> {
            int pinCmp = Boolean.compare(b.isPinned(), a.isPinned());
            if (pinCmp != 0) return pinCmp;

            if ("title".equals(currentSortBy)) {
                return a.getTitle()
                        .compareToIgnoreCase(b.getTitle());
            } else { // date
                return b.getLastModified()
                        .compareTo(a.getLastModified());
            }
        });

        notesListAdapter.updateData(notes);
        binding.emptyState.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
        binding.notesRecyclerView.setVisibility(notes.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupPins(ImageView pinIcon, Note note) {
        pinIcon.setSelected(note.isPinned());
        pinIcon.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTogglePin(note);
                v.setSelected(note.isPinned());
                String msg = note.isPinned() ? "pinned" : "unpinned";
                Snackbar.make(binding.getRoot(), "Note " + msg, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void showSortDialog() {
        String[] sortOptions = {"Sort by Date", "Sort by Title"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Sort Notes")
                .setItems(sortOptions, (dialog, which) -> {
                    currentSortBy = (which == 1) ? "title" : "date";
                    displayNotes();                // re-run filter+sort pipeline
                })
                .show();
    }

    private class TagListAdapter extends RecyclerView.Adapter<TagListAdapter.ViewHolder> {
        private List<Tag> listTags = new LinkedList<>();
        private final Set<String> selectedTags = new LinkedHashSet<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TagChipBinding binding = TagChipBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false
            );
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(listTags.get(position), selectedTags.contains(listTags.get(position).getName()));
        }

        @Override
        public int getItemCount() {
            return listTags.size();
        }

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
                binding.tagName.setText(tag.getName());

                int colorInt = ContextCompat.getColor(
                        requireContext(),
                        tag.getColorResId()
                );

                binding.getRoot().setStrokeColor(colorInt);
                binding.getRoot().setStrokeWidth(
                        isSelected
                                ? getResources().getDimensionPixelSize(R.dimen.chip_stroke_width)
                                : 0
                );

                binding.getRoot().setOnClickListener(v -> {
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
            ListNoteBinding binding = ListNoteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(listNotes.get(position));
        }

        @Override
        public int getItemCount() {return listNotes.size();}

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
                binding.noteDateText.setText(note.getLastModified().toString().substring(0, 16)); // Cuts off the year and EDT
                binding.noteTagsText.setText(TextUtils.join(", ", note.getTagNames()));
                setupPins(binding.notePinIcon, note);
                binding.noteNotificationIcon.setVisibility(
                        note.isNotificationsEnabled() && note.getNotificationDate() != null && note.getNotificationDate().after(new Date())
                                ? View.VISIBLE
                                : View.GONE
                );

                binding.getRoot().setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onManageNotes(note);
                    }
                });
            }
        }
    }
}