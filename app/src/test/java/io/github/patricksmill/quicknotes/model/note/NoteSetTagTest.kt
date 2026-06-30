package io.github.patricksmill.quicknotes.model.note

import io.github.patricksmill.quicknotes.model.tag.Tag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoteSetTagTest {
    @Test
    fun setTagReplacesExistingTagWithSameName() {
        val note = Note("Title", "body")
        note.setTag(Tag("hot", 0x7f060001))
        note.setTag(Tag("hot", 0x7f060002))

        assertEquals(1, note.tags.size)
        assertEquals(0x7f060002, note.tags.first().colorResId)
    }

    @Test
    fun setTagReplacesExistingTagCaseInsensitively() {
        val note = Note("Title", "body")
        note.setTag(Tag("Hot", 0x7f060001))
        note.setTag(Tag("hot", 0x7f060002))

        assertEquals(1, note.tags.size)
        assertEquals(0x7f060002, note.tags.first().colorResId)
    }
}
