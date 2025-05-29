package com.example.quicknotes.view;

import androidx.annotation.NonNull;
import com.example.quicknotes.model.ManageTags;
import com.example.quicknotes.model.Note;
import java.util.Date;
import java.util.List;

/**
 * Interface that defines the methods that the program's user interface should contain.
 */
public interface NotesUI {
    void updateView(List<Note> notes);

    /**
     * Interface that classes interested in being notified of events happening
     * to the user interface should implement.
     */
    interface Listener {
        /**
         * Creates and displays a new empty note.
         */
        void onNewNote();

        /**
         * Saves a note to the library, whether it's new or existing.
         * @param note the note to be saved
         * @param isNewNote whether this is a new note or an existing one being edited
         */
        void onSaveNote(@NonNull Note note, boolean isNewNote);

        /**
         * Navigates to the main notes browsing view.
         */
        void onBrowseNotes();

        /**
         * Retrieves all user notes.
         * @return a list of all notes
         */
        List<Note> onGetNotes();

        /**
         * Opens the note management interface for a specific note.
         * @param note The note to be managed
         */
        void onManageNotes(@NonNull Note note);

        /**
         * Deletes a note from the library.
         * @param note the note to be deleted
         * @return the deleted note, or null if not found
         */
        Note onDeleteNote(@NonNull Note note);

        /**
         * Restores the most recently deleted note.
         */
        void onUndoDelete();

        /**
         * Adds a tag to a note.
         * @param note the note to tag
         * @param tag the tag to add
         */
        void onSetTag(@NonNull Note note, @NonNull String tag);

        /**
         * Gets the tag management system.
         * @return the tag management instance
         */
        ManageTags onManageTags();

        /**
         * Searches notes by various criteria.
         * @param query the search query
         * @param title whether to search in titles
         * @param content whether to search in content
         * @param tag whether to search in tags
         */
        void onSearchNotes(@NonNull String query, boolean title, boolean content, boolean tag);

        /**
         * Opens the settings interface.
         */
        void onOpenSettings();

        /**
         * Toggles the pin status of a note.
         * @param note the note to pin/unpin
         */
        void onTogglePin(@NonNull Note note);

        /**
         * Adds demo notes to the library.
         */
        void onAddDemoNotes();

        /**
         * Configures notification settings for a note.
         * @param note the note to configure
         * @param enabled whether notifications are enabled
         * @param date the notification date/time
         */
        void onSetNotification(@NonNull Note note, boolean enabled, Date date);
    }

    /**
     * Sets the listener object to be notified of UI events.
     * @param listener the listener object
     */
    void setListener(final Listener listener);
}
