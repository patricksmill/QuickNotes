package io.github.patricksmill.quicknotes.model.tag

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiModelCatalogTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val catalog = AiModelCatalog(context)

    @Test
    fun curatedSuggestionsIncludeExpectedOpenAiModels() {
        val suggestions = catalog.curatedSuggestions(TagSettingsManager.AiProvider.OPENAI)
        val ids = suggestions.map { it.id }

        assertTrue(ids.contains("gpt-5-mini"))
        assertTrue(ids.contains("gpt-5.2"))
        assertTrue(ids.contains("gpt-4.1"))
    }

    @Test
    fun mergedSuggestionsIncludeCurrentModel() {
        val merged = catalog.mergedSuggestions(
            provider = TagSettingsManager.AiProvider.OPENAI,
            currentModel = "my-custom-model"
        )

        assertTrue(merged.any { it.id == "my-custom-model" })
        assertTrue(merged.any { it.id == "gpt-5-mini" })
    }

    @Test
    fun cachedSuggestionsRoundTrip() {
        val options = listOf(
            AiModelCatalog.ModelOption("test-model", "Test Model")
        )
        catalog.saveCachedSuggestions(TagSettingsManager.AiProvider.OPENAI, options)

        val reloaded = AiModelCatalog(context).cachedSuggestions(TagSettingsManager.AiProvider.OPENAI)

        assertEquals(1, reloaded.size)
        assertEquals("test-model", reloaded[0].id)
        assertEquals("Test Model", reloaded[0].label)
    }
}
