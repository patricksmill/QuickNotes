package com.example.quicknotes.model;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * NoteLibrary manages the collection of notes and their associated operations.
 * It acts as the Model in the MVC architecture, providing methods to add, edit, delete, search, and manage notes.
 */
public class NoteLibrary {
    private final Context ctx;
    private final List<Note> notes;
    private Note recentlyDeletedNote;
    private final TagManager tagManager;

    /**
     * Constructs a NoteLibrary instance with the given context.
     *
     * @param ctx The application context
     */
    public NoteLibrary(@NonNull Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.notes = new ArrayList<>(Persistence.loadNotes(this.ctx));
        ensureNoteIds();
        this.tagManager = new TagManager(this);
    }

    /**
     * Returns the application context associated with this library.
     *
     * @return The application context
     */
    public Context getContext() {
        return ctx;
    }

    /**
     * Returns the ManageTags instance for tag management.
     *
     * @return The ManageTags instance
     */
    public TagManager getManageTags() {
        return tagManager;
    }

    /**
     * Returns a list of all notes in the library.
     *
     * @return List of all notes
     */
    public List<Note> getNotes() {
        return new ArrayList<>(notes);
    }

    /**
     * Adds a new note to the library.
     *
     * @param note The note to add
     */
    public void addNote(@NonNull Note note) {
        if (note.getTitle() == null) return;
        String title = note.getTitle().trim();
        if (title.isEmpty()) return;
        if (notes.stream().anyMatch(n -> n.getTitle().equalsIgnoreCase(title))) {
            return;
        }
        updateNoteDate(note);
        if (tagManager.isAiMode()) {
            tagManager.aiAutoTag(note, tagManager.getAutoTagLimit());
        } else {
            tagManager.simpleAutoTag(note, tagManager.getAutoTagLimit());
        }
        notes.add(note);
        Persistence.saveNotes(ctx, notes);
    }

    /**
     * Deletes a note from the library.
     *
     * @param note The note to delete
     */
    public void deleteNote(@NonNull Note note) {
        if (notes.remove(note)) {
            recentlyDeletedNote = note;
            Persistence.saveNotes(ctx, notes);
        }
    }

    /**
     * Undoes the last delete operation, restoring the most recently deleted note.
     *
     * @return true if the note was successfully restored, false otherwise
     */
    public boolean undoDelete() {
        if (recentlyDeletedNote != null) {
            notes.add(recentlyDeletedNote);
            updateNoteDate(recentlyDeletedNote);
            Persistence.saveNotes(ctx, notes);
            recentlyDeletedNote = null;
            return true;
        }
        return false;
    }

    /**
     * Searches for notes by title, content, or tags, in any combination.
     *
     * @param query   The search query
     * @param title   True to search by title
     * @param content True to search by content
     * @param tag     True to search by tags
     * @return List of notes that match the search query
     */
    public List<Note> searchNotes(@NonNull String query, boolean title, boolean content, boolean tag) {
        if (query.trim().isEmpty()) {
            return getNotes();
        }
        String lower = query.toLowerCase();
        Set<Note> results = new LinkedHashSet<>();
        if (title) {
            notes.stream()
                    .filter(n -> n.getTitle().toLowerCase().contains(lower))
                    .forEach(results::add);
        }
        if (content) {
            notes.stream()
                    .filter(n -> n.getContent().toLowerCase().contains(lower))
                    .forEach(results::add);
        }
        if (tag) {
            notes.stream()
                    .filter(n -> n.getTags().stream()
                            .anyMatch(t -> t.name().toLowerCase().contains(lower)))
                    .forEach(results::add);
        }
        return new ArrayList<>(results);
    }

    /**
     * Toggles the pinned status of a note.
     *
     * @param note The note to toggle the pinned status of
     */
    public void togglePin(@NonNull Note note) {
        note.setPinned(!note.isPinned());
        Persistence.saveNotes(ctx, notes);
    }

    /**
     * Deletes all notes from the library.
     * This operation cannot be undone.
     */
    public void deleteAllNotes() {
        notes.clear();
        recentlyDeletedNote = null;
        tagManager.cleanupUnusedTags();
        Persistence.saveNotes(ctx, notes);
    }

    /**
     * Updates notification settings for a note and persists the changes.
     * @param note The note to update
     * @param enabled Whether notifications are enabled
     * @param date The notification date/time
     */
    public void updateNoteNotificationSettings(@NonNull Note note, boolean enabled, Date date) {
        note.setNotificationsEnabled(enabled);
        note.setNotificationDate(date);
        Persistence.saveNotes(ctx, notes);
    }

    /**
     * Updates the last modified date of a note.
     *
     * @param note The note to update
     */
    private void updateNoteDate(@NonNull Note note) {
        note.setLastModified(new Date());
    }

    /**
     * Ensures all notes have a stable unique ID. Assigns a UUID to any note missing an ID
     * and persists the updated list once if changes were made.
     */
    private void ensureNoteIds() {
        boolean changed = false;
        for (Note n : notes) {
            if (n.getId() == null || n.getId().trim().isEmpty()) {
                n.setId(java.util.UUID.randomUUID().toString());
                changed = true;
            }
        }
        if (changed) {
            Persistence.saveNotes(ctx, notes);
        }
    }
}
