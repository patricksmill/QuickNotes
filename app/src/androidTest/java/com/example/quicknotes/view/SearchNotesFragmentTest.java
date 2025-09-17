package com.example.quicknotes.view;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.example.quicknotes.R;
import com.example.quicknotes.controller.ControllerActivity;
import com.example.quicknotes.model.Note;
import com.example.quicknotes.model.NoteLibrary;
import com.example.quicknotes.model.Persistence;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.LinkedHashSet;

public class SearchNotesFragmentTest {
    @Rule
    public final ActivityScenarioRule<ControllerActivity> activityRule =
            new ActivityScenarioRule<>(ControllerActivity.class);

    private final String noteName = "Test Search Note";
    private final String secondNoteName = "Second Test Note";
    private final String secondNoteContent = "This is another searchable test note";

    private Context ctx;

    @Before
    public void setup() {
        ctx = ApplicationProvider.getApplicationContext();
        ctx.deleteFile("notes.json");
    }

    private void addTestNotesToLibrary() {
        NoteLibrary noteLibrary = new NoteLibrary(ctx);
        String noteContent = "This is a searchable test note";
        noteLibrary.addNote(new Note(noteName, noteContent, new LinkedHashSet<>()));
        noteLibrary.addNote(new Note(secondNoteName, secondNoteContent, new LinkedHashSet<>()));
        Persistence.saveNotes(ctx, noteLibrary.getNotes());
        activityRule.getScenario().recreate();
    }

    @Test
    public void emptyStateTest() {
        onView(withId(R.id.empty_state)).check(matches(isDisplayed()));
        onView(withId(androidx.appcompat.R.id.search_button))
                .perform(click());
        onView(withId(androidx.appcompat.R.id.search_src_text))
                .perform(typeText("dummyquery"), pressImeActionButton());
        onView(withId(androidx.appcompat.R.id.search_close_btn)).perform(click());
        onView(withId(R.id.empty_state)).check(matches(isDisplayed()));
    }

    @Test
    public void searchByTitleTest() {
        addTestNotesToLibrary();
        onView(withText(noteName)).check(matches(isDisplayed()));
        onView(withText(secondNoteName)).check(matches(isDisplayed()));
        onView(withId(androidx.appcompat.R.id.search_button))
                .perform(click());
        onView(withId(androidx.appcompat.R.id.search_src_text))
                .perform(typeText(secondNoteName), pressImeActionButton());
        onView(allOf(withId(R.id.noteNameText), withText(secondNoteName)))
            .check(matches(isDisplayed()));
    }

    @Test
    public void searchByContentTest() {
        addTestNotesToLibrary();
        onView(withId(R.id.notesRecyclerView)).check(matches(isDisplayed()));

        onView(withId(androidx.appcompat.R.id.search_button))
                .perform(click());
        onView(withId(androidx.appcompat.R.id.search_src_text))
                .perform(typeText(secondNoteContent), pressImeActionButton());

        onView(allOf(withId(R.id.noteNameText), withText(secondNoteName)))
            .check(matches(isDisplayed()));
    }

    @Test
    public void noResultsTest() {
        addTestNotesToLibrary();
        onView(withId(R.id.notesRecyclerView)).check(matches(isDisplayed()));
        onView(withId(androidx.appcompat.R.id.search_button)).perform(click());
        onView(withId(androidx.appcompat.R.id.search_src_text))
                .perform(typeText("xyznonexistent123"), pressImeActionButton());
        onView(withId(R.id.empty_state)).check(matches(isDisplayed()));
    }

    @Test
    public void clearSearchTest() {
        addTestNotesToLibrary();
        onView(withId(R.id.notesRecyclerView)).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.noteNameText), withText(noteName)))
            .check(matches(isDisplayed()));
        onView(allOf(withId(R.id.noteNameText), withText(secondNoteName)))
            .check(matches(isDisplayed()));
        onView(withId(androidx.appcompat.R.id.search_button)).perform(click());
        onView(withId(androidx.appcompat.R.id.search_src_text))
                .perform(typeText("xyznonexistent123"), pressImeActionButton());
        onView(withId(androidx.appcompat.R.id.search_close_btn)).perform(click());
        onView(withId(R.id.notesRecyclerView)).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.noteNameText), withText(noteName)))
                .check(matches(isDisplayed()));
        onView(allOf(withId(R.id.noteNameText), withText(secondNoteName)))
                .check(matches(isDisplayed()));
    }
}
