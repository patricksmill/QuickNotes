package com.example.quicknotes.view;

import android.content.Context;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.quicknotes.R;
import com.example.quicknotes.controller.ControllerActivity;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Class to test the add notes screen functionality from a user perspective
 */
public class ManageNoteFragmentTest {
    @Rule
    public ActivityScenarioRule<ControllerActivity> activityRule =
            new ActivityScenarioRule<>(ControllerActivity.class);

    @Before
    public void clearData() {
        Context ctx = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        ctx.deleteFile("notes.json");
    }

    @Test
    public void addNoteTest() {
        //Navigate to ManageNotesFragment
        Espresso.onView(ViewMatchers.withId(R.id.addNoteFab))
                .perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.noteTitleText))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        //Add Note
        String noteName = "Test Note";
        typeText(R.id.noteTitleText, noteName);
        String noteContent = "This is a test note";
        typeText(R.id.noteContentText, noteContent);
        Espresso.onView(ViewMatchers.withId(R.id.saveButton))
                .perform(ViewActions.click());


        //Ensure all note content is displayed in RecyclerView
        Espresso.onView(ViewMatchers.withId(R.id.notesRecyclerView))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        Espresso.onView(ViewMatchers.withText(noteName))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        Espresso.onView(ViewMatchers.withText(noteContent))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void addNoteNoContentTest() {

        //Navigate to ManageNotesFragment
        Espresso.onView(ViewMatchers.withId(R.id.addNoteFab))
                .perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withId(R.id.noteTitleText))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        //Type Partial note, attempt to hit save
        typeText(R.id.noteTitleText, "This note should not be saved");
        Espresso.onView(ViewMatchers.withId(R.id.saveButton))
                .perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withId(R.id.noteTitleText))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        Espresso.onView(ViewMatchers.withText("Missing Item Field Error"))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        Espresso.pressBack();
        Espresso.onView(ViewMatchers.withId(R.id.noteTitleText))
                .check(ViewAssertions.doesNotExist());

        //Ensure note is not displayed in RecyclerView
        Espresso.onView(ViewMatchers.withId(R.id.notesRecyclerView))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        Espresso.onView(ViewMatchers.withText("This note should not be saved"))
                .check(ViewAssertions.doesNotExist());
    }

    @Test
    public void cancelAddNoteTest() {
        //Navigate to ManageNotesFragment
        Espresso.onView(ViewMatchers.withId(R.id.addNoteFab))
                .perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withId(R.id.noteTitleText))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        //Enter note information and cancel (press back)
        String tempNoteName = "Temporary Note";
        typeText(R.id.noteTitleText, tempNoteName);
        typeText(R.id.noteContentText, "This should not be saved.");
        Espresso.pressBack();

        //Ensure note is not displayed in RecyclerView
        Espresso.onView(ViewMatchers.withId(R.id.noteTitleText))
                .check(ViewAssertions.doesNotExist());
        Espresso.onView(ViewMatchers.withId(R.id.notesRecyclerView))
                .check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())));
        Espresso.onView(ViewMatchers.withText(tempNoteName))
                .check(ViewAssertions.doesNotExist());
    }

    @Test
    public void deleteNoteFromEditScreenTest() {
        // Navigate to ManageNotesFragment
        Espresso.onView(ViewMatchers.withId(R.id.addNoteFab))
                .perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withId(R.id.noteTitleText))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        //Create and Add a new note
        String noteToDeleteName = "Note To Delete";
        String noteToDeleteContent = "This note will be deleted from edit screen.";
        typeText(R.id.noteTitleText, noteToDeleteName);
        typeText(R.id.noteContentText, noteToDeleteContent);
        Espresso.onView(ViewMatchers.withId(R.id.saveButton))
                .perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withId(R.id.noteTitleText))
                .check(ViewAssertions.doesNotExist());

        //Ensure note is displayed in RecyclerView, Click Note
        Espresso.onView(ViewMatchers.withId(R.id.notesRecyclerView))
                .perform(RecyclerViewActions.actionOnItem(
                        ViewMatchers.hasDescendant(ViewMatchers.withText(noteToDeleteName)),
                        ViewActions.click()));
        Espresso.onView(ViewMatchers.withId(R.id.noteTitleText))
                .check(ViewAssertions.matches(ViewMatchers.withText(noteToDeleteName)));
        Espresso.onView(ViewMatchers.withId(R.id.noteContentText))
                .check(ViewAssertions.matches(ViewMatchers.withText(noteToDeleteContent)));

        //Click Cancel Button, navigate prompts
        Espresso.onView(ViewMatchers.withId(R.id.cancelButton)).perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withText(android.R.string.ok))
                .perform(ViewActions.click());

        //Ensure note is not displayed in RecyclerView
        Espresso.onView(ViewMatchers.withId(R.id.noteTitleText))
                .check(ViewAssertions.doesNotExist());

        Espresso.onView(ViewMatchers.withText(noteToDeleteName))
                .check(ViewAssertions.doesNotExist());
    }

    private static void typeText(int viewId, String text) {
        Espresso.onView(ViewMatchers.withId(viewId))
                .perform(ViewActions.typeText(text));
        Espresso.closeSoftKeyboard();
    }
}