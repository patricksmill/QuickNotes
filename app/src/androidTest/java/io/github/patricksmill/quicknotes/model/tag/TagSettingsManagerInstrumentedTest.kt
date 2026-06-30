package io.github.patricksmill.quicknotes.model.tag

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * API key persistence uses Android Keystore encryption and must run on device/emulator.
 */
class TagSettingsManagerInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext

    @Before
    fun setUp() {
        clearPrefs()
    }

    @After
    fun tearDown() {
        clearPrefs()
    }

    @Test
    fun providerSpecificValuesPersistIndependently() {
        val settings = TagSettingsManager(context)

        settings.setSelectedProvider(TagSettingsManager.AiProvider.OPENAI)
        settings.saveApiKey("openai-key")
        settings.saveModel(TagSettingsManager.AiProvider.OPENAI, "gpt-openai")

        settings.setSelectedProvider(TagSettingsManager.AiProvider.ANTHROPIC)
        settings.saveApiKey("claude-key")
        settings.saveModel(TagSettingsManager.AiProvider.ANTHROPIC, "claude-model")

        settings.setSelectedProvider(TagSettingsManager.AiProvider.GOOGLE)
        settings.saveApiKey("google-key")
        settings.saveModel(TagSettingsManager.AiProvider.GOOGLE, "gemini-model")

        settings.setSelectedProvider(TagSettingsManager.AiProvider.CUSTOM)
        settings.saveApiKey("custom-key")
        settings.saveCustomBaseUrl("https://example.com/v1")
        settings.saveModel(TagSettingsManager.AiProvider.CUSTOM, "custom-model")

        val reloaded = TagSettingsManager(context)

        assertEquals("openai-key", reloaded.apiKeyFor(TagSettingsManager.AiProvider.OPENAI))
        assertEquals("gpt-openai", reloaded.modelFor(TagSettingsManager.AiProvider.OPENAI))
        assertEquals("claude-key", reloaded.apiKeyFor(TagSettingsManager.AiProvider.ANTHROPIC))
        assertEquals("claude-model", reloaded.modelFor(TagSettingsManager.AiProvider.ANTHROPIC))
        assertEquals("google-key", reloaded.apiKeyFor(TagSettingsManager.AiProvider.GOOGLE))
        assertEquals("gemini-model", reloaded.modelFor(TagSettingsManager.AiProvider.GOOGLE))
        assertEquals("custom-key", reloaded.apiKeyFor(TagSettingsManager.AiProvider.CUSTOM))
        assertEquals("https://example.com/v1", reloaded.endpointFor(TagSettingsManager.AiProvider.CUSTOM))
        assertEquals("custom-model", reloaded.modelFor(TagSettingsManager.AiProvider.CUSTOM))
    }

    private fun clearPrefs() {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .clear()
            .commit()
    }
}
