package com.example.quicknotes.view

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.example.quicknotes.R
import com.example.quicknotes.controller.ControllerActivity
import com.example.quicknotes.model.Note
import com.example.quicknotes.model.NoteLibrary
import com.example.quicknotes.model.Persistence
import com.example.quicknotes.model.Tag
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SearchNotesFragmentTest {
    @Rule
    val activityRule: ActivityScenarioRule<ControllerActivity?> =
        ActivityScenarioRule<ControllerActivity?>(ControllerActivity::class.java)

    private val noteName = "Test Search Note"
    private val secondNoteName = "Second Test Note"
    private val secondNoteContent = "This is another searchable test note"

    private var ctx: Context? = null

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext()
        ctx!!.deleteFile("notes.json")
    }

    private fun addTestNotesToLibrary() {
        val noteLibrary = NoteLibrary(ctx!!)
        val noteContent = "This is a searchable test note"
        noteLibrary.addNote(Note(noteName, noteContent, LinkedHashSet<Tag?>()))
        noteLibrary.addNote(Note(secondNoteName, secondNoteContent, LinkedHashSet<Tag?>()))
        Persistence.saveNotes(ctx!!, noteLibrary.getNotes())
        activityRule.getScenario().recreate()
    }

    @Test
    fun emptyStateTest() {
        Espresso.onView(ViewMatchers.withId(R.id.empty_state))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(withId(androidx.appcompat.R.id.search_button))
            .perform(ViewActions.click())
        Espresso.onView(withId(androidx.appcompat.R.id.search_src_text))
            .perform(ViewActions.typeText("dummyquery"), ViewActions.pressImeActionButton())
        Espresso.onView(withId(androidx.appcompat.R.id.search_close_btn))
            .perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.empty_state))
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
            Matchers.allOf<View?>(
                ViewMatchers.withId(R.id.noteNameText),
                ViewMatchers.withText(secondNoteName)
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun searchByContentTest() {
        addTestNotesToLibrary()
        Espresso.onView(ViewMatchers.withId(R.id.notesRecyclerView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(withId(androidx.appcompat.R.id.search_button))
            .perform(ViewActions.click())
        Espresso.onView(withId(androidx.appcompat.R.id.search_src_text))
            .perform(ViewActions.typeText(secondNoteContent), ViewActions.pressImeActionButton())

        Espresso.onView(
            Matchers.allOf<View?>(
                ViewMatchers.withId(R.id.noteNameText),
                ViewMatchers.withText(secondNoteName)
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun noResultsTest() {
        addTestNotesToLibrary()
        Espresso.onView(ViewMatchers.withId(R.id.notesRecyclerView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(withId(androidx.appcompat.R.id.search_button)).perform(ViewActions.click())
        Espresso.onView(withId(androidx.appcompat.R.id.search_src_text))
            .perform(ViewActions.typeText("xyznonexistent123"), ViewActions.pressImeActionButton())
        Espresso.onView(ViewMatchers.withId(R.id.empty_state))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun clearSearchTest() {
        addTestNotesToLibrary()
        Espresso.onView(ViewMatchers.withId(R.id.notesRecyclerView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
            Matchers.allOf<View?>(
                ViewMatchers.withId(R.id.noteNameText),
                ViewMatchers.withText(noteName)
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
            Matchers.allOf<View?>(
                ViewMatchers.withId(R.id.noteNameText),
                ViewMatchers.withText(secondNoteName)
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(withId(androidx.appcompat.R.id.search_button)).perform(ViewActions.click())
        Espresso.onView(withId(androidx.appcompat.R.id.search_src_text))
            .perform(ViewActions.typeText("xyznonexistent123"), ViewActions.pressImeActionButton())
        Espresso.onView(withId(androidx.appcompat.R.id.search_close_btn))
            .perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.notesRecyclerView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
            Matchers.allOf<View?>(
                ViewMatchers.withId(R.id.noteNameText),
                ViewMatchers.withText(noteName)
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
            Matchers.allOf<View?>(
                ViewMatchers.withId(R.id.noteNameText),
                ViewMatchers.withText(secondNoteName)
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}
