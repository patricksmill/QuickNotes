package io.github.patricksmill.quicknotes.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import io.github.patricksmill.quicknotes.controller.ControllerActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ControllerActivity>()

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteFile("notes.json")
        composeRule.onNodeWithTag("settingsButton").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun testSettingsDisplayed() {
        composeRule.onNodeWithText("AI Provider").assertIsDisplayed()
        composeRule.onNodeWithText("OpenAI API Key").assertIsDisplayed()
        composeRule.onNodeWithText("Model Library").assertIsDisplayed()
        composeRule.onNodeWithText("OpenAI Model").assertIsDisplayed()
        composeRule.onNodeWithText("Auto-Tag Limit").assertIsDisplayed()
        composeRule.onNodeWithText("Manage notifications").assertIsDisplayed()
        composeRule.onNodeWithText("Delete All Notes").assertIsDisplayed()
    }

    @Test
    fun testSwitchToClaudeUpdatesFields() {
        composeRule.onNodeWithText("AI Provider").performClick()
        composeRule.onNodeWithText("Claude").performClick()
        composeRule.onNodeWithText("Claude API Key").assertIsDisplayed()
        composeRule.onNodeWithText("Claude Model").assertIsDisplayed()
    }

    @Test
    fun testSwitchToCustomShowsEndpointField() {
        composeRule.onNodeWithText("AI Provider").performClick()
        composeRule.onNodeWithText("Custom").performClick()
        composeRule.onNodeWithText("Custom API Key").assertIsDisplayed()
        composeRule.onNodeWithText("Custom API Base URL").assertIsDisplayed()
    }

    @Test
    fun testModelLibraryShowsCuratedModels() {
        composeRule.onNodeWithText("Model Library").performClick()
        composeRule.onNodeWithText("GPT-5 mini").assertIsDisplayed()
        composeRule.onNodeWithText("GPT-5.2").assertIsDisplayed()
        composeRule.onNodeWithText("GPT-4.1").assertIsDisplayed()
    }
}
