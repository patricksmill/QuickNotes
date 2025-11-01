package com.example.quicknotes.view

import android.Manifest
import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.example.quicknotes.R
import com.example.quicknotes.controller.ControllerActivity
import com.example.quicknotes.model.note.Note
import com.example.quicknotes.model.note.NoteLibrary
import com.example.quicknotes.model.Persistence
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

class SearchNotesFragmentTest {
    private val activityRule: ActivityScenarioRule<ControllerActivity> =
        ActivityScenarioRule(ControllerActivity::class.java)

    @get:Rule
    val ruleChain: TestRule = RuleChain
        .outerRule(GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS))
        .around(activityRule)

    private val noteName = "Test Search Note"
    private val secondNoteName = "Second Test Note"
    private val secondNoteContent = "This is another searchable test note"

    private var ctx: Context? = null

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext()
        ctx!!.deleteFile("notes.json")
        skipExactAlarmMessage()
    }

    private fun addTestNotesToLibrary() {
        val noteLibrary = NoteLibrary(ctx!!)
        val noteContent = "This is a searchable test note"
        noteLibrary.addNote(Note(noteName, noteContent, emptySet()))
        noteLibrary.addNote(Note(secondNoteName, secondNoteContent, emptySet()))
        Persistence.saveNotes(ctx!!, noteLibrary.getNotes())
        activityRule.getScenario().recreate()
        // Give time for the new window to gain focus after recreate
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(700))
    }

    private fun skipExactAlarmMessage() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // If our in-app dialog shows, tap "Later" to bypass it.
        val laterBtn = device.wait(Until.findObject(By.text("Later")), 1500)
        laterBtn?.click()
    }

    private fun waitFor(delayMs: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isRoot()
            }

            override fun getDescription(): String {
                return "Wait for ${delayMs}ms."
            }

            override fun perform(uiController: androidx.test.espresso.UiController, view: View?) {
                uiController.loopMainThreadForAtLeast(delayMs)
            }
        }
    }

    @Test
    fun emptyStateTest() {
        Espresso.onView(withId(R.id.empty_state))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(withId(androidx.appcompat.R.id.search_button))
            .perform(ViewActions.click())
        Espresso.onView(withId(androidx.appcompat.R.id.search_src_text))
            .perform(ViewActions.typeText("dummyquery"), ViewActions.pressImeActionButton())
        Espresso.onView(withId(androidx.appcompat.R.id.search_close_btn))
            .perform(ViewActions.click())
        Espresso.onView(withId(R.id.empty_state))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun searchByTitleTest() {
        addTestNotesToLibrary()
        Espresso.onView(ViewMatchers.withText(noteName))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(secondNoteName))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(withId(androidx.appcompat.R.id.search_button))
            .perform(ViewActions.click())
        Espresso.onView(withId(androidx.appcompat.R.id.search_src_text))
            .perform(ViewActions.typeText(secondNoteName), ViewActions.pressImeActionButton())
        Espresso.onView(
            Matchers.allOf(
                withId(R.id.noteNameText),
                ViewMatchers.withText(secondNoteName)
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun searchByContentTest() {
        addTestNotesToLibrary()
        Espresso.onView(withId(R.id.notesRecyclerView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(withId(androidx.appcompat.R.id.search_button))
            .perform(ViewActions.click())
        Espresso.onView(withId(androidx.appcompat.R.id.search_src_text))
            .perform(ViewActions.typeText(secondNoteContent), ViewActions.pressImeActionButton())

        Espresso.onView(
            Matchers.allOf(
                withId(R.id.noteNameText),
                ViewMatchers.withText(secondNoteName)
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun noResultsTest() {
        addTestNotesToLibrary()
        Espresso.onView(withId(R.id.notesRecyclerView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(withId(androidx.appcompat.R.id.search_button)).perform(ViewActions.click())
        Espresso.onView(withId(androidx.appcompat.R.id.search_src_text))
            .perform(ViewActions.typeText("xyznonexistent123"), ViewActions.pressImeActionButton())
        Espresso.onView(withId(R.id.empty_state))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun clearSearchTest() {
        addTestNotesToLibrary()
        Espresso.onView(withId(R.id.notesRecyclerView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
            Matchers.allOf(
                withId(R.id.noteNameText),
                ViewMatchers.withText(noteName)
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
            Matchers.allOf(
                withId(R.id.noteNameText),
                ViewMatchers.withText(secondNoteName)
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(withId(androidx.appcompat.R.id.search_button)).perform(ViewActions.click())
        Espresso.onView(withId(androidx.appcompat.R.id.search_src_text))
            .perform(ViewActions.typeText("xyznonexistent123"), ViewActions.pressImeActionButton())
        Espresso.onView(withId(androidx.appcompat.R.id.search_close_btn))
            .perform(ViewActions.click())
        Espresso.onView(withId(R.id.notesRecyclerView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
            Matchers.allOf(
                withId(R.id.noteNameText),
                ViewMatchers.withText(noteName)
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
            Matchers.allOf(
                withId(R.id.noteNameText),
                ViewMatchers.withText(secondNoteName)
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}
