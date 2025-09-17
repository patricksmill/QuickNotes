package com.example.quicknotes.model;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TagOperationsManager handles basic tag operations for notes.
 * It manages tag creation, assignment, retrieval, and filtering operations.
 */
public class TagOperationsManager {
    private final Context ctx;
    private final NoteLibrary noteLibrary;
    private final TagColorManager colorManager;

    /**
     * Constructs a TagOperationsManager instance.
     *
     * @param ctx The application context
     * @param noteLibrary The note library containing all notes
     * @param colorManager The color manager for tag colors
     */
    public TagOperationsManager(@NonNull Context ctx, @NonNull NoteLibrary noteLibrary, 
                               @NonNull TagColorManager colorManager) {
        this.ctx = ctx.getApplicationContext();
        this.noteLibrary = noteLibrary;
        this.colorManager = colorManager;
    }

    /**
     * Sets a tag for the given note, creating the tag if necessary.
     *
     * @param note The note to tag
     * @param name The tag name
     */
    public void setTag(@NonNull Note note, @NonNull String name) {
        if (name.trim().isEmpty()) return;
        
        String tagName = name.trim();
        int colorRes = colorManager.getTagColorRes(tagName);
        Tag tag = new Tag(tagName, colorRes);
        
        note.setTag(tag);
        Persistence.saveNotes(ctx, noteLibrary.getNotes());
    }

    /**
     * Adds multiple tags to the given note and persists once.
     * @param note The note to tag
     * @param names List of tag names
     */
    public void setTags(@NonNull Note note, @NonNull java.util.List<String> names) {
        boolean changed = false;
        for (String name : names) {
            if (name == null) continue;
            String trimmed = name.trim();
            if (trimmed.isEmpty()) continue;
            int colorRes = colorManager.getTagColorRes(trimmed);
            Tag tag = new Tag(trimmed, colorRes);
            int before = note.getTags().size();
            note.setTag(tag);
            if (note.getTags().size() != before) {
                changed = true;
            }
        }
        if (changed) {
            Persistence.saveNotes(ctx, noteLibrary.getNotes());
        }
    }

    /**
     * Returns all tags used in the note library.
     *
     * @return Set of Tag objects
     */
    public Set<Tag> getAllTags() {
        Set<String> tagNames = extractAllTagNames();
        return createTagsWithColors(tagNames);
    }

    /**
     * Filters notes by the given set of tag names.
     *
     * @param activeTagNames The set of tag names to filter by
     * @return List of notes containing at least one of the specified tags
     */
    public List<Note> filterNotesByTags(@NonNull Set<String> activeTagNames) {
        if (activeTagNames.isEmpty()) {
            return noteLibrary.getNotes();
        }
        return findNotesWithTags(activeTagNames);
    }

    /**
     * Removes color assignments for tags that are no longer used in any note.
     */
    public void cleanupUnusedTags() {
        Set<String> usedTagNames = extractAllTagNames();
        colorManager.cleanupUnusedColors(usedTagNames);
    }

    /**
     * Gets all unique tag names currently used in the note library.
     *
     * @return Set of tag names
     */
    public Set<String> getAllTagNames() {
        return extractAllTagNames();
    }




    /**
     * Extracts all unique tag names from the note library.
     *
     * @return Set of unique tag names
     */
    private Set<String> extractAllTagNames() {
        return noteLibrary.getNotes().stream()
                .flatMap(note -> note.getTags().stream())
                .map(Tag::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Creates Tag objects with colors for the given tag names.
     *
     * @param tagNames Set of tag names
     * @return Set of Tag objects with assigned colors
     */
    private Set<Tag> createTagsWithColors(@NonNull Set<String> tagNames) {
        return tagNames.stream()
                .map(name -> new Tag(name, colorManager.getTagColorRes(name)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Finds notes that contain at least one of the specified tags.
     *
     * @param activeTagNames Set of tag names to search for
     * @return List of matching notes
     */
    private List<Note> findNotesWithTags(@NonNull Set<String> activeTagNames) {
        List<Note> filtered = new ArrayList<>();
        
        for (Note note : noteLibrary.getNotes()) {
            if (noteHasAnyTag(note, activeTagNames)) {
                filtered.add(note);
            }
        }
        
        return filtered;
    }

    /**
     * Checks if a note has any of the specified tags.
     *
     * @param note The note to check
     * @param tagNames Set of tag names to look for
     * @return true if the note has at least one matching tag
     */
    private boolean noteHasAnyTag(@NonNull Note note, @NonNull Set<String> tagNames) {
        return note.getTags().stream()
                .anyMatch(tag -> tagNames.contains(tag.name()));
    }
} 