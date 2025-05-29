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

        binding.tagsInputLayout.setEndIconOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("How to enter tags")
                .setMessage("Enter tags separated by commas, like:\n\nwork, urgent, meeting\n\nSpaces are optional but will be trimmed.")
                .setPositiveButton("Got it", null)
                .show());


        // Set up save button click listener
        binding.saveButton.setOnClickListener(v -> {
            String title = Objects.requireNonNull(binding.noteTitleText.getText()).toString().trim();
            String content = Objects.requireNonNull(binding.noteContentText.getText()).toString().trim();
            String tagsString = Objects.requireNonNull(binding.noteTagsText.getText()).toString();

            if (title.isEmpty() || content.isEmpty()) {
                Snackbar.make(v, R.string.missing_item_field_error, Snackbar.LENGTH_LONG).show();
                return;
            }

            List<String> tagNames = Arrays.stream(tagsString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            currentNote.setTitle(title);
            currentNote.setContent(content);

            currentNote.getTags().clear();
            tagNames.forEach(tagName -> listener.onSetTag(currentNote, tagName));

            boolean enabled = binding.notificationsEnabled.isChecked();
            Date selectedDate = getSelectedDate();
            listener.onSetNotification(currentNote, enabled, selectedDate);

            listener.onSaveNote(currentNote, isNewNote);
            listener.onBrowseNotes();
            dismiss(); // Close Sheet
        });

        binding.deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Note")
                    .setMessage("Are you sure? This will permanently erase your note.")
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dlg1, which1) ->
                            listener.onDeleteNote(currentNote)).show();
            listener.onBrowseNotes();
            dismiss(); // Close Sheet
        });
    }

    @NonNull
    private Date getSelectedDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, binding.datePicker.getYear());
        cal.set(Calendar.MONTH, binding.datePicker.getMonth());
        cal.set(Calendar.DAY_OF_MONTH, binding.datePicker.getDayOfMonth());
        cal.set(Calendar.HOUR_OF_DAY, binding.timePicker.getHour());
        cal.set(Calendar.MINUTE, binding.timePicker.getMinute());

        return cal.getTime();
    }

    /**
     * @param listener the listener object
     */
    @Override
    public void setListener(final Listener listener) {this.listener = listener;}

    /**
     * @param notes the list of notes to be displayed.
     */
    @Override
    public void updateView(List<Note> notes) {
        // This method is not used in this fragment
    }

    private void bindNoteFields() {
        if (currentNote == null) return;

        binding.noteTitleText.setText(currentNote.getTitle());
        binding.noteContentText.setText(currentNote.getContent());
        binding.noteTagsText.setText(
                TextUtils.join(", ",
                        currentNote.getTags()
                                .stream()
                                .map(Tag::getName)
                                .collect(Collectors.toList())
                )
        );

        boolean notificationsEnabled = currentNote.isNotificationsEnabled();
        binding.notificationsEnabled.setChecked(notificationsEnabled);
        binding.datePicker.setVisibility(notificationsEnabled ? View.VISIBLE : View.GONE);
        binding.timePicker.setVisibility(notificationsEnabled ? View.VISIBLE : View.GONE);
        binding.datePicker.setMinDate(System.currentTimeMillis());

        binding.notificationsEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.datePicker.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            binding.timePicker.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        if (currentNote.getNotificationDate() != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(currentNote.getNotificationDate());

            binding.datePicker.updateDate(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            binding.timePicker.setHour(cal.get(Calendar.HOUR_OF_DAY));
            binding.timePicker.setMinute(cal.get(Calendar.MINUTE));
        }
    }


    public void setNoteToEdit(Note note) {
        this.currentNote = note;
        this.isNewNote = currentNote.getTitle().isEmpty() && currentNote.getContent().isEmpty();
        if (binding != null) {
            bindNoteFields();
        }
    }
}

