package com.example.quicknotes.view

import com.example.quicknotes.model.note.Note
import com.example.quicknotes.model.tag.Tag
import com.example.quicknotes.model.tag.TagManager
import com.example.quicknotes.model.tag.TagRepository.ColorOption
import java.util.Date
import java.util.function.Consumer

/**
 * Interface that defines the methods that the program's user interface should contain.
 */
interface NotesUI {
    /**
     * Interface that classes interested in being notified of events happening
     * to the user interface should implement.
     */
    interface Listener {
        /**
         * Creates and displays a new empty note.
         */
        fun onNewNote()

        /**
         * Saves a note to the library, whether it's new or existing.
         * @param note the note to be saved
         * @param isNewNote whether this is a new note or an existing one being edited
         */
        fun onSaveNote(note: Note, isNewNote: Boolean)

        /**
         * Navigates to the main notes browsing view.
         */
        fun onBrowseNotes()

        /**
         * Retrieves all user notes.
         * @return a list of all notes
         */
        fun onGetNotes(): List<Note>

        /**
         * Opens the note management interface for a specific note.
         * @param note The note to be managed
         */
        fun onManageNotes(note: Note)

        /**
         * Deletes a note from the library.
         *
         * @param note the note to be deleted
         */
        fun onDeleteNote(note: Note)

        /**
         * Restores the most recently deleted note.
         */
        fun onUndoDelete()

        /**
         * Adds multiple tags to a note in a single operation.
         * @param note the note to tag
         * @param tags list of tag names to add
         */
        fun onSetTags(note: Note, tags: MutableList<String?>)

        /**
         * Gets the tag management system.
         * @return the tag management instance
         */
        fun onManageTags(): TagManager?

        /**
         * Searches notes by various criteria.
         * @param query the search query
         * @param title whether to search in titles
         * @param content whether to search in content
         * @param tag whether to search in tags
         */
        fun onSearchNotes(query: String, title: Boolean, content: Boolean, tag: Boolean)

        /**
         * Opens the settings interface.
         */
        fun onOpenSettings()

        /**
         * Toggles the pin status of a note.
         * @param note the note to pin/unpin
         */
        fun onTogglePin(note: Note)

        /**
         * Adds demo notes to the library.
         */
        fun onAddDemoNotes()

        /**
         * Configures notification settings for a note.
         * @param note the note to configure
         * @param enabled whether notifications are enabled
         * @param date the notification date/time
         */
        fun onSetNotification(note: Note, enabled: Boolean, date: Date?)

        /**
         * Deletes all notes from the library.
         */
        fun onDeleteAllNotes()

        /**
         * Gets all available tags.
         * @return set of all tags
         */
        fun onGetAllTags(): MutableSet<Tag>

        /**
         * Gets available color options for tags.
         * @return list of color options
         */
        fun onGetAvailableColors(): MutableList<ColorOption?>?

        /**
         * Sets the color for a specific tag.
         * @param tagName the name of the tag
         * @param colorResId the resource ID of the color
         */
        fun onSetTagColor(tagName: String, colorResId: Int)

        /**
         * Validates if a notification date is valid (not in the past).
         * @param date the date to validate
         * @return true if the date is valid, false otherwise
         */
        fun onValidateNotificationDate(date: Date): Boolean

        /**
         * Checks if a note should display a notification icon.
         * @param note the note to check
         * @return true if the notification icon should be visible, false otherwise
         */
        fun onShouldShowNotificationIcon(note: Note): Boolean

        // ===== Tag management operations =====
        fun onRenameTag(oldName: String, newName: String)
        fun onDeleteTag(tagName: String)
        fun onMergeTags(sources: MutableList<String?>, target: String)

        // ===== AI suggestion controls =====
        fun onIsAiTaggingConfigured(): Boolean
        fun onShouldConfirmAiSuggestions(): Boolean
        fun onAiSuggestTags(
            note: Note, limit: Int,
            onSuggestions: Consumer<MutableList<String?>?>,
            onError: Consumer<String?>
        )
    }
}
