package com.example.quicknotes.controller;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.quicknotes.R;
import com.example.quicknotes.model.ManageTags;
import com.example.quicknotes.model.Note;
import com.example.quicknotes.model.NoteLibrary;
import com.example.quicknotes.model.Notifier;
import com.example.quicknotes.model.Persistence;
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
        this.mainUI = new MainUI(this);
        setContentView(this.mainUI.getRootView());

        if (savedInstanceState == null) {
            SearchNotesFragment searchFrag = new SearchNotesFragment();
            searchFrag.setListener(this);
            currentSearchFragment = searchFrag;
            this.mainUI.displayFragment(searchFrag, false);
        } else {
            Fragment existing = getSupportFragmentManager()
                    .findFragmentById(R.id.fragmentContainerView);
            if (existing instanceof SearchNotesFragment) {
                currentSearchFragment = (SearchNotesFragment) existing;
                currentSearchFragment.setListener(this);
            } else if (existing instanceof ManageNoteFragment) {
                ((ManageNoteFragment) existing).setListener(this);
            }
        }
    }

    /**
     * Returns the NoteLibrary instance.
     * @return the NoteLibrary instance
     */
    public NoteLibrary getNoteLibrary() {
        return noteLibrary;
    }

    /**
     * Adds demo notes to the library and updates the view.
     */
    public void onAddDemoNotes() {
        String[] titles = {
                "Meeting",
                "Shopping List",
                "Ideas for Presentation",
                "Reminder",
                "Workout routine"
        };
        String[] topics = {
            "discuss the new project timeline and deliverables. Ensure we increase shareholder value",
            "groceries, household items, and birthday gifts.",
            "emphasize key points, statistics, and visual aids.",
            "call the doctor's office to schedule the a physical",
            "4 sets of 10 push-ups, 10 sit-ups, 10 squats, and 10 lunges."
        };
        for (int i = 0; i < topics.length; i++) {
            Note demo = new Note(
                    titles[i],
                    topics[i],
                    new LinkedHashSet<>()
            );
            noteLibrary.addNote(demo);
        }
        updateNotesView();
        Snackbar.make(mainUI.getRootView(), "Demo notes added", Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Navigates to the AddNotesFragment for creating a new note.
     */
    @Override
    public void onNewNote() {
        Note newNote = new Note("", "", new LinkedHashSet<>());
        // Launch bottom sheet to edit it
        onManageNotes(newNote);
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
     * @return the deleted note, or null if not found
     */
    @Override
    public Note onDeleteNote(@NonNull Note note) {
        Note deleted = noteLibrary.deleteNote(note);
        updateNotesView();
        return deleted;
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
        if (current instanceof SearchNotesFragment) return;

        SearchNotesFragment frag = new SearchNotesFragment();
        frag.setListener(this);
        currentSearchFragment = frag;
        mainUI.displayFragment(frag, false);
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
        ManageNoteFragment manageNoteFragment = new ManageNoteFragment();
        manageNoteFragment.setListener(this);
        manageNoteFragment.setNoteToEdit(note);
        manageNoteFragment.show(getSupportFragmentManager(), "ManageNote");
    }

    /**
     * Returns the ManageTags instance for tag management.
     *
     * @return the ManageTags instance
     */
    @Override
    public ManageTags onManageTags() {
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
    public void onSetNotification(Note note, boolean enabled, Date date) {
        note.setNotificationsEnabled(enabled);
        note.setNotificationDate(date);
        Persistence.saveNotes(this, noteLibrary.getNotes());

        // Schedule if needed
        Notifier notifier = new Notifier(this);
        notifier.scheduleNotification(note);
        updateNotesView();
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
     * This is the single point of view updates for consistency.
     */
    private void updateNotesView() {
        if (currentSearchFragment != null) {
            currentSearchFragment.updateView(noteLibrary.getNotes());
        }
    }
}




