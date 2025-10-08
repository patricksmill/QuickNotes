package com.example.quicknotes.view;

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
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quicknotes.R;
import com.example.quicknotes.databinding.FragmentSearchNotesBinding;
import com.example.quicknotes.databinding.ListNoteBinding;
import com.example.quicknotes.databinding.TagChipBinding;
import com.example.quicknotes.model.Note;
import com.example.quicknotes.model.NoteViewModel;
import com.example.quicknotes.model.Tag;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.chip.Chip;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchNotesFragment extends Fragment {
    private DateFormat dateFormat;

    private FragmentSearchNotesBinding binding;
    private NoteViewModel noteViewModel;
    private final TagListAdapter tagAdapter = new TagListAdapter();
    private final NotesListAdapter notesListAdapter = new NotesListAdapter();
    private final Set<String> activeTagFilters = new LinkedHashSet<>();
    private List<Note> baseNotes = new ArrayList<>();
    private String currentSortBy = "date";
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchNotesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        noteViewModel = new ViewModelProvider(requireActivity()).get(NoteViewModel.class);
        navController = NavHostFragment.findNavController(this);

        // Use a non-static, locale-aware formatter so changes to locale at runtime are respected
        dateFormat = java.text.DateFormat.getDateTimeInstance(
                java.text.DateFormat.MEDIUM,
                java.text.DateFormat.SHORT,
                Locale.getDefault()
        );

        setupRecyclerViews();
        setupSearchView();
        setupClickListeners();
        setupSwipeToDelete();

        noteViewModel.getNotes().observe(getViewLifecycleOwner(), this::updateView);
        noteViewModel.getUiEvents().observe(getViewLifecycleOwner(), event -> {
            NoteViewModel.UiAction action = event.getContentIfNotHandled();
            if (action == null) return;
            if (action == NoteViewModel.UiAction.OPEN_SETTINGS) {
                navController.navigate(R.id.action_searchNotesFragment_to_settingsFragment);
            }
        });
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
            noteViewModel.searchNotes("", true, true, true);
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
                noteViewModel.searchNotes(query, true, true, true);
                return true;
            }
        });
    }

    private void setupClickListeners() {
        binding.sortButton.setOnClickListener(v -> showSortDialog());
        binding.manageTagsButton.setOnClickListener(v -> navController.navigate(R.id.action_searchNotesFragment_to_manageTagsFragment));
        binding.addNoteFab.setOnClickListener(v -> onNewNote());
        binding.addNoteFab.setOnLongClickListener(v -> {
            noteViewModel.addDemoNotes();
            return true;
        });
        binding.settingsButton.setOnClickListener(v -> onOpenSettings());
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
                if (position == RecyclerView.NO_POSITION) return;

                Note noteToDelete = notesListAdapter.listNotes.get(position);
                noteViewModel.deleteNote(noteToDelete);
                Snackbar.make(binding.getRoot(), "Note deleted", Snackbar.LENGTH_LONG)
                        .setAction("Undo", v -> noteViewModel.undoDelete())
                        .show();
            }
        }).attachToRecyclerView(binding.notesRecyclerView);
    }

    public void updateView(List<Note> notes) {
        baseNotes = notes != null ? new ArrayList<>(notes) : new ArrayList<>();
        if (binding != null) {
            tagAdapter.updateData(noteViewModel.getAllTags());
            tagAdapter.setSelectedTags(activeTagFilters);
            displayNotes();
        }
    }

    private void displayNotes() {
        if (binding == null) return;

        List<Note> notes = activeTagFilters.isEmpty() ? new ArrayList<>(baseNotes)
                : baseNotes.stream()
                .filter(n -> noteViewModel.filterNotesByTags(activeTagFilters).contains(n))
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
            noteViewModel.togglePin(note);
            v.setSelected(note.isPinned());
            Snackbar.make(binding.getRoot(), "Note " + (note.isPinned() ? "pinned" : "unpinned"), Snackbar.LENGTH_SHORT).show();
        });
    }

    private void showSortDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort Notes")
                .setItems(new String[]{"Sort by Date", "Sort by Title"}, (dialog, which) -> {
                    currentSortBy = (which == 1) ? "title" : "date";
                    displayNotes();
                })
                .show();
    }


    public void onNewNote() {
        SearchNotesFragmentDirections.ActionSearchNotesFragmentToManageNoteFragment action =
                SearchNotesFragmentDirections.actionSearchNotesFragmentToManageNoteFragment(
                        new Note("", "", new LinkedHashSet<>()));
        navController.navigate(action);
    }


    public void onManageNotes(@NonNull Note note) {
        SearchNotesFragmentDirections.ActionSearchNotesFragmentToManageNoteFragment action =
                SearchNotesFragmentDirections.actionSearchNotesFragmentToManageNoteFragment(note);
        navController.navigate(action);
    }


    public void onOpenSettings() {
        navController.navigate(R.id.action_searchNotesFragment_to_settingsFragment);
    }

    private class TagListAdapter extends RecyclerView.Adapter<TagListAdapter.ViewHolder> {
        private List<Tag> listTags = new LinkedList<>();
        private final Set<String> selectedTags = new LinkedHashSet<>();

        public TagListAdapter() {
            setHasStableIds(true);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(TagChipBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(listTags.get(position), selectedTags.contains(listTags.get(position).name()));
        }

        @Override
        public int getItemCount() { return listTags.size(); }

        @Override
        public long getItemId(int position) {
            return listTags.get(position).name().hashCode();
        }

        public void updateData(@NonNull Collection<Tag> tags) {
            List<Tag> newList = new ArrayList<>(tags);
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() { return listTags.size(); }

                @Override
                public int getNewListSize() { return newList.size(); }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return listTags.get(oldItemPosition).name()
                            .equals(newList.get(newItemPosition).name());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    Tag a = listTags.get(oldItemPosition);
                    Tag b = newList.get(newItemPosition);
                    return a.name().equals(b.name()) && a.colorResId() == b.colorResId();
                }
            });
            this.listTags = newList;
            diff.dispatchUpdatesTo(this);
        }

        public void setSelectedTags(@NonNull Set<String> names) {
            Set<String> previous = new LinkedHashSet<>(selectedTags);
            selectedTags.clear();
            selectedTags.addAll(names);

            // Notify only affected chips
            Set<String> changed = new LinkedHashSet<>(previous);
            changed.addAll(selectedTags);
            for (String name : changed) {
                int pos = findTagPositionByName(name);
                if (pos >= 0) notifyItemChanged(pos);
            }
        }

        private int findTagPositionByName(String name) {
            for (int i = 0; i < listTags.size(); i++) {
                if (listTags.get(i).name().equals(name)) return i;
            }
            return -1;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final TagChipBinding binding;

            public ViewHolder(@NonNull TagChipBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Tag tag, boolean isSelected) {
                Chip chip = binding.tagChip;
                chip.setText(tag.name());
                chip.setChecked(isSelected);
                int baseColor = ContextCompat.getColor(requireContext(), tag.colorResId());
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(baseColor));
                // Choose readable text color based on luminance
                int textColor = ColorUtils.calculateLuminance(baseColor) > 0.5 ?
                        ContextCompat.getColor(requireContext(), android.R.color.black) :
                        ContextCompat.getColor(requireContext(), android.R.color.white);
                chip.setTextColor(textColor);
                chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(ColorUtils.blendARGB(baseColor, 0xFF000000, 0.2f)));
                chip.setChipStrokeWidth(getResources().getDimensionPixelSize(R.dimen.chip_stroke_width));
                chip.setOnClickListener(v -> {
                    String previouslySelected = selectedTags.isEmpty() ? null : selectedTags.iterator().next();
                    selectedTags.clear();
                    if (!isSelected) selectedTags.add(tag.name());

                    activeTagFilters.clear();
                    activeTagFilters.addAll(selectedTags);
                    displayNotes();

                    // Notify only the two chips that changed selection state
                    if (previouslySelected != null) {
                        int prevPos = findTagPositionByName(previouslySelected);
                        if (prevPos >= 0) notifyItemChanged(prevPos);
                    }
                    int currPos = getBindingAdapterPosition();
                    if (currPos != RecyclerView.NO_POSITION) notifyItemChanged(currPos);
                });
            }
        }
    }

    private class NotesListAdapter extends RecyclerView.Adapter<NotesListAdapter.ViewHolder> {
        List<Note> listNotes = new LinkedList<>();

        public NotesListAdapter() {
            setHasStableIds(true);
        }

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

        @Override
        public long getItemId(int position) {
            return listNotes.get(position).getId().hashCode();
        }

        public void updateData(@NonNull List<Note> notes) {
            List<Note> newList = new ArrayList<>(notes);
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() { return listNotes.size(); }

                @Override
                public int getNewListSize() { return newList.size(); }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    String a = listNotes.get(oldItemPosition).getId();
                    String b = newList.get(newItemPosition).getId();
                    if (a == null || b == null) return false;
                    return a.equals(b);
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    Note a = listNotes.get(oldItemPosition);
                    Note b = newList.get(newItemPosition);
                    if (a.isPinned() != b.isPinned()) return false;
                    if (!a.getTitle().equals(b.getTitle())) return false;
                    if (!a.getContent().equals(b.getContent())) return false;
                    if (a.getLastModified() == null ? b.getLastModified() != null : !a.getLastModified().equals(b.getLastModified())) return false;
                    if (a.isNotificationsEnabled() != b.isNotificationsEnabled()) return false;
                    if (a.getNotificationDate() == null ? b.getNotificationDate() != null : !a.getNotificationDate().equals(b.getNotificationDate())) return false;
                    // Compare tag names (ordering not guaranteed)
                    List<String> at = new ArrayList<>(a.getTagNames());
                    List<String> bt = new ArrayList<>(b.getTagNames());
                    java.util.Collections.sort(at);
                    java.util.Collections.sort(bt);
                    return at.equals(bt);
                }
            });
            this.listNotes = newList;
            diff.dispatchUpdatesTo(this);
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
                binding.noteDateText.setText(dateFormat.format(note.getLastModified()));
                binding.noteTagsText.setText(TextUtils.join(", ", note.getTagNames()));
                setupPins(binding.notePinIcon, note);
                boolean showNotif = (note.isNotificationsEnabled() && note.getNotificationDate() != null && note.getNotificationDate().after(new java.util.Date()));
                binding.noteNotifIcon.setVisibility(showNotif ? View.VISIBLE : View.GONE);
                binding.getRoot().setOnClickListener(v -> onManageNotes(note));
            }
        }
    }
}