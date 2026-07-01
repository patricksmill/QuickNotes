package io.github.patricksmill.quicknotes.view

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import io.github.patricksmill.quicknotes.controller.ControllerActivity
import io.github.patricksmill.quicknotes.model.Persistence
import io.github.patricksmill.quicknotes.model.note.Note
import io.github.patricksmill.quicknotes.model.note.NoteLibrary
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class SearchNotesScreenTest {
    private val composeRule = createAndroidComposeRule<ControllerActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS))
        .around(composeRule)

    private val noteName = "Test Search Note"
    private val secondNoteName = "Second Test Note"
    private val secondNoteContent = "This is another searchable test note"
    private lateinit var ctx: Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext()
        ctx.deleteFile("notes.json")
    }

    private fun addTestNotesToLibrary() {
        val noteLibrary = NoteLibrary(ctx)
        noteLibrary.addNote(Note(noteName, "This is a searchable test note", emptySet()))
        noteLibrary.addNote(Note(secondNoteName, secondNoteContent, emptySet()))
        Persistence.saveNotes(ctx, noteLibrary.getNotes())
        composeRule.scenario.recreate()
        composeRule.waitForIdle()
    }

    @Test
    fun emptyStateTest() {
        composeRule.onNodeWithTag("empty_state").assertIsDisplayed()
    }

    @Test
    fun searchByTitleTest() {
        addTestNotesToLibrary()
        composeRule.onNodeWithText(noteName).assertIsDisplayed()
        composeRule.onNodeWithText(secondNoteName).assertIsDisplayed()
        composeRule.onNodeWithTag("search_bar").performClick()
        composeRule.onNodeWithText("Search notes").performTextInput(secondNoteName)
        composeRule.onNodeWithText(secondNoteName).assertIsDisplayed()
    }

    @Test
    fun noResultsTest() {
        addTestNotesToLibrary()
        composeRule.onNodeWithTag("search_bar").performClick()
        composeRule.onNodeWithText("Search notes").performTextInput("xyznonexistent123")
        composeRule.onNodeWithTag("empty_state").assertIsDisplayed()
    }
}
