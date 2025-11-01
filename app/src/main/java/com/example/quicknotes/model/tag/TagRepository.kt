package com.example.quicknotes.model.tag

import android.content.Context
import androidx.annotation.ColorRes
import com.example.quicknotes.R
import com.example.quicknotes.model.Persistence
import com.example.quicknotes.model.note.Note
import com.example.quicknotes.model.note.NoteLibrary
import java.util.Collections
import java.util.Random
import java.util.function.Supplier
import java.util.stream.Collectors

/**
 * TagRepository centralizes tag operations and color management.
 * - Assigns/renames/merges/deletes tags across the entire library
 * - Maintains the persistent mapping of tag name -> color resource id
 * - Exposes available color options from resources
 */
class TagRepository(
    ctx: Context,
    private val noteLibrary: NoteLibrary
) {
    private val appContext: Context = ctx.applicationContext

    @JvmRecord
    data class ColorOption(@JvmField val name: String, @JvmField @field:ColorRes val resId: Int)

    @JvmField
    val availableColors: MutableList<ColorOption?>

    private val colorMap: MutableMap<String?, Int?>
    private val random = Random()

    init {
        this.availableColors = loadAvailableColors()
        this.colorMap = Persistence.loadTagMap(appContext)
    }

    // ----- Public API: Colors -----
    fun getTagColorRes(tagName: String): Int {
        val key = tagName.trim { it <= ' ' }
        var res = colorMap[key]
        if (res == null || res == 0) {
            res = assignRandomColor()
            colorMap[key] = res
            saveColorMap()
        }
        return res
    }

    fun setTagColor(tagName: String, @ColorRes resId: Int) {
        if (tagName.trim { it <= ' ' }.isEmpty()) return
        colorMap[tagName.trim { it <= ' ' }] = resId
        saveColorMap()
    }

    fun cleanupUnusedColors(usedTagNames: MutableSet<String?>) {
        if (colorMap.keys.removeIf { k: String? -> !usedTagNames.contains(k) }) {
            saveColorMap()
        }
    }

    // ----- Public API: Tag operations -----
    fun setTag(note: Note, name: String) {
        if (name.trim { it <= ' ' }.isEmpty()) return
        val tagName = name.trim { it <= ' ' }
        val colorRes = getTagColorRes(tagName)
        val tag = Tag(tagName, colorRes)
        note.setTag(tag)
        Persistence.saveNotes(appContext, noteLibrary.getNotes())
    }

    fun setTags(note: Note, names: MutableList<String?>) {
        var changed = false
        for (name in names) {
            if (name == null) continue
            val trimmed = name.trim { it <= ' ' }
            if (trimmed.isEmpty()) continue
            val colorRes = getTagColorRes(trimmed)
            val tag = Tag(trimmed, colorRes)
            val before = note.tags.size
            note.setTag(tag)
            if (note.tags.size != before) changed = true
        }
        if (changed) {
            Persistence.saveNotes(appContext, noteLibrary.getNotes())
        }
    }

    val allTags: MutableSet<Tag>
        get() {
            val tagNames = extractAllTagNames()
            return createTagsWithColors(tagNames)
        }

    fun cleanupUnusedTags() {
        val usedTagNames = extractAllTagNames()
        cleanupUnusedColors(usedTagNames)
    }

    val allTagNames: MutableSet<String?>
        get() = extractAllTagNames()

    fun renameTag(oldName: String, newName: String) {
        val from = oldName.trim { it <= ' ' }
        val to = newName.trim { it <= ' ' }
        if (from.isEmpty() || to.isEmpty() || from.equals(to, ignoreCase = true)) return

        val colorRes = getTagColorRes(from)
        val newTag = Tag(to, colorRes)

        var changed = false
        for (note in noteLibrary.getNotes()) {
            val toRemove = mutableListOf<Tag>()
            for (t in note.tags) {
                if (t.name.equals(from, ignoreCase = true)) toRemove.add(t)
            }
            if (toRemove.isNotEmpty()) {
                note.tags.removeAll(toRemove)
                note.setTag(newTag)
                changed = true
            }
        }

        if (from != to) {
            setTagColor(to, colorRes)
        }
        cleanupUnusedTags()

        if (changed) {
            Persistence.saveNotes(appContext, noteLibrary.getNotes())
        }
    }

    fun deleteTag(tagName: String) {
        val key = tagName.trim { it <= ' ' }
        if (key.isEmpty()) return

        var changed = false
        for (note in noteLibrary.getNotes()) {
            val toRemove = mutableListOf<Tag>()
            for (t in note.tags) {
                if (t.name.equals(key, ignoreCase = true)) toRemove.add(t)
            }
            if (toRemove.isNotEmpty()) {
                note.tags.removeAll(toRemove)
                changed = true
            }
        }

        cleanupUnusedTags()

        if (changed) {
            Persistence.saveNotes(appContext, noteLibrary.getNotes())
        }
    }

    fun mergeTags(sourceNames: MutableCollection<String?>, targetName: String) {
        if (sourceNames.isEmpty()) return
        val target = targetName.trim { it <= ' ' }
        if (target.isEmpty()) return

        val targetColor = getTagColorRes(target)
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
                if (sources.contains(t.name)) toRemove.add(t)
            }
            if (toRemove.isNotEmpty()) {
                note.tags.removeAll(toRemove)
                note.setTag(targetTag)
                noteChanged = true
            }
            if (noteChanged) changed = true
        }

        cleanupUnusedTags()

        if (changed) {
            Persistence.saveNotes(appContext, noteLibrary.getNotes())
        }
    }

    // ----- Internals -----
    private fun loadAvailableColors(): MutableList<ColorOption?> {
        val names = appContext.resources.getStringArray(R.array.tag_color_names)
        val ta = appContext.resources.obtainTypedArray(R.array.tag_color_resources)
        try {
            val opts: MutableList<ColorOption?> = ArrayList()
            var i = 0
            while (i < ta.length() && i < names.size) {
                val id = ta.getResourceId(i, 0)
                if (id != 0) opts.add(ColorOption(names[i], id))
                i++
            }
            return Collections.unmodifiableList<ColorOption?>(opts)
        } finally {
            ta.recycle()
        }
    }

    private fun assignRandomColor(): Int {
        return availableColors[random.nextInt(availableColors.size)]!!.resId
    }

    private fun saveColorMap() {
        Persistence.saveTagMap(appContext, colorMap)
    }

    private fun extractAllTagNames(): MutableSet<String?> {
        return noteLibrary.getNotes().stream()
            .flatMap { note: Note -> note.tags.stream() }
            .map(Tag::name)
            .collect(Collectors.toCollection(Supplier { LinkedHashSet() }))
    }

    private fun createTagsWithColors(tagNames: MutableSet<String?>): MutableSet<Tag> {
        return tagNames.asSequence()
            .filterNotNull()
            .filter { it.isNotBlank() }
            .map { name -> Tag(name, getTagColorRes(name)) }
            .toMutableSet()
    }
}


