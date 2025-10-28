package com.example.quicknotes.view

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.quicknotes.R
import com.example.quicknotes.controller.ControllerActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Instrumentation test for the SettingsFragment and TagColorSettingsFragment
 * This class tests the functionality of the settings screens,
 * including preference navigation and dialog interactions.
 */
class SettingsFragmentTest {
    @Rule
    var activityRule: ActivityScenarioRule<ControllerActivity?> =
        ActivityScenarioRule<ControllerActivity?>(ControllerActivity::class.java)

    @Before
    fun setUp() {
        val ctx = InstrumentationRegistry
            .getInstrumentation()
            .getTargetContext()
        ctx.deleteFile("notes.json")
        navigateToSettings()
    }

    /**
     * Helper method to navigate to the settings screen
     */
    private fun navigateToSettings() {
        Espresso.onView(ViewMatchers.withId(R.id.settingsButton))
            .perform(ViewActions.click())
    }


    /**
     * Test that the settings screen displays correctly
     */
    @Test
    fun testSettingsDisplayed() {
        Espresso.onView(ViewMatchers.withText("OpenAI API Key"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("Auto-Tag Limit"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("Allow Notifications"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("Delete All Notes"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("Edit Tag Colors"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    /**
     * Test that clicking on the OpenAI API key preference opens a dialog
     */
    @Test
    fun testOpenApiKeyClick() {
        Espresso.onView(ViewMatchers.withText("OpenAI API Key"))
            .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withText("OpenAI API Key"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("Cancel"))
            .perform(ViewActions.click())
    }

    /**
     * Test that clicking on the Delete All Notes preference shows a confirmation dialog
     */
    @Test
    fun testDeleteAllNotesDialog() {
        Espresso.onView(ViewMatchers.withText("Delete All Notes"))
            .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withText("Delete All Notes"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("Are you sure? This will permanently erase all your notes."))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("Cancel"))
            .perform(ViewActions.click())
    }

    /**
     * Test toggling AI mode preference
     */
    @Test
    fun testToggleAiMode() {
        Espresso.onView(ViewMatchers.withText("Use AI-powered Auto-Tagging"))
            .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withText("Use AI-powered Auto-Tagging"))
            .perform(ViewActions.click())
    }

    /**
     * Test navigation to Tag Color Settings screen
     */
    @Test
    fun testNavigateToTagColorSettings() {
        Espresso.onView(ViewMatchers.withText("Edit Tag Colors"))
            .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withText("No tags available"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.pressBack()
        Espresso.onView(ViewMatchers.withText("AI"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}