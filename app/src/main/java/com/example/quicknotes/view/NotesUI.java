package com.example.quicknotes.view;

import androidx.annotation.NonNull;

import com.example.quicknotes.model.Tag;
import com.example.quicknotes.model.TagColorManager;
import com.example.quicknotes.model.TagManager;
import com.example.quicknotes.model.Note;
import java.util.Date;
import java.util.List;

/**
 * Interface that defines the methods that the program's user interface should contain.
 */
public interface NotesUI {

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
         *
         * @param note the note to be deleted
         */
        void onDeleteNote(@NonNull Note note);

        /**
         * Restores the most recently deleted note.
         */
        void onUndoDelete();

        /**
         * Adds multiple tags to a note in a single operation.
         * @param note the note to tag
         * @param tags list of tag names to add
         */
        void onSetTags(@NonNull Note note, @NonNull java.util.List<String> tags);

        /**
         * Gets the tag management system.
         * @return the tag management instance
         */
        TagManager onManageTags();

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

        /**
         * Deletes all notes from the library.
         */
        void onDeleteAllNotes();

        /**
         * Gets all available tags.
         * @return set of all tags
         */
        java.util.Set<Tag> onGetAllTags();

        /**
         * Gets available color options for tags.
         * @return list of color options
         */
        java.util.List<TagColorManager.ColorOption> onGetAvailableColors();

        /**
         * Sets the color for a specific tag.
         * @param tagName the name of the tag
         * @param colorResId the resource ID of the color
         */
        void onSetTagColor(@NonNull String tagName, int colorResId);

        /**
         * Validates if a notification date is valid (not in the past).
         * @param date the date to validate
         * @return true if the date is valid, false otherwise
         */
        boolean onValidateNotificationDate(@NonNull Date date);

        /**
         * Checks if a note should display a notification icon.
         * @param note the note to check
         * @return true if the notification icon should be visible, false otherwise
         */
        boolean onShouldShowNotificationIcon(@NonNull Note note);

        // ===== Tag management operations =====
        void onRenameTag(@NonNull String oldName, @NonNull String newName);
        void onDeleteTag(@NonNull String tagName);
        void onMergeTags(@NonNull java.util.List<String> sources, @NonNull String target);

        // ===== AI suggestion controls =====
        boolean onIsAiTaggingConfigured();
        boolean onShouldConfirmAiSuggestions();
        void onAiSuggestTags(@NonNull Note note, int limit,
                             @NonNull java.util.function.Consumer<java.util.List<String>> onSuggestions,
                             @NonNull java.util.function.Consumer<String> onError);
    }

}
