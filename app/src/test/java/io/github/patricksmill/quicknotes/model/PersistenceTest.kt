package io.github.patricksmill.quicknotes.model

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
class PersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        File(context.filesDir, "notes.json").delete()
        File(context.filesDir, "tag_lookup.json").delete()
    }

    @Test
    fun loadNotesReturnsEmptyListWhenFileMissing() {
        assertTrue(Persistence.loadNotes(context).isEmpty())
    }

    @Test
    fun saveAndLoadTagMapRoundTrip() {
        val tagMap = mutableMapOf<String?, Int?>(
            "work" to 0xFF0000,
            "personal" to 0x00FF00
        )

        Persistence.saveTagMap(context, tagMap)
        val loaded = Persistence.loadTagMap(context)

        assertEquals(2, loaded.size)
        assertEquals(0xFF0000, loaded["work"])
        assertEquals(0x00FF00, loaded["personal"])
    }
}
