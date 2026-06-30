package io.github.patricksmill.quicknotes.view.compose.theme

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickNotesThemeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun themeRendersContent() {
        composeTestRule.setContent {
            QuickNotesTheme {
                Text("Compose smoke test")
            }
        }
        composeTestRule.onNodeWithText("Compose smoke test").assertIsDisplayed()
    }
}
