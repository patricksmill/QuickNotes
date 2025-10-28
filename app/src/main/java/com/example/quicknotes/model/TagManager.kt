package com.example.quicknotes.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.quicknotes.model.AutoTaggingService.TagAssignmentCallback
import com.example.quicknotes.model.AutoTaggingService.TagSuggestionsCallback
import com.example.quicknotes.model.TagColorManager.ColorOption
import java.util.function.Consumer

/**
 * ManageTags serves as a facade for tag-related operations.
 * It coordinates between TagColorManager, TagOperationsManager, AutoTaggingService, and TagSettingsManager
 * to provide a unified interface for tag management in the NoteLibrary.
 */
class TagManager(noteLibrary: NoteLibrary) {
    private val ctx: Context
    private val colorManager: TagColorManager
    private val operationsManager: TagOperationsManager
    private val autoTaggingService: AutoTaggingService
    private val settingsManager: TagSettingsManager

    /**
     * Constructs a ManageTags instance for the given NoteLibrary.
     *
     * @param noteLibrary The NoteLibrary to manage tags for.
     */
    init {
        val ctx = noteLibrary.context
        this.ctx = ctx


        // Initialize component managers
        this.colorManager = TagColorManager(ctx)
        this.operationsManager = TagOperationsManager(ctx, noteLibrary, colorManager)
        this.autoTaggingService = AutoTaggingService(ctx, operationsManager)
        this.settingsManager = TagSettingsManager(ctx)


        // Initialize existing tag colors
        initializeExistingTagColors()
    }

    val availableColors: MutableList<ColorOption?>
        /**
         * Returns the list of available color options for tags.
         *
         * @return List of ColorOption
         */
        get() = colorManager.availableColors


    /**
     * Sets the color resource ID for a given tag name.
     *
     * @param tagName The name of the tag
     * @param resId   The color resource ID
     */
    fun setTagColor(tagName: String, resId: Int) {
        colorManager.setTagColor(tagName, resId)
    }

    /**
     * Sets multiple tags for the given note in a single operation.
     * @param note The note to tag
     * @param names List of tag names
     */
    fun setTags(note: Note, names: MutableList<String?>) {
        operationsManager.setTags(note, names)
    }

    val allTags: MutableSet<Tag>
        /**
         * Returns all tags used in the note library.
         *
         * @return Set of Tag objects
         */
        get() = operationsManager.allTags

    /**
     * Removes color assignments for tags that are no longer used in any note.
     */
    fun cleanupUnusedTags() {
        operationsManager.cleanupUnusedTags()
    }

    /**
     * Renames a tag across all notes.
     */
    fun renameTag(oldName: String, newName: String) {
        operationsManager.renameTag(oldName, newName)
    }

    /**
     * Deletes a tag from all notes.
     */
    fun deleteTag(tagName: String) {
        operationsManager.deleteTag(tagName)
    }

    /**
     * Merges source tags into a target tag across all notes.
     */
    fun mergeTags(sources: MutableCollection<String?>, target: String) {
        operationsManager.mergeTags(sources, target)
    }

    val isAiMode: Boolean
        /**
         * Checks if AI-powered auto-tagging is enabled.
         *
         * @return true if AI mode is enabled, false for keyword-based tagging
         */
        get() = settingsManager.isAiMode

    val autoTagLimit: Int
        /**
         * Gets the maximum number of tags to assign during auto-tagging.
         *
         * @return The auto-tag limit
         */
        get() = settingsManager.autoTagLimit

    /**
     * Performs auto-tagging on a note using the configured strategy.
     * Uses AI tagging if configured and available, otherwise falls back to keyword tagging.
     *
     * @param note  The note to auto-tag
     * @param limit The maximum number of tags to assign
     */
    fun simpleAutoTag(note: Note, limit: Int) {
        Log.d("AutoTagging", "Invoking simpleAutoTag, limit=" + limit + ", note=" + note.title)
        autoTaggingService.performSimpleAutoTag(note, limit)
    }

    /**
     * Performs AI-powered auto-tagging on a note.
     *
     * @param note  The note to auto-tag
     * @param limit The maximum number of tags to assign
     */
    fun aiAutoTag(note: Note, limit: Int) {
        if (!settingsManager.isAiTaggingConfigured) {
            // Fall back to simple tagging if AI is not configured
            Log.d("AutoTagging", "AI not configured. Falling back to simple tagging.")
            simpleAutoTag(note, limit)
            return
        }
        if (!this.isOnline) {
            // Offline fallback to simple tagging with user notification
            Log.d("AutoTagging", "Offline detected. Falling back to simple tagging.")
            simpleAutoTag(note, limit)
            val h = Handler(Looper.getMainLooper())
            h.post {
                Toast.makeText(
                    ctx,
                    "Offline: using keyword tagging",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        val apiKey = settingsManager.apiKey ?: return
        val existingTagNames = operationsManager.allTagNames
            .filterNotNull()
            .toSet()

        autoTaggingService.performAiAutoTag(
            note, limit, apiKey, existingTagNames,
            object : TagAssignmentCallback {
                override fun onTagAssigned(tagName: String) {
                    operationsManager.setTag(note, tagName)
                }
            })
    }

    /**
     * Requests AI tag suggestions without applying them.
     */
    fun aiSuggestTags(
        note: Note, limit: Int, onSuggestions: Consumer<MutableList<String?>?>,
        onError: Consumer<String?>
    ) {
        if (!settingsManager.isAiTaggingConfigured) {
            onSuggestions.accept(mutableListOf())
            return
        }
        if (!this.isOnline) {
            Log.d("AutoTagging", "Offline detected. Suggest will return error.")
            onError.accept("Offline")
            return
        }
        val apiKey = settingsManager.apiKey
        val existingTagNames = operationsManager.allTagNames
            .filterNotNull()
            .toSet()
        
        autoTaggingService.performAiSuggest(
            note, limit, apiKey, existingTagNames,
            object : TagSuggestionsCallback {
                override fun onSuggestions(suggestions: List<String>) {
                    onSuggestions.accept(suggestions.map { it as String? }.toMutableList())
                }

                override fun onError(message: String) {
                    onError.accept(message)
                }
            })
    }

    private val isOnline: Boolean
        get() {
            val cm =
                ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (cm == null) return false
            val network = cm.activeNetwork
            if (network == null) return false
            val caps = cm.getNetworkCapabilities(network)
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

    /**
     * Initializes colors for existing tags in the note library.
     */
    private fun initializeExistingTagColors() {
        val existingTagNames = operationsManager.allTagNames.filterNotNull()
        for (tagName in existingTagNames) {
            colorManager.getTagColorRes(tagName) // This will assign a color if not already assigned
        }
    }
}
