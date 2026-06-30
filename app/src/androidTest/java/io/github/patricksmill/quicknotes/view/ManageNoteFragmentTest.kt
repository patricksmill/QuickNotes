package io.github.patricksmill.quicknotes.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import io.github.patricksmill.quicknotes.controller.ControllerActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ManageNoteFragmentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ControllerActivity>()

    @Before
    fun clearData() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteFile("notes.json")
    }

    @Test
    fun addNoteTest() {
        composeRule.onNodeWithTag("addNoteFab").performClick()
        composeRule.onNodeWithText("Note Name").performTextInput("Test Note")
        composeRule.onNodeWithText("Note Content").performTextInput("This is a test note")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithText("Test Note").assertIsDisplayed()
        composeRule.onNodeWithText("This is a test note").assertIsDisplayed()
    }

    @Test
    fun addNoteNoContentTest() {
        composeRule.onNodeWithTag("addNoteFab").performClick()
        composeRule.onNodeWithText("Note Name").performTextInput("This note should not be saved")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithText("Missing Item Field Error").assertIsDisplayed()
    }
}
