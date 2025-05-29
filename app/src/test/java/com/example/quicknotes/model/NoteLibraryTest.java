package com.example.quicknotes.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Comprehensive Unit Tests for the NoteLibrary class.
 *
 */
public class NoteLibraryTest {
    
    private TestNoteLibrary noteLibrary;


    @BeforeEach
    public void setUp() {
        noteLibrary = new TestNoteLibrary();
    }


    @Test
    public void getManageTags() {
        TestNoteLibrary.TestManageTags manageTags = noteLibrary.getManageTags();
        assertNotNull(manageTags, "ManageTags instance should not be null");
    }

    @Test
    public void getNotes() {
        List<Note> notes = noteLibrary.getNotes();
        assertTrue(notes.isEmpty(), " notes list should be empty");


        Note testNote = new Note("Test Copy", "Content", Collections.emptySet());
        noteLibrary.addNote(testNote);

        List<Note> notes1 = noteLibrary.getNotes();
        assertEquals(1, notes1.size());
        notes1.add(new Note("Another Note", "Content", Collections.emptySet()));

        List<Note> notes2 = noteLibrary.getNotes();
        assertEquals(1, notes2.size(), "Modifying the retrieved list should not affect the original list.");
        assertTrue(notes2.stream().anyMatch(n -> n.getTitle().equals("Test Copy")), "The original note should still be present.");
    }

    @Test
    @DisplayName("addNote - Valid note should be added successfully")
    public void addNote_validNote_returnsTrueAndAddsNote() {
        Note note1 = new Note("Test Note 1", "Content 1", Collections.emptySet());
        assertTrue(noteLibrary.addNote(note1), "addNote should return true for a new valid note.");
        assertEquals(1, noteLibrary.getNotes().size(), "Notes list size should be 1 after adding one note.");
        assertTrue(noteLibrary.getNotes().contains(note1), "Notes list should contain the added note.");
    }

    @Test
    @DisplayName("addNote - Note with null title should not be added")
    public void addNote_nullTitle_returnsFalse() {
        Note noteWithNullTitle = new Note(null, "Content", Collections.emptySet());
        assertFalse(noteLibrary.addNote(noteWithNullTitle), "addNote should return false for a note with null title.");
        assertTrue(noteLibrary.getNotes().isEmpty(), "Notes list should be empty if addNote failed.");
    }

    @Test
    @DisplayName("addNote - Note with empty title should not be added")
    public void addNote_emptyTitle_returnsFalse() {
        Note noteWithEmptyTitle = new Note("", "Content", Collections.emptySet());
        assertFalse(noteLibrary.addNote(noteWithEmptyTitle), "addNote should return false for a note with empty title.");
        assertTrue(noteLibrary.getNotes().isEmpty(), "Notes list should be empty if addNote failed.");

        Note noteWithWhitespaceTitle = new Note("   ", "Content", Collections.emptySet());
        assertFalse(noteLibrary.addNote(noteWithWhitespaceTitle), "addNote should return false for a note with whitespace-only title.");
        assertTrue(noteLibrary.getNotes().isEmpty(), "Notes list should be empty if addNote failed.");
    }

    @Test
    @DisplayName("addNote - Note with duplicate title (case-insensitive) should not be added")
    public void addNote_duplicateTitle_returnsFalse() {
        Note note1 = new Note("Unique Title", "Content 1", Collections.emptySet());
        assertTrue(noteLibrary.addNote(note1), "First note should be added successfully.");

        Note note2_sameCase = new Note("Unique Title", "Content 2", Collections.emptySet());
        assertFalse(noteLibrary.addNote(note2_sameCase), "addNote should return false for a note with the same title (same case).");
        assertEquals(1, noteLibrary.getNotes().size(), "Notes list size should remain 1 after attempting to add duplicate.");

        Note note3_differentCase = new Note("unique title", "Content 3", Collections.emptySet());
        assertFalse(noteLibrary.addNote(note3_differentCase), "addNote should return false for a note with the same title (different case).");
        assertEquals(1, noteLibrary.getNotes().size(), "Notes list size should remain 1 after attempting to add duplicate.");
    }


    @Test
    @DisplayName("deleteNote - Successfully delete an existing note")
    public void deleteNote_existingNote_returnsDeletedNoteAndRemovesFromList() {
        Note noteToDelete = new Note("Delete Me", "Content", Collections.emptySet());
        noteLibrary.addNote(noteToDelete);
        assertEquals(1, noteLibrary.getNotes().size());

        Note deletedNote = noteLibrary.deleteNote(noteToDelete);
        assertNotNull(deletedNote, "deleteNote should return the deleted note.");
        assertEquals("Delete Me", deletedNote.getTitle());
        assertTrue(noteLibrary.getNotes().isEmpty(), "Notes list should be empty after deleting the note.");

    }

    @Test
    @DisplayName("deleteNote - Attempt to delete a non-existent note")
    public void deleteNote_nonExistentNote_returnsNull() {
        Note noteInLibrary = new Note("I Exist", "Content", Collections.emptySet());
        noteLibrary.addNote(noteInLibrary);

        Note nonExistentNote = new Note("I Don't Exist", "Content", Collections.emptySet());
        Note result = noteLibrary.deleteNote(nonExistentNote);

        assertNull(result, "deleteNote should return null if the note is not found.");
        assertEquals(1, noteLibrary.getNotes().size(), "Notes list size should remain unchanged.");
    }

    @Test
    @DisplayName("undoDelete - Successfully undo a deletion")
    public void undoDelete_afterDeletion_returnsTrueAndRestoresNote() {
        Note note = new Note("To Be Deleted", "Content", Collections.emptySet());
        noteLibrary.addNote(note);
        noteLibrary.deleteNote(note);
        assertTrue(noteLibrary.getNotes().isEmpty(), "Notes list should be empty after deletion.");

        assertTrue(noteLibrary.undoDelete(), "undoDelete should return true when a note was recently deleted.");
        assertEquals(1, noteLibrary.getNotes().size(), "Notes list should contain one note after undo.");
        assertEquals("To Be Deleted", noteLibrary.getNotes().get(0).getTitle(), "Restored note should have the correct title.");
    }

    @Test
    @DisplayName("undoDelete - Attempt to undo when no deletion has occurred")
    public void undoDelete_noRecentDeletion_returnsFalse() {
        Note note = new Note("Never Deleted", "Content", Collections.emptySet());
        noteLibrary.addNote(note);

        assertFalse(noteLibrary.undoDelete(), "undoDelete should return false if no note was recently deleted.");
        assertEquals(1, noteLibrary.getNotes().size(), "Notes list size should remain unchanged.");
    }

    @Test
    @DisplayName("searchNotes - Empty query should return all notes")
    public void searchNotes_emptyQuery_returnsAllNotes() {
        Note note1 = new Note("Note One", "Content Apple", Collections.emptySet());
        Note note2 = new Note("Note Two", "Content Banana", Collections.emptySet());
        noteLibrary.addNote(note1);
        noteLibrary.addNote(note2);

        List<Note> results = noteLibrary.searchNotes("", true, true, true);
        assertEquals(2, results.size(), "Empty query should return all notes.");

        List<Note> resultsWhitespace = noteLibrary.searchNotes("   ", true, true, true);
        assertEquals(2, resultsWhitespace.size(), "Whitespace query should return all notes.");
    }

    @Test
    @DisplayName("searchNotes - By title only")
    public void searchNotes_byTitleOnly() {
        Note note1 = new Note("UniqueTitleSearch", "Some content", Collections.emptySet());
        Note note2 = new Note("Another Note", "Different content", Collections.emptySet());
        noteLibrary.addNote(note1);
        noteLibrary.addNote(note2);

        List<Note> results = noteLibrary.searchNotes("UniqueTitleSearch", true, false, false);
        assertEquals(1, results.size());
        assertEquals("UniqueTitleSearch", results.get(0).getTitle());
    }

    @Test
    @DisplayName("searchNotes - By content only")
    public void searchNotes_byContentOnly() {
        Note note1 = new Note("Content Note 1", "SpecificKeyword", Collections.emptySet());
        Note note2 = new Note("Content Note 2", "Other stuff", Collections.emptySet());
        noteLibrary.addNote(note1);
        noteLibrary.addNote(note2);

        List<Note> results = noteLibrary.searchNotes("SpecificKeyword", false, true, false);
        assertEquals(1, results.size());
        assertTrue(results.get(0).getContent().contains("SpecificKeyword"));
    }

    @Test
    @DisplayName("searchNotes - By tag only")
    public void searchNotes_byTagOnly() {
        Tag tag1 = new Tag("SearchTag", 0);
        Tag tag2 = new Tag("OtherTag", 0);
        Note note1 = new Note("Tagged Note 1", "Content", new HashSet<>(List.of(tag1)));
        Note note2 = new Note("Tagged Note 2", "Content", new HashSet<>(List.of(tag2)));
        Note note3 = new Note("Untagged Note", "Content", Collections.emptySet());
        noteLibrary.addNote(note1);
        noteLibrary.addNote(note2);
        noteLibrary.addNote(note3);

        List<Note> results = noteLibrary.searchNotes("SearchTag", false, false, true);
        assertEquals(1, results.size());
        assertTrue(results.get(0).getTags().contains(tag1));
    }

    @Test
    @DisplayName("searchNotes - No results found")
    public void searchNotes_noResults() {
        Note note1 = new Note("Note A", "Content X", Collections.emptySet());
        noteLibrary.addNote(note1);

        List<Note> results = noteLibrary.searchNotes("NonExistentQuery", true, true, true);
        assertTrue(results.isEmpty(), "Search for non-existent query should return empty list.");
    }

    @Test
    @DisplayName("searchNotes - Case-insensitive search")
    public void searchNotes_caseInsensitive() {
        Note note1 = new Note("CaSeSeNsItIvE Title", "CaSeSeNsItIvE Content", Collections.singleton(new Tag("CaSeSeNsItIvETaG", 0)));
        noteLibrary.addNote(note1);

        List<Note> titleResults = noteLibrary.searchNotes("casesensitive title", true, false, false);
        assertEquals(1, titleResults.size(), "Title search should be case-insensitive.");

        List<Note> contentResults = noteLibrary.searchNotes("casesensitive content", false, true, false);
        assertEquals(1, contentResults.size(), "Content search should be case-insensitive.");

        List<Note> tagResults = noteLibrary.searchNotes("casesensitivetag", false, false, true);
        assertEquals(1, tagResults.size(), "Tag search should be case-insensitive.");
    }

    @Test
    @DisplayName("togglePin - Successfully toggle pin status")
    public void togglePin_updatesPinnedStatus() {
        Note note = new Note("Pin Test Note", "Content", Collections.emptySet());
        noteLibrary.addNote(note);
        assertFalse(note.isPinned(), "Note should initially not be pinned.");

        // Pin the note
        noteLibrary.togglePin(note);
        assertTrue(note.isPinned(), "Note should be pinned after first toggle.");

        // Unpin the note
        noteLibrary.togglePin(note);
        assertFalse(note.isPinned(), "Note should be unpinned after second toggle.");
    }
}
