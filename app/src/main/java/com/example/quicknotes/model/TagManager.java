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
    private final android.content.Context ctx;
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
        this.ctx = ctx;
        
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
     * Renames a tag across all notes.
     */
    public void renameTag(@NonNull String oldName, @NonNull String newName) {
        operationsManager.renameTag(oldName, newName);
    }

    /**
     * Deletes a tag from all notes.
     */
    public void deleteTag(@NonNull String tagName) {
        operationsManager.deleteTag(tagName);
    }

    /**
     * Merges source tags into a target tag across all notes.
     */
    public void mergeTags(@NonNull java.util.Collection<String> sources, @NonNull String target) {
        operationsManager.mergeTags(sources, target);
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
        android.util.Log.d("AutoTagging", "Invoking simpleAutoTag, limit=" + limit + ", note=" + note.getTitle());
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
            android.util.Log.d("AutoTagging", "AI not configured. Falling back to simple tagging.");
            simpleAutoTag(note, limit);
            return;
        }
        if (!isOnline()) {
            // Offline fallback to simple tagging with user notification
            android.util.Log.d("AutoTagging", "Offline detected. Falling back to simple tagging.");
            simpleAutoTag(note, limit);
            android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
            h.post(() -> android.widget.Toast.makeText(ctx, "Offline: using keyword tagging", android.widget.Toast.LENGTH_SHORT).show());
            return;
        }

        String apiKey = settingsManager.getApiKey();
        Set<String> existingTagNames = operationsManager.getAllTagNames();
        
        autoTaggingService.performAiAutoTag(note, limit, apiKey, existingTagNames, 
            tagName -> operationsManager.setTag(note, tagName));
    }

    /**
     * Requests AI tag suggestions without applying them.
     */
    public void aiSuggestTags(@NonNull Note note, int limit, @NonNull java.util.function.Consumer<java.util.List<String>> onSuggestions,
                              @NonNull java.util.function.Consumer<String> onError) {
        if (!settingsManager.isAiTaggingConfigured()) {
            onSuggestions.accept(java.util.Collections.emptyList());
            return;
        }
        if (!isOnline()) {
            android.util.Log.d("AutoTagging", "Offline detected. Suggest will return error.");
            onError.accept("Offline");
            return;
        }
        String apiKey = settingsManager.getApiKey();
        Set<String> existingTagNames = operationsManager.getAllTagNames();
        autoTaggingService.performAiSuggest(note, limit, apiKey, existingTagNames,
                new AutoTaggingService.TagSuggestionsCallback() {
                    @Override
                    public void onSuggestions(@NonNull java.util.List<String> suggestions) {
                        onSuggestions.accept(suggestions);
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        onError.accept(message);
                    }
                });
    }

    private boolean isOnline() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;
        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
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
