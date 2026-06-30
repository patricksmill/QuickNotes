package io.github.patricksmill.quicknotes.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.dropbox.dropshots.Dropshots
import io.github.patricksmill.quicknotes.controller.ControllerActivity
import org.junit.Rule
import org.junit.Test

class SearchNotesScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ControllerActivity>()

    @get:Rule
    val dropshots = Dropshots()

    @Test
    fun emptyNotesList() {
        composeRule.onNodeWithTag("empty_state").assertIsDisplayed()
        composeRule.scenario.onActivity { activity ->
            dropshots.assertSnapshot(activity, name = "search_notes_empty")
        }
    }
}
