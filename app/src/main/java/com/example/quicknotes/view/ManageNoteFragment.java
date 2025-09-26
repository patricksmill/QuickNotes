package com.example.quicknotes.view;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.quicknotes.R;
import com.example.quicknotes.databinding.FragmentManageNoteBinding;
import com.example.quicknotes.model.Note;
import com.example.quicknotes.model.NoteViewModel;
import com.example.quicknotes.model.Tag;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ManageNoteFragment extends BottomSheetDialogFragment {
    private FragmentManageNoteBinding binding;
    private NoteViewModel noteViewModel;
    private Note currentNote;
    private boolean isNewNote = false;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        this.binding = FragmentManageNoteBinding.inflate(inflater, container, false);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        noteViewModel = new ViewModelProvider(requireActivity()).get(NoteViewModel.class);
        navController = NavHostFragment.findNavController(this);

        noteViewModel.getSnackbarMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
            }
        });
        noteViewModel.getUiEvents().observe(getViewLifecycleOwner(), event -> {
            NoteViewModel.UiAction action = event.getContentIfNotHandled();
            if (action == null) return;
            switch (action) {
                case BROWSE_NOTES -> navController.navigateUp();
                case OPEN_SETTINGS -> navController.navigate(R.id.action_searchNotesFragment_to_settingsFragment);
            }
        });

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            );
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeHeight);
            return insets;
        });

        if (getArguments() != null) {
            ManageNoteFragmentArgs args = ManageNoteFragmentArgs.fromBundle(getArguments());
            currentNote = args.getNote();
            if (currentNote != null) {
                isNewNote = currentNote.getTitle().isEmpty() && currentNote.getContent().isEmpty();
            }
        }

        bindNoteFields();
        setupListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog dialog) {
            android.widget.FrameLayout bottomSheet = dialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet
            );
            if (bottomSheet != null) {
                BottomSheetBehavior<android.widget.FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                android.view.ViewGroup.LayoutParams lp = bottomSheet.getLayoutParams();
                lp.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
                bottomSheet.setLayoutParams(lp);
                behavior.setFitToContents(true);
                behavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    private void setupListeners() {
        binding.tagsInputLayout.setEndIconOnClickListener(v -> 
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("How to enter tags")
                        .setMessage("Enter tags separated by commas, like:\n\nwork, urgent, meeting\n\nSpaces are optional but will be trimmed.")
                        .setPositiveButton("Got it", null)
                        .show());

        binding.saveButton.setOnClickListener(v -> saveNote());
        assert getView() != null;
        android.view.View aiBtn = getView().findViewById(R.id.aiSuggestButton);
        android.view.View aiProgress = getView().findViewById(R.id.aiSuggestProgress);
        android.view.View tagPickerBtn = getView().findViewById(R.id.openTagPickerButton);
        if (aiBtn != null) {
            aiBtn.setOnClickListener(v -> suggestTagsWithLoading(aiBtn, aiProgress));
        }
        if (tagPickerBtn != null) {
            tagPickerBtn.setOnClickListener(v -> openTagPicker());
        }

        android.view.View addTagChip = getView().findViewById(R.id.addTagChip);
        if (addTagChip != null) {
            addTagChip.setOnClickListener(v -> promptCreateTag());
        }
        binding.noteTagsText.setOnEditorActionListener((v1, actionId, event) -> {
            tokenizeTagsFromInput();
            return false;
        });
        binding.noteTagsText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && (s.toString().endsWith(" ") || s.toString().endsWith(";") || s.toString().endsWith("\n"))) {
                    tokenizeTagsFromInput();
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        assert getView() != null;
        android.view.View cancel = getView().findViewById(R.id.cancelButton);
        if (cancel != null) {
            cancel.setOnClickListener(v -> dismiss());
        }
    }

    private void suggestTagsWithLoading(android.view.View aiBtn, android.view.View progress) {
        boolean aiConfigured = noteViewModel.isAiTaggingConfigured();
        if (!aiConfigured) {
            showError("AI tagging is not configured");
            return;
        }

        String title = getText(binding.noteTitleText).trim();
        String content = getText(binding.noteContentText).trim();
        if (title.isEmpty() && content.isEmpty()) {
            showError("Enter a title or content first");
            return;
        }

        Note temp = new Note(title, content, new java.util.LinkedHashSet<>());
        if (aiBtn != null) aiBtn.setVisibility(View.INVISIBLE);
        if (progress != null) progress.setVisibility(View.VISIBLE);
        noteViewModel.aiSuggestTags(temp, 5, suggestions -> {
            if (suggestions == null || suggestions.isEmpty()) {
                showError("No suggestions");
            } else {
                showSuggestionDialog(suggestions);
            }
            if (progress != null) progress.setVisibility(View.GONE);
            if (aiBtn != null) aiBtn.setVisibility(View.VISIBLE);
        }, err -> {
            showError("Suggest failed: " + err);
            if (progress != null) progress.setVisibility(View.GONE);
            if (aiBtn != null) aiBtn.setVisibility(View.VISIBLE);
        });
    }

    private void showSuggestionDialog(java.util.List<String> suggestions) {
        String[] items = suggestions.toArray(new String[0]);
        boolean[] checked = new boolean[items.length];
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("AI tag suggestions")
                .setMultiChoiceItems(items, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Apply", (d, w) -> {
                    java.util.List<String> chosen = new java.util.ArrayList<>();
                    for (int i = 0; i < items.length; i++) if (checked[i]) chosen.add(items[i]);
                    if (!chosen.isEmpty()) {
                        noteViewModel.getTagManager().setTags(currentNote, chosen);
                        bindNoteFields();
                        addSelectedChips(chosen);
                    }
                })
                .show();
    }

    private void openTagPicker() {
        java.util.Set<com.example.quicknotes.model.Tag> all = noteViewModel.getTagManager().getAllTags();
        java.util.List<String> names = new java.util.ArrayList<>();
        for (com.example.quicknotes.model.Tag t : all) names.add(t.name());
        names.sort(String::compareToIgnoreCase);
        String[] items = names.toArray(new String[0]);
        boolean[] checked = new boolean[items.length];
        java.util.Set<String> selected = new java.util.LinkedHashSet<>(getSelectedChipNames());
        if (currentNote != null) {
            for (com.example.quicknotes.model.Tag t : currentNote.getTags()) selected.add(t.name());
        }
        for (int i = 0; i < items.length; i++) checked[i] = selected.contains(items[i]);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Pick tags")
                .setMultiChoiceItems(items, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Apply", (d, w) -> {
                    java.util.List<String> chosen = new java.util.ArrayList<>();
                    for (int i = 0; i < items.length; i++) if (checked[i]) chosen.add(items[i]);
                    binding.selectedTagsGroup.removeAllViews();
                    addSelectedChips(chosen);
                    if (currentNote != null) {
                        currentNote.getTags().clear();
                        noteViewModel.getTagManager().setTags(currentNote, chosen);
                    }
                })
                .show();
    }

    private void promptCreateTag() {
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("New tag name");
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Create new tag")
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Add", (d, w) -> {
                    String name = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!name.isEmpty()) addSelectedChips(java.util.Collections.singletonList(name));
                })
                .show();
    }

    private void tokenizeTagsFromInput() {
        String raw = getText(binding.noteTagsText);
        String normalized = raw.replace('\n', ' ').replace(';', ',').replace(' ', ',');
        java.util.List<String> tokens = Arrays.stream(normalized.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toList());
        if (!tokens.isEmpty()) {
            addSelectedChips(tokens);
            binding.noteTagsText.setText("");
        }
    }

    private void addSelectedChips(java.util.List<String> names) {
        for (String name : names) {
            if (name == null || name.trim().isEmpty()) continue;
            if (getSelectedChipNames().contains(name)) continue;
            com.google.android.material.chip.Chip chip = createTagChip(name, true);
            binding.selectedTagsGroup.addView(chip);
        }
    }

    private void renderSuggestedChips(java.util.List<String> suggestions) {
        binding.suggestedTagsGroup.removeAllViews();
        for (String name : suggestions) {
            com.google.android.material.chip.Chip chip = createTagChip(name, false);
            chip.setCheckable(true);
            chip.setOnClickListener(v -> {
                if (chip.isChecked()) {
                    addSelectedChips(java.util.Collections.singletonList(name));
                } else {
                    removeSelectedChip(name);
                }
            });
            binding.suggestedTagsGroup.addView(chip);
        }
    }

    private com.google.android.material.chip.Chip createTagChip(String name, boolean closable) {
        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
        chip.setText(name);
        chip.setCloseIconVisible(closable);
        chip.setClickable(true);
        chip.setCheckable(false);
        chip.setOnCloseIconClickListener(v -> removeSelectedChip(name));
        return chip;
    }

    private void removeSelectedChip(String name) {
        for (int i = 0; i < binding.selectedTagsGroup.getChildCount(); i++) {
            android.view.View v = binding.selectedTagsGroup.getChildAt(i);
            if (v instanceof com.google.android.material.chip.Chip && name.equals(((com.google.android.material.chip.Chip) v).getText().toString())) {
                binding.selectedTagsGroup.removeViewAt(i);
                break;
            }
        }
    }

    private java.util.Set<String> getSelectedChipNames() {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        for (int i = 0; i < binding.selectedTagsGroup.getChildCount(); i++) {
            android.view.View v = binding.selectedTagsGroup.getChildAt(i);
            if (v instanceof com.google.android.material.chip.Chip) names.add(((com.google.android.material.chip.Chip) v).getText().toString());
        }
        return names;
    }

    private void saveNote() {
        String title = getText(binding.noteTitleText).trim();
        String content = getText(binding.noteContentText).trim();
        List<String> tagNames = new java.util.ArrayList<>(getSelectedChipNames());
        String tagsString = getText(binding.noteTagsText);
        if (!tagsString.trim().isEmpty()) {
            String normalized = tagsString.replace('\n', ' ').replace(';', ',').replace(' ', ',');
            java.util.List<String> tokens = java.util.Arrays.stream(normalized.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
            for (String t : tokens) if (!tagNames.contains(t)) tagNames.add(t);
        }

        currentNote.setTitle(title);
        currentNote.setContent(content);
        currentNote.getTags().clear();
        noteViewModel.setTags(currentNote, tagNames);

        if (isNewNote && noteViewModel.shouldConfirmAiSuggestions() && noteViewModel.isAiTaggingConfigured()) {
            noteViewModel.aiSuggestTags(currentNote, 5, this::renderSuggestedChips, e -> {});
        }

        if (handleNotifications()) {
            noteViewModel.saveNote(currentNote, isNewNote);
            noteViewModel.requestBrowseNotes();
        }
    }

    private boolean handleNotifications() {
        boolean enabled = binding.notificationsEnabled.isChecked();
        Date selectedDate = null;

        if (enabled) {
            selectedDate = getSelectedDate();
            if (!noteViewModel.isValidNotificationDate(selectedDate)) {
                showError("Cannot set notification for past date/time. Please select a future time.");
                return false;
            }
        }

        noteViewModel.scheduleNotification(currentNote, enabled, selectedDate);
        return true;
    }

    private String getText(android.widget.EditText editText) {
        return Objects.requireNonNull(editText.getText()).toString();
    }

    private void showError(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    @NonNull
    private Date getSelectedDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, binding.datePicker.getYear());
        cal.set(Calendar.MONTH, binding.datePicker.getMonth());
        cal.set(Calendar.DAY_OF_MONTH, binding.datePicker.getDayOfMonth());
        cal.set(Calendar.HOUR_OF_DAY, binding.timePicker.getHour());
        cal.set(Calendar.MINUTE, binding.timePicker.getMinute());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private void bindNoteFields() {
        if (currentNote == null) return;

        binding.noteTitleText.setText(currentNote.getTitle());
        binding.noteContentText.setText(currentNote.getContent());
        binding.selectedTagsGroup.removeAllViews();
        java.util.List<String> existing = currentNote.getTags().stream().map(Tag::name).collect(Collectors.toList());
        addSelectedChips(existing);
        binding.noteTagsText.setText(TextUtils.join(", ", existing));

        setupNotificationFields();
    }

    private void setupNotificationFields() {
        boolean notificationsEnabled = currentNote.isNotificationsEnabled();
        binding.notificationsEnabled.setChecked(notificationsEnabled);
        updateNotificationVisibility(notificationsEnabled);
        
        binding.datePicker.setMinDate(System.currentTimeMillis());
        binding.notificationsEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateNotificationVisibility(isChecked);
            if (isChecked) setDefaultNotificationTime();
        });

        if (currentNote.getNotificationDate() != null) {
            setDateTimeFromNote();
        } else if (notificationsEnabled) {
            setDefaultNotificationTime();
        }
    }

    private void updateNotificationVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        binding.datePicker.setVisibility(visibility);
        binding.timePicker.setVisibility(visibility);
    }

    private void setDateTimeFromNote() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentNote.getNotificationDate());
        binding.datePicker.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        binding.timePicker.setHour(cal.get(Calendar.HOUR_OF_DAY));
        binding.timePicker.setMinute(cal.get(Calendar.MINUTE));
    }

    private void setDefaultNotificationTime() {
        Calendar defaultTime = Calendar.getInstance();
        defaultTime.add(Calendar.HOUR_OF_DAY, 1);
        binding.datePicker.updateDate(defaultTime.get(Calendar.YEAR), defaultTime.get(Calendar.MONTH), defaultTime.get(Calendar.DAY_OF_MONTH));
        binding.timePicker.setHour(defaultTime.get(Calendar.HOUR_OF_DAY));
        binding.timePicker.setMinute(defaultTime.get(Calendar.MINUTE));
    }
}
