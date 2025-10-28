package com.example.quicknotes.model

import android.content.Context
import java.util.function.Supplier
import java.util.stream.Collectors

/**
 * TagOperationsManager handles basic tag operations for notes.
 * It manages tag creation, assignment, retrieval, and filtering operations.
 */
class TagOperationsManager(
    ctx: Context, private val noteLibrary: NoteLibrary,
    private val colorManager: TagColorManager
) {
    private val ctx: Context = ctx.applicationContext

    /**
     * Sets a tag for the given note, creating the tag if necessary.
     *
     * @param note The note to tag
     * @param name The tag name
     */
    fun setTag(note: Note, name: String) {
        if (name.trim { it <= ' ' }.isEmpty()) return

        val tagName = name.trim { it <= ' ' }
        val colorRes = colorManager.getTagColorRes(tagName)
        val tag = Tag(tagName, colorRes)

        note.setTag(tag)
        Persistence.saveNotes(ctx, noteLibrary.getNotes())
    }

    /**
     * Adds multiple tags to the given note and persists once.
     * @param note The note to tag
     * @param names List of tag names
     */
    fun setTags(note: Note, names: MutableList<String?>) {
        var changed = false
        for (name in names) {
            if (name == null) continue
            val trimmed = name.trim { it <= ' ' }
            if (trimmed.isEmpty()) continue
            val colorRes = colorManager.getTagColorRes(trimmed)
            val tag = Tag(trimmed, colorRes)
            val before = note.tags.size
            note.setTag(tag)
            if (note.tags.size != before) {
                changed = true
            }
        }
        if (changed) {
            Persistence.saveNotes(ctx, noteLibrary.getNotes())
        }
    }

    val allTags: MutableSet<Tag>
        /**
         * Returns all tags used in the note library.
         *
         * @return Set of Tag objects
         */
        get() {
            val tagNames = extractAllTagNames()
            return createTagsWithColors(tagNames)
        }

    /**
     * Removes color assignments for tags that are no longer used in any note.
     */
    fun cleanupUnusedTags() {
        val usedTagNames = extractAllTagNames()
        colorManager.cleanupUnusedColors(usedTagNames)
    }

    val allTagNames: MutableSet<String?>
        /**
         * Gets all unique tag names currently used in the note library.
         *
         * @return Set of tag names
         */
        get() = extractAllTagNames()

    /**
     * Renames a tag across all notes and updates the color mapping key.
     * The new tag will retain the color of the old tag (or assigned if missing).
     *
     * @param oldName Existing tag name
     * @param newName New tag name
     */
    fun renameTag(oldName: String, newName: String) {
        val from = oldName.trim { it <= ' ' }
        val to = newName.trim { it <= ' ' }
        if (from.isEmpty() || to.isEmpty() || from.equals(to, ignoreCase = true)) return

        val colorRes = colorManager.getTagColorRes(from)
        val newTag = Tag(to, colorRes)

        var changed = false
        for (note in noteLibrary.getNotes()) {
            // Collect matches to remove to avoid ConcurrentModification
            val toRemove = mutableListOf<Tag>()
            for (t in note.tags) {
                if (t.name.equals(from, ignoreCase = true)) {
                    toRemove.add(t)
                }
            }
            if (toRemove.isNotEmpty()) {
                note.tags.removeAll(toRemove)
                note.setTag(newTag)
                changed = true
            }
        }

        // Move color key mapping if needed
        if (from != to) {
            colorManager.setTagColor(to, colorRes)
        }
        // Clean up unused colors after rename
        cleanupUnusedTags()

        if (changed) {
            Persistence.saveNotes(ctx, noteLibrary.getNotes())
        }
    }

    /**
     * Deletes a tag from all notes and removes its color mapping.
     *
     * @param tagName Tag to delete
     */
    fun deleteTag(tagName: String) {
        val key = tagName.trim { it <= ' ' }
        if (key.isEmpty()) return

        var changed = false
        for (note in noteLibrary.getNotes()) {
            val toRemove = mutableListOf<Tag>()
            for (t in note.tags) {
                if (t.name.equals(key, ignoreCase = true)) {
                    toRemove.add(t)
                }
            }
            if (toRemove.isNotEmpty()) {
                note.tags.removeAll(toRemove)
                changed = true
            }
        }

        // Remove color mapping if now unused
        cleanupUnusedTags()

        if (changed) {
            Persistence.saveNotes(ctx, noteLibrary.getNotes())
        }
    }

    /**
     * Merges multiple source tags into a single target tag across all notes.
     * Retains the color of the target tag (assigns if not present yet).
     *
     * @param sourceNames Tags to merge
     * @param targetName Target tag name
     */
    fun mergeTags(sourceNames: MutableCollection<String?>, targetName: String) {
        if (sourceNames.isEmpty()) return
        val target = targetName.trim { it <= ' ' }
        if (target.isEmpty()) return

        // Ensure target color exists
        val targetColor = colorManager.getTagColorRes(target)
        val targetTag = Tag(target, targetColor)

        val sources = sourceNames.asSequence()
            .filterNotNull()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals(target, ignoreCase = true) }
            .toMutableSet()
        if (sources.isEmpty()) return

        var changed = false
        for (note in noteLibrary.getNotes()) {
            var noteChanged = false
            val toRemove = mutableListOf<Tag>()
            for (t in note.tags) {
                if (sources.contains(t.name)) {
                    toRemove.add(t)
                }
            }
            if (toRemove.isNotEmpty()) {
                note.tags.removeAll(toRemove)
                note.setTag(targetTag)
                noteChanged = true
            }
            if (noteChanged) changed = true
        }

        // Clean up unused colors for removed tags
        cleanupUnusedTags()

        if (changed) {
            Persistence.saveNotes(ctx, noteLibrary.getNotes())
        }
    }

    /**
     * Extracts all unique tag names from the note library.
     *
     * @return Set of unique tag names
     */
    private fun extractAllTagNames(): MutableSet<String?> {
        return noteLibrary.getNotes().stream()
            .flatMap { note: Note -> note.tags.stream() }
            .map(Tag::name)
            .collect(Collectors.toCollection(Supplier { LinkedHashSet() }))
    }

    /**
     * Creates Tag objects with colors for the given tag names.
     *
     * @param tagNames Set of tag names
     * @return Set of Tag objects with assigned colors
     */
    private fun createTagsWithColors(tagNames: MutableSet<String?>): MutableSet<Tag> {
        return tagNames.asSequence()
            .filterNotNull()
            .filter { it.isNotBlank() }
            .map { name -> Tag(name, colorManager.getTagColorRes(name)) }
            .toMutableSet()
    }

}