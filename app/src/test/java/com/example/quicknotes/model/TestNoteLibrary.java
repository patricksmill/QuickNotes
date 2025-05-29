package com.example.quicknotes.model;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A standalone implementation of NoteLibrary-like functionality for testing.
 * Does not use Android context for testing purposes, tagging systems are stubbed, identical otherwise
 */
public class TestNoteLibrary {
    private final List<Note> inMemoryNotes = new ArrayList<>();
    private Note recentlyDeletedNote;
    private final TestManageTags manageTags;

    /**
     * Creates a TestNoteLibrary with no dependencies on Android Context.
     */
    public TestNoteLibrary() {
        this.manageTags = new TestManageTags(this);
    }

    /**
     * Returns the ManageTags instance for this library.
     * @return The ManageTags instance
     */
    public TestManageTags getManageTags() {
        return manageTags;
    }

    /**
     * Returns a list of all notes in the in-memory library.
     * @return List of all notes
     */
    public List<Note> getNotes() {
        return new ArrayList<>(inMemoryNotes);
    }

    /**
     * Adds a new note to the in-memory library.
     * @param note The note to add
     * @return true if the note was added, false if a note with the same title already exists or input is invalid
     */
    public boolean addNote(@NonNull Note note) {
        if (note.getTitle() == null) return false;
        String title = note.getTitle().trim();
        if (title.isEmpty()) return false;
        if (inMemoryNotes.stream().anyMatch(n -> n.getTitle().equalsIgnoreCase(title))) {
            return false;
        }

        updateNoteDate(note);
        if (manageTags.isAiMode()) {
            manageTags.aiAutoTag(note, manageTags.getAutoTagLimit());
        } else {
            manageTags.simpleAutoTag(note, manageTags.getAutoTagLimit());
        }
        inMemoryNotes.add(note);
        return true;
    }


    /**
     * Deletes a note from the in-memory library.
     * @param note The note to delete
     * @return The deleted note, or null if the note was not found
     */
    public Note deleteNote(@NonNull Note note) {
        if (inMemoryNotes.remove(note)) {
            recentlyDeletedNote = note;
            return note;
        }
        return null;
    }

    /**
     * Undoes the last delete operation, restoring the most recently deleted note.
     * @return true if the note was successfully restored, false otherwise
     */
    public boolean undoDelete() {
        if (recentlyDeletedNote != null) {
            inMemoryNotes.add(recentlyDeletedNote);
            updateNoteDate(recentlyDeletedNote);
            recentlyDeletedNote = null;
            return true;
        }
        return false;
    }

    /**
     * Searches for notes by title, content, or tags, in any combination.
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
            inMemoryNotes.stream()
                    .filter(n -> n.getTitle().toLowerCase().contains(lower))
                    .forEach(results::add);
        }
        if (content) {
            inMemoryNotes.stream()
                    .filter(n -> n.getContent().toLowerCase().contains(lower))
                    .forEach(results::add);
        }
        if (tag) {
            inMemoryNotes.stream()
                    .filter(n -> n.getTags().stream()
                            .anyMatch(t -> t.getName().toLowerCase().contains(lower)))
                    .forEach(results::add);
        }
        return new ArrayList<>(results);
    }

    /**
     * Toggles the pinned status of a note.
     * @param note The note to toggle the pinned status of
     */
    public void togglePin(@NonNull Note note) {
        note.setPinned(!note.isPinned());
    }

    /**
     * Updates the last modified date of a note
     * @param note The note to update
     */
    private void updateNoteDate(@NonNull Note note) {
        note.setLastModified(new Date());
    }

    /**
     * Simple implementation of ManageTags for testing
     */
    public static class TestManageTags {

        public TestManageTags(TestNoteLibrary noteLibrary) {
        }

        public boolean isAiMode() {
            return false;
        }

        public int getAutoTagLimit() {
            return 3;
        }

        public void simpleAutoTag(Note note, int limit) {
        }

        public void aiAutoTag(Note note, int limit) {
        }
    }
}
