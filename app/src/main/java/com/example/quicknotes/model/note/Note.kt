package com.example.quicknotes.model.note

import com.example.quicknotes.model.tag.Tag
import java.util.Date
import java.util.UUID

/**
 * Represents a note in the QuickNotes application.
 * Contains title, content, tags, and metadata like creation/modification dates.
 */
class Note(
    var title: String = "",
    var content: String = "",
    tags: Set<Tag>? = null
) {
    val id: String = UUID.randomUUID().toString()
    val tags: MutableSet<Tag> = tags?.toMutableSet() ?: mutableSetOf()
    var lastModified: Date = Date()
    var isNotificationsEnabled: Boolean = false
    var notificationDate: Date? = null
    var isPinned: Boolean = false

    /**
     * Adds a tag to the note.
     */
    fun setTag(tag: Tag) {
        tags.add(tag)
    }

    /**
     * Gets a list of tag names from the note's tags.
     */
    val tagNames: List<String>
        get() = tags.map { it.name }

    /**
     * Returns a string representation of the note.
     */
    override fun toString(): String {
        return "$title: $content"
    }

    /**
     * Checks equality based on the note's ID.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Note) return false
        return id == other.id
    }

    /**
     * Returns hash code based on the note's ID.
     */
    override fun hashCode(): Int {
        return id.hashCode()
    }
}