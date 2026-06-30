package io.github.patricksmill.quicknotes.view

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.dropbox.dropshots.Dropshots
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.controller.ControllerActivity
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented screenshot test for the main notes list empty state.
 *
 * Record baselines on a device or emulator:
 *   ./gradlew :app:recordDebugAndroidTestScreenshots
 *
 * Verify against baselines:
 *   ./gradlew :app:connectedDebugAndroidTest
 */
class SearchNotesScreenshotTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(ControllerActivity::class.java)

    @get:Rule
    val dropshots = Dropshots()

    @Test
    fun emptyNotesList() {
        onView(withId(R.id.empty_state))
            .check(matches(isDisplayed()))
        activityRule.scenario.onActivity { activity ->
            dropshots.assertSnapshot(activity, name = "search_notes_empty")
        }
    }
}
