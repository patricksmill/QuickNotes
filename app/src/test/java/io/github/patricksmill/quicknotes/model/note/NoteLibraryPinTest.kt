package io.github.patricksmill.quicknotes.model.note

import androidx.test.core.app.ApplicationProvider
import io.github.patricksmill.quicknotes.model.Persistence
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
        library.addNote(note)

        assertFalse(note.isPinned)
        library.togglePin(note)
        assertTrue(note.isPinned)
        library.togglePin(note)
        assertFalse(note.isPinned)
    }

    @Test
    fun togglePinPersistsAcrossReload() {
        val note = Note("Title", "body")
        library.addNote(note)
        library.togglePin(note)

        val reloaded = NoteLibrary(context)
        val loadedNote = reloaded.getNotes().first { it.id == note.id }
        assertTrue(loadedNote.isPinned)
    }
}
