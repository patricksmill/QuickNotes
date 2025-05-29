package com.example.quicknotes.controller;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.quicknotes.R;
import com.example.quicknotes.model.TagManager;
import com.example.quicknotes.model.Note;
import com.example.quicknotes.model.NoteLibrary;
import com.example.quicknotes.model.Notifier;
import com.example.quicknotes.view.MainUI;
import com.example.quicknotes.view.ManageNoteFragment;
import com.example.quicknotes.view.NotesUI;
import com.example.quicknotes.view.SearchNotesFragment;
import com.example.quicknotes.view.SettingsFragment;
import com.google.android.material.snackbar.Snackbar;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * ControllerActivity acts as the Controller in the MVC architecture
 * It mediates between the NoteLibrary (Model) and the various UI Fragments (View),
 * handling user actions and updating the model or view as appropriate.
 */
public class ControllerActivity extends AppCompatActivity implements NotesUI.Listener {
    private MainUI mainUI;
    private NoteLibrary noteLibrary;
    private SearchNotesFragment currentSearchFragment;
    private Notifier notifier;
    private boolean wasWaitingForPermission = false;

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.noteLibrary = new NoteLibrary(getApplicationContext());
        this.notifier = new Notifier(this);
        this.mainUI = new MainUI(this);
        setContentView(this.mainUI.getRootView());
        
        // Provide root view to notifier for Snackbar display
        this.notifier.setRootView(this.mainUI.getRootView());

        setupFragments(savedInstanceState);
        handleNotificationIntent(getIntent());
        checkAlarmPermission();
    }

    /**
     * Checks and requests alarm permission if needed, with user-friendly messaging.
     */
    private void checkAlarmPermission() {
        if (!notifier.canScheduleExactAlarms()) {
            new AlertDialog.Builder(this)
                    .setTitle("Notification Permission Required")
                    .setMessage("QuickNotes needs permission to schedule exact alarms for note reminders. " +
                               "This allows you to get notified at the exact time you set for your notes.")
                    .setPositiveButton("Grant Permission", (dialog, which) -> {
                        wasWaitingForPermission = true;
                        notifier.requestExactAlarmPermission();
                    })
                    .setNegativeButton("Later", null)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if permission was granted when returning from settings
        if (wasWaitingForPermission && notifier.canScheduleExactAlarms()) {
            wasWaitingForPermission = false;
            Snackbar.make(mainUI.getRootView(), "Alarm permission granted! You can now set note reminders.", 
                         Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Sets up initial fragments based on saved state.
     */
    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            showSearchFragment();
        } else {
            Fragment existing = getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
            if (existing instanceof SearchNotesFragment) {
                currentSearchFragment = (SearchNotesFragment) existing;
                currentSearchFragment.setListener(this);
            } else if (existing instanceof ManageNoteFragment) {
                ((ManageNoteFragment) existing).setListener(this);
            }
        }
    }

    /**
     * Creates and displays a new SearchNotesFragment.
     */
    private void showSearchFragment() {
        SearchNotesFragment searchFrag = new SearchNotesFragment();
        searchFrag.setListener(this);
        currentSearchFragment = searchFrag;
        this.mainUI.displayFragment(searchFrag, false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    /**
     * Handles intents from notifications to view specific notes.
     */
    private void handleNotificationIntent(Intent intent) {
        if (intent == null || !"viewNote".equals(intent.getStringExtra("action"))) return;

        String noteTitle = intent.getStringExtra("noteTitle");
        if (noteTitle == null) return;

        Note noteToView = noteLibrary.getNotes().stream()
                .filter(note -> note.getTitle().equals(noteTitle))
                .findFirst()
                .orElse(null);

        if (noteToView != null) {
            onBrowseNotes();
            onManageNotes(noteToView);
        } else {
            Snackbar.make(mainUI.getRootView(), "Note not found: " + noteTitle, Snackbar.LENGTH_LONG).show();
        }
    }


    /**
     * Adds demo notes to the library and updates the view.
     */
    public void onAddDemoNotes() {
        String[] titles = {"Meeting", "Shopping List", "Ideas for Presentation", "Reminder", "Workout routine"};
        String[] topics = {
                "discuss the new project timeline and deliverables. Ensure we increase shareholder value",
                "groceries, household items, and birthday gifts.",
                "emphasize key points, statistics, and visual aids.",
                "call the doctor's office to schedule the a physical",
                "4 sets of 10 push-ups, 10 sit-ups, 10 squats, and 10 lunges."
        };
        
        for (int i = 0; i < topics.length; i++) {
            noteLibrary.addNote(new Note(titles[i], topics[i], new LinkedHashSet<>()));
        }
        updateNotesView();
        Snackbar.make(mainUI.getRootView(), "Demo notes added", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onNewNote() {
        onManageNotes(new Note("", "", new LinkedHashSet<>()));
    }

    /**
     * Saves a note to the library and updates the view.
     * @param note the note to be saved
     * @param isNewNote whether this is a new note or an existing one being edited
     */
    @Override
    public void onSaveNote(@NonNull Note note, boolean isNewNote) {
        if (isNewNote) {
            noteLibrary.addNote(note);
            noteLibrary.getManageTags().cleanupUnusedTags();
        }
        updateNotesView();
    }

    /**
     * Deletes the specified note from the library and updates the view.
     *
     * @param note the note to be deleted
     */
    @Override
    public void onDeleteNote(@NonNull Note note) {
        notifier.cancelNotification(note);
        noteLibrary.deleteNote(note);
        updateNotesView();
    }

    /**
     * Restores the most recently deleted note, if possible, and updates the view.
     */
    @Override
    public void onUndoDelete() {
        if (noteLibrary.undoDelete() && currentSearchFragment != null) {
            currentSearchFragment.updateView(noteLibrary.getNotes());
        }
    }


    /**
     * Toggles the pin status of the specified note and updates the view.
     *
     * @param note The note to pin or unpin
     */
    @Override
    public void onTogglePin(@NonNull Note note) {
        noteLibrary.togglePin(note);
        updateNotesView();
    }

    /**
     * Navigates back to the main notes browsing fragment.
     */
    @Override
    public void onBrowseNotes() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        if (!(current instanceof SearchNotesFragment)) {
            showSearchFragment();
        }
    }

    /**
     * Retrieves all user notes from the library.
     *
     * @return A list containing all notes
     */
    @Override
    public List<Note> onGetNotes() {
        return noteLibrary.getNotes();
    }

    /**
     * Navigates to the ManageNoteFragment for the specified note.
     *
     * @param note The note to be managed
     */
    @Override
    public void onManageNotes(@NonNull Note note) {
        ManageNoteFragment fragment = new ManageNoteFragment();
        fragment.setListener(this);
        fragment.setNoteToEdit(note);
        fragment.show(getSupportFragmentManager(), "ManageNote");
    }

    /**
     * Returns the ManageTags instance for tag management.
     *
     * @return the ManageTags instance
     */
    @Override
    public TagManager onManageTags() {
        return noteLibrary.getManageTags();
    }

    /**
     * Sets a tag for the specified note.
     *
     * @param note The note to tag
     * @param tag  The tag to add
     */
    @Override
    public void onSetTag(@NonNull Note note, @NonNull String tag) {
        noteLibrary.getManageTags().setTag(note, tag);
        updateNotesView();
    }

    @Override
    public void onSetNotification(@NonNull Note note, boolean enabled, Date date) {
        // Check alarm permission before setting notification
        if (enabled && !notifier.canScheduleExactAlarms()) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("To set note reminders, QuickNotes needs permission to schedule exact alarms.")
                    .setPositiveButton("Grant Permission", (dialog, which) -> {
                        wasWaitingForPermission = true;
                        notifier.requestExactAlarmPermission();
                        // Note: The notification will be set when user returns from settings
                        // and onResume() is called, if they grant permission
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> Snackbar.make(mainUI.getRootView(), "Notification not set - permission required",
                                 Snackbar.LENGTH_SHORT).show())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }
        
        notifier.updateNotification(note, enabled, date);
        noteLibrary.updateNoteNotificationSettings(note, enabled, date);
        updateNotesView();
    }

    @Override
    public void onDeleteAllNotes() {
        noteLibrary.deleteAllNotes();
        updateNotesView();
        Snackbar.make(mainUI.getRootView(), "All notes deleted", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public java.util.Set<com.example.quicknotes.model.Tag> onGetAllTags() {
        return noteLibrary.getManageTags().getAllTags();
    }

    @Override
    public java.util.List<com.example.quicknotes.model.TagColorManager.ColorOption> onGetAvailableColors() {
        return noteLibrary.getManageTags().getAvailableColors();
    }

    @Override
    public void onSetTagColor(@NonNull String tagName, int colorResId) {
        noteLibrary.getManageTags().setTagColor(tagName, colorResId);
    }

    @Override
    public boolean onValidateNotificationDate(@NonNull Date date) {
        return notifier.isValidNotificationDate(date);
    }

    @Override
    public boolean onShouldShowNotificationIcon(@NonNull Note note) {
        return note.isNotificationsEnabled() && note.getNotificationDate() != null && note.getNotificationDate().after(new Date());
    }

    /**
     * Handles a search query by delegating to the model's searchNotes method and updating the view.
     *
     * @param query   The search query
     * @param title   Whether to search in titles
     * @param content Whether to search in content
     * @param tag     Whether to search in tags
     */
    @Override
    public void onSearchNotes(@NonNull String query, boolean title, boolean content, boolean tag) {
        List<Note> searchResults = noteLibrary.searchNotes(query, title, content, tag);
        if (currentSearchFragment != null) {
            currentSearchFragment.updateView(searchResults);
        }
    }

    /**
     * Navigates to the SettingsFragment.
     */
    @Override
    public void onOpenSettings() {
        mainUI.displayFragment(new SettingsFragment(), true);
    }

    /**
     * Updates the notes view with the current state of the note library.
     */
    private void updateNotesView() {
        if (currentSearchFragment != null) {
            currentSearchFragment.updateView(noteLibrary.getNotes());
        }
    }
}




