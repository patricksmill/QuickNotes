package com.example.quicknotes.view;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.quicknotes.R;
import com.example.quicknotes.databinding.FragmentManageNoteBinding;
import com.example.quicknotes.model.Note;
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

/**
 * Fragment for managing note creation and editing.
 * Handles both new note creation and existing note modification.
 */
public class ManageNoteFragment extends BottomSheetDialogFragment implements NotesUI {
    private FragmentManageNoteBinding binding;
    private Listener listener;
    private Note currentNote;
    private boolean isNewNote = false;

    /**
     * Called when it's time to create the view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return the fragment's root view.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        this.binding = FragmentManageNoteBinding.inflate(inflater, container, false);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
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

    /**
     * Sets up all view listeners and event handlers.
     */
    private void setupListeners() {
        binding.tagsInputLayout.setEndIconOnClickListener(v -> 
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("How to enter tags")
                        .setMessage("Enter tags separated by commas, like:\n\nwork, urgent, meeting\n\nSpaces are optional but will be trimmed.")
                        .setPositiveButton("Got it", null)
                        .show());

        binding.saveButton.setOnClickListener(v -> saveNote());
        android.view.View aiBtn = getView().findViewById(R.id.aiSuggestButton);
        android.view.View aiProgress = getView().findViewById(R.id.aiSuggestProgress);
        if (aiBtn != null) {
            aiBtn.setOnClickListener(v -> suggestTagsWithLoading(aiBtn, aiProgress));
        }
        // Cancel at top just dismisses
        assert getView() != null;
        android.view.View cancel = getView().findViewById(R.id.cancelButton);
        if (cancel != null) {
            cancel.setOnClickListener(v -> dismiss());
        }
    }

    private void suggestTagsWithLoading(android.view.View aiBtn, android.view.View progress) {
        if (listener == null) return;
        boolean aiConfigured = listener.onIsAiTaggingConfigured();
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
        if (aiBtn != null) aiBtn.setEnabled(false);
        if (progress != null) progress.setVisibility(View.VISIBLE);
        listener.onAiSuggestTags(temp, 5, suggestions -> {
            if (suggestions == null || suggestions.isEmpty()) {
                showError("No suggestions");
            } else {
                showSuggestionDialog(suggestions);
            }
            if (aiBtn != null) aiBtn.setEnabled(true);
            if (progress != null) progress.setVisibility(View.GONE);
        }, err -> {
            showError("Suggest failed: " + err);
            if (aiBtn != null) aiBtn.setEnabled(true);
            if (progress != null) progress.setVisibility(View.GONE);
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
                    if (!chosen.isEmpty() && listener != null) {
                        listener.onSetTags(currentNote, chosen);
                        bindNoteFields();
                    }
                })
                .show();
    }

    /**
     * Handles saving the current note.
     */
    private void saveNote() {
        if (listener == null) {
            showError("Unable to save note at this time");
            return;
        }

        String title = getText(binding.noteTitleText).trim();
        String content = getText(binding.noteContentText).trim();
        String tagsString = getText(binding.noteTagsText);

        if (title.isEmpty() || content.isEmpty()) {
            showError(getString(R.string.missing_item_field_error));
            return;
        }

        List<String> tagNames = Arrays.stream(tagsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        currentNote.setTitle(title);
        currentNote.setContent(content);
        currentNote.getTags().clear();
        listener.onSetTags(currentNote, tagNames);

        // If AI confirmation is enabled and this is a new note, show suggestions dialog instead of auto-applying
        if (isNewNote && listener.onShouldConfirmAiSuggestions() && listener.onIsAiTaggingConfigured()) {
            // Do not navigate away yet; show suggestions first
            listener.onAiSuggestTags(currentNote, 5, s -> {
                if (s != null && !s.isEmpty()) showSuggestionDialog(s);
            }, e -> {});
        }

        if (handleNotifications()) {
            listener.onSaveNote(currentNote, isNewNote);
            listener.onBrowseNotes();
            dismiss();
        }
    }

    /**
     * Handles notification settings validation and setup.
     */
    private boolean handleNotifications() {
        boolean enabled = binding.notificationsEnabled.isChecked();
        Date selectedDate = null;

        if (enabled) {
            selectedDate = getSelectedDate();
            if (listener != null && !listener.onValidateNotificationDate(selectedDate)) {
                showError("Cannot set notification for past date/time. Please select a future time.");
                return false;
            }
        }

        if (listener != null) {
            listener.onSetNotification(currentNote, enabled, selectedDate);
        }
        return true;
    }

    /**
     * Gets text from EditText widget safely.
     */
    private String getText(android.widget.EditText editText) {
        return Objects.requireNonNull(editText.getText()).toString();
    }

    /**
     * Shows error message using Snackbar.
     */
    private void showError(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    /**
     * Gets the selected date and time from the date and time pickers.
     */
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

    /**
     * Sets the listener for UI events.
     *
     * @param listener the listener object
     */
    public void setListener(final Listener listener) {
        this.listener = listener;
    }

    /**
     * Binds note data to the UI fields.
     */
    private void bindNoteFields() {
        if (currentNote == null) return;

        binding.noteTitleText.setText(currentNote.getTitle());
        binding.noteContentText.setText(currentNote.getContent());
        binding.noteTagsText.setText(TextUtils.join(", ", 
                currentNote.getTags().stream().map(Tag::name).collect(Collectors.toList())));

        setupNotificationFields();
    }

    /**
     * Sets up notification-related fields and listeners.
     */
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

    /**
     * Updates visibility of notification date/time pickers.
     */
    private void updateNotificationVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        binding.datePicker.setVisibility(visibility);
        binding.timePicker.setVisibility(visibility);
    }

    /**
     * Sets date/time pickers from the current note's notification date.
     */
    private void setDateTimeFromNote() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentNote.getNotificationDate());
        binding.datePicker.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        binding.timePicker.setHour(cal.get(Calendar.HOUR_OF_DAY));
        binding.timePicker.setMinute(cal.get(Calendar.MINUTE));
    }

    /**
     * Sets the date and time pickers to a default time (1 hour from now).
     */
    private void setDefaultNotificationTime() {
        Calendar defaultTime = Calendar.getInstance();
        defaultTime.add(Calendar.HOUR_OF_DAY, 1);
        binding.datePicker.updateDate(defaultTime.get(Calendar.YEAR), defaultTime.get(Calendar.MONTH), defaultTime.get(Calendar.DAY_OF_MONTH));
        binding.timePicker.setHour(defaultTime.get(Calendar.HOUR_OF_DAY));
        binding.timePicker.setMinute(defaultTime.get(Calendar.MINUTE));
    }

    /**
     * Sets the note to be edited in this fragment.
     */
    public void setNoteToEdit(Note note) {
        this.currentNote = note;
        this.isNewNote = currentNote.getTitle().isEmpty() && currentNote.getContent().isEmpty();
        if (binding != null) bindNoteFields();
    }
}

