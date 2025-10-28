package com.example.quicknotes.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.util.Date

/**
 * NoteLibrary manages the collection of notes and their associated operations.
 * It acts as the Model in the MVC architecture, providing methods to add, edit, delete, search, and manage notes.
 */
class NoteLibrary(ctx: Context) {
    /**
     * Returns the application context associated with this library.
     */
    val context: Context = ctx.applicationContext
    
    private val notes: MutableList<Note> = mutableListOf()
    private var recentlyDeletedNote: Note? = null

    /**
     * Returns the ManageTags instance for tag management.
     */
    val manageTags: TagManager

    init {
        notes.addAll(Persistence.loadNotes(context))
        ensureNoteIds()
        this.manageTags = TagManager(this)
    }

    /**
     * Returns a snapshot list of all notes in the library.
     */
    fun getNotes(): List<Note> = notes.toList()

    /**
     * Adds a new note to the library.
     */
    fun addNote(note: Note) {
        val title = note.title.trim()
        if (title.isEmpty()) return
        
        // Check if a note with the same title already exists
        if (notes.any { it.title.equals(title, ignoreCase = true) }) {
            return
        }
        
        updateNoteDate(note)
        
        val aiMode = manageTags.isAiMode
        val confirmAi = TagSettingsManager(context).isAiConfirmationEnabled
        
        when {
            aiMode && confirmAi -> {
                // If AI confirmation is enabled but we're offline, fall back to simple tagging now
                if (!isOnline) {
                    manageTags.simpleAutoTag(note, manageTags.autoTagLimit)
                }
                // Otherwise, suggestions UI will handle user confirmation; do not auto-apply here
            }
            aiMode -> {
                manageTags.aiAutoTag(note, manageTags.autoTagLimit)
            }
            else -> {
                // Always run simple tagging when AI mode is off
                manageTags.simpleAutoTag(note, manageTags.autoTagLimit)
            }
        }
        
        notes.add(note)
        Persistence.saveNotes(context, notes)
    }

    /**
     * Deletes a note from the library.
     */
    fun deleteNote(note: Note) {
        if (notes.remove(note)) {
            recentlyDeletedNote = note
            Persistence.saveNotes(context, notes)
        }
    }

    /**
     * Undoes the last delete operation, restoring the most recently deleted note.
     */
    fun undoDelete(): Boolean {
        val deletedNote = recentlyDeletedNote ?: return false
        
        notes.add(deletedNote)
        updateNoteDate(deletedNote)
        Persistence.saveNotes(context, notes)
        recentlyDeletedNote = null
        return true
    }

    /**
     * Searches for notes by title, content, or tags, in any combination.
     */
    fun searchNotes(
        query: String,
        title: Boolean,
        content: Boolean,
        tag: Boolean
    ): List<Note> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return getNotes()
        }
        
        val lowerQuery = trimmedQuery.lowercase()
        val results = mutableSetOf<Note>()
        
        if (title) {
            results.addAll(
                notes.filter { it.title.lowercase().contains(lowerQuery) }
            )
        }
        
        if (content) {
            results.addAll(
                notes.filter { it.content.lowercase().contains(lowerQuery) }
            )
        }
        
        if (tag) {
            results.addAll(
                notes.filter { note ->
                    note.tags.any { tag ->
                        tag.name.lowercase().contains(lowerQuery)
                    }
                }
            )
        }
        
        return results.toList()
    }

    /**
     * Toggles the pinned status of a note.
     */
    fun togglePin(note: Note) {
        note.isPinned = !note.isPinned
        Persistence.saveNotes(context, notes)
    }

    /**
     * Deletes all notes from the library.
     * This operation cannot be undone.
     */
    fun deleteAllNotes() {
        notes.clear()
        recentlyDeletedNote = null
        manageTags.cleanupUnusedTags()
        Persistence.saveNotes(context, notes)
    }

    /**
     * Updates notification settings for a note and persists the changes.
     */
    fun updateNoteNotificationSettings(note: Note, enabled: Boolean, date: Date?) {
        note.isNotificationsEnabled = enabled
        note.notificationDate = date
        Persistence.saveNotes(context, notes)
    }

    /**
     * Updates the last modified date of a note.
     */
    private fun updateNoteDate(note: Note) {
        note.lastModified = Date()
    }

    /**
     * Ensures all notes have a stable unique ID. 
     * Since Note.id is immutable, we don't need to check for empty IDs
     * as they are always generated during Note construction.
     */
    private fun ensureNoteIds() {
        // Note: Since Note.id is a val property that's always initialized with UUID.randomUUID(),
        // we don't need to check for empty IDs. All notes should already have valid IDs.
        // This method is kept for compatibility but doesn't need to do anything.
    }

    /**
     * Checks if the device is currently online.
     */
    private val isOnline: Boolean
        get() {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network)
            return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }
}