package io.github.patricksmill.quicknotes.view

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.controller.ControllerActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsFragmentTest {
    @Rule
    var activityRule: ActivityScenarioRule<ControllerActivity?> =
        ActivityScenarioRule(ControllerActivity::class.java)

    @Before
    fun setUp() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        ctx.deleteFile("notes.json")
        navigateToSettings()
    }

    private fun navigateToSettings() {
        Espresso.onView(ViewMatchers.withId(R.id.settingsButton))
            .perform(ViewActions.click())
    }

    private fun selectProvider(name: String) {
        Espresso.onView(ViewMatchers.withText("AI Provider"))
            .perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withText(name))
            .perform(ViewActions.click())
    }

    @Test
    fun testSettingsDisplayed() {
        Espresso.onView(ViewMatchers.withText("AI Provider"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("OpenAI API Key"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("Model Library"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("OpenAI Model"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("Auto-Tag Limit"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("Manage notifications"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("Delete All Notes"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun testSwitchToClaudeUpdatesFields() {
        selectProvider("Claude")

        Espresso.onView(ViewMatchers.withText("Claude API Key"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("Claude Model"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun testSwitchToCustomShowsEndpointField() {
        selectProvider("Custom")

        Espresso.onView(ViewMatchers.withText("Custom API Key"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("Custom API Base URL"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun testModelLibraryShowsCuratedModels() {
        Espresso.onView(ViewMatchers.withText("Model Library"))
            .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withText("GPT-5 mini"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("GPT-5.2"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("GPT-4.1"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun testOpenApiProviderTitleIsShownInitially() {
        Espresso.onView(ViewMatchers.withText("OpenAI API Key"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withText("OpenAI Model"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}
