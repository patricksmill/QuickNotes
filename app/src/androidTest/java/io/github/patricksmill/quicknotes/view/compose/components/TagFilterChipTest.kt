package io.github.patricksmill.quicknotes.view.compose.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.view.compose.theme.QuickNotesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagFilterChipTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysTagName() {
        composeTestRule.setContent {
            QuickNotesTheme {
                TagFilterChip(
                    name = "Work",
                    colorResId = R.color.tag_color_blue,
                    selected = false,
                    onClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
    }
}
