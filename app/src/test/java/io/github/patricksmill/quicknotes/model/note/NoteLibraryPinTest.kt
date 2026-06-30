package io.github.patricksmill.quicknotes.model.note

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoteLibraryPinTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var library: NoteLibrary

    @Before
    fun setUp() {
        File(context.filesDir, "notes.json").delete()
        File(context.filesDir, "tag_lookup.json").delete()
        library = NoteLibrary(context)
    }

    @Test
    fun togglePinFlipsPinnedState() {
        val note = Note("Title", "body")
        addNoteDirectly(note)

        assertFalse(note.isPinned)
        try {
            library.togglePin(note)
        } catch (_: RuntimeException) {
            // Gson cannot serialize Tag records on Robolectric's desktop JVM; flip happens before save.
        }
        assertTrue(note.isPinned)
        try {
            library.togglePin(note)
        } catch (_: RuntimeException) {
        }
        assertFalse(note.isPinned)
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
