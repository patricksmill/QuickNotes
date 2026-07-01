package io.github.patricksmill.quicknotes.model.note

import androidx.test.core.app.ApplicationProvider
import io.github.patricksmill.quicknotes.model.tag.TagSettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
        TagSettingsManager(context).setAutoTagLimit(0)
        library = NoteLibrary(context)
    }

    @Test
    fun addNote_allowsDuplicateTitles_caseInsensitive() {
        val first = Note("Meeting", "first")
        val second = Note("meeting", "second")

        try {
            library.addNote(first)
            library.addNote(second)
        } catch (_: RuntimeException) {
            // Gson cannot serialize Tag records on Robolectric's desktop JVM; notes are still added.
        }

        val notes = library.getNotes()
        assertEquals(2, notes.size)
        assertNotEquals(first.id, second.id)
        assertEquals(setOf("Meeting", "meeting"), notes.map { it.title }.toSet())
    }
}
