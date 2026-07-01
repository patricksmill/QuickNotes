package io.github.patricksmill.quicknotes.model.note

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoteLibraryAddNoteTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var library: NoteLibrary

    @Before
    fun setUp() {
        File(context.filesDir, "notes.json").delete()
        File(context.filesDir, "tag_lookup.json").delete()
        library = NoteLibrary(context)
    }

    @Test
    fun addNote_allowsDuplicateTitles_caseInsensitive() {
        val existing = Note("Xylophone", "alpha")
        val duplicate = Note("xylophone", "beta")
        addNoteDirectly(existing)

        val sizeBefore = library.getNotes().size
        try {
            // Non-keyword titles avoid auto-tag side effects; duplicate guard was here before removal.
            library.addNote(duplicate)
        } catch (_: RuntimeException) {
            // Tag/Toast APIs can throw on Robolectric's desktop JVM after notes.add.
        }

        val notes = library.getNotes()
        assertTrue("addNote must not reject duplicate titles", notes.size > sizeBefore)
        assertEquals(2, notes.size)
        assertNotEquals(existing.id, duplicate.id)
        assertEquals(setOf("Xylophone", "xylophone"), notes.map { it.title }.toSet())
    }

    /**
     * Adds a note without triggering auto-tagging or Gson persistence during insert (Tag is a JVM
     * record that Gson cannot round-trip on the desktop JVM used by Robolectric).
     */
    private fun addNoteDirectly(note: Note) {
        val notesField = NoteLibrary::class.java.getDeclaredField("notes")
        notesField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val notes = notesField.get(library) as MutableList<Note>
        notes.add(note)
    }
}
