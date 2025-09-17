package com.example.quicknotes.view;

import android.app.AlertDialog;
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
        bindNoteFields();
        setupListeners();
    }

    /**
     * Sets up all view listeners and event handlers.
     */
    private void setupListeners() {
        binding.tagsInputLayout.setEndIconOnClickListener(v -> 
                new AlertDialog.Builder(requireContext())
                        .setTitle("How to enter tags")
                        .setMessage("Enter tags separated by commas, like:\n\nwork, urgent, meeting\n\nSpaces are optional but will be trimmed.")
                        .setPositiveButton("Got it", null)
                        .show());

        binding.saveButton.setOnClickListener(v -> saveNote());
        binding.deleteButton.setOnClickListener(this::deleteNote);
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
     * Handles deleting the current note.
     */
    private void deleteNote(View v) {
        if (listener == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Note")
                .setMessage("Are you sure? This will permanently erase your note.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dlg1, which1) -> {
                    listener.onDeleteNote(currentNote);
                    listener.onBrowseNotes();
                    dismiss();
                }).show();
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
    @Override
    public void setListener(final Listener listener) {
        this.listener = listener;
    }

    /**
     * Updates the view with a list of notes (not used in this fragment).
     *
     * @param notes the list of notes to be displayed.
     */
    @Override
    public void updateView(List<Note> notes) {
        // This method is not used in this fragment
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

