package io.github.patricksmill.quicknotes.model.tag

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TagSettingsManagerTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        clearPrefs()
    }

    @After
    fun tearDown() {
        clearPrefs()
    }

    @Test
    fun legacyCustomEndpointPromotesCustomProvider() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit()
            .putString("openai_api_base_url", "https://example.com/v1")
            .putString("pref_ai_model", "legacy-model")
            .commit()

        val settings = TagSettingsManager(context)

        assertEquals(TagSettingsManager.AiProvider.CUSTOM, settings.selectedProvider)
        assertEquals("https://example.com/v1", settings.selectedAiEndpoint)
        assertEquals("legacy-model", settings.selectedAiModelKey)
    }

    @Test
    fun defaultModelsAreProviderAppropriate() {
        val settings = TagSettingsManager(context)

        assertEquals("gpt-5-mini", settings.modelFor(TagSettingsManager.AiProvider.OPENAI))
        assertEquals("claude-sonnet-4-20250514", settings.modelFor(TagSettingsManager.AiProvider.ANTHROPIC))
        assertEquals("gemini-2.5-flash", settings.modelFor(TagSettingsManager.AiProvider.GOOGLE))
        assertEquals("gpt-5-mini", settings.modelFor(TagSettingsManager.AiProvider.CUSTOM))
    }

    private fun clearPrefs() {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .clear()
            .commit()
    }
}
