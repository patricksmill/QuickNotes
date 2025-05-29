package com.example.quicknotes.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

class NoteTest {

    private Note note;
    private final String initialTitle = "Test Title";
    private final String initialContent = "Initial content";

    @BeforeEach
    public void setup() {
        note = new Note(initialTitle, initialContent, null);
    }

    @Test
    public void getTitle() {
        assertEquals(initialTitle, note.getTitle());
    }

    @Test
    public void setTitle() {
        String newTitle = "Updated Title";
        note.setTitle(newTitle);
        assertEquals(newTitle, note.getTitle());
    }

    @Test
    public void getContent() {
        assertEquals(initialContent, note.getContent());
    }

    @Test
    public void setContent() {
        String newContent = "Updated content";
        note.setContent(newContent);
        assertEquals(newContent, note.getContent());
    }

    @Test
    public void testToString() {
        assertTrue(note.toString().contains(initialTitle));
        assertTrue(note.toString().contains(initialContent));
    }

    @Test
    public void getTags_emptyInitially() {
        assertTrue(note.getTags().isEmpty());
    }

    @Test
    public void setTag_addsTag() {
        Tag tag = new Tag("Urgent", 2);
        note.setTag(tag);
        assertTrue(note.getTags().contains(tag));
    }

    @Test
    public void getLastModified_changesAfterEdit() throws InterruptedException {
        Date before = note.getLastModified();
        Thread.sleep(10);
        note.setContent("new");
        assertTrue(note.getLastModified().after(before));
    }

    @Test
    public void getTagNames_returnsCorrectNames() {
        Tag tag1 = new Tag("Work", 0);
        Tag tag2 = new Tag("Ideas", 0);
        note.setTag(tag1);
        note.setTag(tag2);

        var tagNames = note.getTagNames();
        assertTrue(tagNames.contains("Work"));
        assertTrue(tagNames.contains("Ideas"));
        assertEquals(2, tagNames.size());
    }

    @Test
    public void setLastModified_setsManually() {
        Date date = new Date(0);
        note.setLastModified(date);
        assertEquals(date, note.getLastModified());
    }

    @Test
    public void isPinned_defaultsFalseAndSetsTrue() {
        assertFalse(note.isPinned());
        note.setPinned(true);
        assertTrue(note.isPinned());
    }

    @Test
    public void isNotificationsEnabled_defaultsFalseAndSetsTrue() {
        assertFalse(note.isNotificationsEnabled());
        note.setNotificationsEnabled(true);
        assertTrue(note.isNotificationsEnabled());
    }

    @Test
    public void getNotificationDate_setAndGet() {
        Date date = new Date();
        note.setNotificationDate(date);
        assertEquals(date, note.getNotificationDate());
    }
}
