package com.example.quicknotes.model;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Set;

/**
 * ManageTags serves as a facade for tag-related operations.
 * It coordinates between TagColorManager, TagOperationsManager, AutoTaggingService, and TagSettingsManager
 * to provide a unified interface for tag management in the NoteLibrary.
 */
public class TagManager {
    private final TagColorManager colorManager;
    private final TagOperationsManager operationsManager;
    private final AutoTaggingService autoTaggingService;
    private final TagSettingsManager settingsManager;

    /**
     * Constructs a ManageTags instance for the given NoteLibrary.
     *
     * @param noteLibrary The NoteLibrary to manage tags for.
     */
    public TagManager(@NonNull NoteLibrary noteLibrary) {
        Context ctx = noteLibrary.getContext();
        
        // Initialize component managers
        this.colorManager = new TagColorManager(ctx);
        this.operationsManager = new TagOperationsManager(ctx, noteLibrary, colorManager);
        this.autoTaggingService = new AutoTaggingService(ctx, operationsManager);
        this.settingsManager = new TagSettingsManager(ctx);
        
        // Initialize existing tag colors
        initializeExistingTagColors();
    }

    /**
     * Returns the list of available color options for tags.
     *
     * @return List of ColorOption
     */
    public List<TagColorManager.ColorOption> getAvailableColors() {
        return colorManager.getAvailableColors();
    }


    /**
     * Sets the color resource ID for a given tag name.
     *
     * @param tagName The name of the tag
     * @param resId   The color resource ID
     */
    public void setTagColor(@NonNull String tagName, int resId) {
        colorManager.setTagColor(tagName, resId);
    }

    /**
     * Sets a tag for the given note, creating the tag if necessary.
     *
     * @param note The note to tag
     * @param name The tag name
     */
    public void setTag(@NonNull Note note, @NonNull String name) {
        operationsManager.setTag(note, name);
    }

    /**
     * Sets multiple tags for the given note in a single operation.
     * @param note The note to tag
     * @param names List of tag names
     */
    public void setTags(@NonNull Note note, @NonNull java.util.List<String> names) {
        operationsManager.setTags(note, names);
    }

    /**
     * Returns all tags used in the note library.
     *
     * @return Set of Tag objects
     */
    public Set<Tag> getAllTags() {
        return operationsManager.getAllTags();
    }

    /**
     * Filters notes by the given set of tag names.
     *
     * @param activeTagNames The set of tag names to filter by
     * @return List of notes containing at least one of the specified tags
     */
    public List<Note> filterNotesByTags(Set<String> activeTagNames) {
        return operationsManager.filterNotesByTags(activeTagNames);
    }

    /**
     * Removes color assignments for tags that are no longer used in any note.
     */
    public void cleanupUnusedTags() {
        operationsManager.cleanupUnusedTags();
    }

    /**
     * Checks if AI-powered auto-tagging is enabled.
     *
     * @return true if AI mode is enabled, false for keyword-based tagging
     */
    public boolean isAiMode() {
        return settingsManager.isAiMode();
    }

    /**
     * Gets the maximum number of tags to assign during auto-tagging.
     *
     * @return The auto-tag limit
     */
    public int getAutoTagLimit() {
        return settingsManager.getAutoTagLimit();
    }

    /**
     * Performs auto-tagging on a note using the configured strategy.
     * Uses AI tagging if configured and available, otherwise falls back to keyword tagging.
     *
     * @param note  The note to auto-tag
     * @param limit The maximum number of tags to assign
     */
    public void simpleAutoTag(@NonNull Note note, int limit) {
        autoTaggingService.performSimpleAutoTag(note, limit);
    }

    /**
     * Performs AI-powered auto-tagging on a note.
     *
     * @param note  The note to auto-tag
     * @param limit The maximum number of tags to assign
     */
    public void aiAutoTag(@NonNull Note note, int limit) {
        if (!settingsManager.isAiTaggingConfigured()) {
            // Fall back to simple tagging if AI is not configured
            simpleAutoTag(note, limit);
            return;
        }

        String apiKey = settingsManager.getApiKey();
        Set<String> existingTagNames = operationsManager.getAllTagNames();
        
        autoTaggingService.performAiAutoTag(note, limit, apiKey, existingTagNames, 
            tagName -> operationsManager.setTag(note, tagName));
    }

    /**
     * Initializes colors for existing tags in the note library.
     */
    private void initializeExistingTagColors() {
        Set<String> existingTagNames = operationsManager.getAllTagNames();
        for (String tagName : existingTagNames) {
            colorManager.getTagColorRes(tagName); // This will assign a color if not already assigned
        }
    }
}
