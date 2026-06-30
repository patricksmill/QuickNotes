package io.github.patricksmill.quicknotes.model.note

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoteLibrarySearchTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var library: NoteLibrary

    @Before
    fun setUp() {
        File(context.filesDir, "notes.json").delete()
        File(context.filesDir, "tag_lookup.json").delete()
        library = NoteLibrary(context)
    }

    @Test
    fun emptyQueryReturnsAllNotes() {
        addNoteDirectly(Note("Alpha", "first"))
        addNoteDirectly(Note("Beta", "second"))

        val results = library.searchNotes("", title = true, content = true, tag = true)

        assertEquals(2, results.size)
    }

    @Test
    fun searchByTitleIsCaseInsensitive() {
        addNoteDirectly(Note("Meeting Notes", "agenda"))

        val results = library.searchNotes("meeting", title = true, content = false, tag = false)

        assertEquals(1, results.size)
        assertEquals("Meeting Notes", results[0].title)
    }

    @Test
    fun searchByContentFindsMatchingNote() {
        addNoteDirectly(Note("Title", "unique keyword xyz"))

        val results = library.searchNotes("keyword", title = false, content = true, tag = false)

        assertEquals(1, results.size)
        assertEquals("unique keyword xyz", results[0].content)
    }

    @Test
    fun searchWithNoMatchesReturnsEmptyList() {
        addNoteDirectly(Note("Title", "content"))

        val results = library.searchNotes("nonexistent", title = true, content = true, tag = true)

        assertTrue(results.isEmpty())
    }

    /**
     * Adds a note without triggering auto-tagging or Gson persistence (Tag is a JVM record
     * that Gson cannot round-trip on the desktop JVM used by Robolectric).
     */
    private fun addNoteDirectly(note: Note) {
        val notesField = NoteLibrary::class.java.getDeclaredField("notes")
        notesField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val notes = notesField.get(library) as MutableList<Note>
        notes.add(note)
    }
}
