package com.example.quicknotes.view;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.quicknotes.R;
import com.example.quicknotes.controller.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Instrumentation test for the SettingsFragment and TagColorSettingsFragment
 * This class tests the functionality of the settings screens,
 * including preference navigation and dialog interactions.
 */
public class SettingsFragmentTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        Context ctx = InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext();
        ctx.deleteFile("notes.json");
        navigateToSettings();
    }
    
    /**
     * Helper method to navigate to the settings screen
     */
    private void navigateToSettings() {
        onView(withId(R.id.settingsButton))
                .perform(click());
    }

    
    /**
     * Test that the settings screen displays correctly
     */
    @Test
    public void testSettingsDisplayed() {
        onView(withText("OpenAI API Key"))
                .check(matches(isDisplayed()));

        onView(withText("Auto-Tag Limit"))
                .check(matches(isDisplayed()));

        onView(withText("Allow Notifications"))
                .check(matches(isDisplayed()));

        onView(withText("Delete All Notes"))
                .check(matches(isDisplayed()));

        onView(withText("Edit Tag Colors"))
                .check(matches(isDisplayed()));
    }
    
    /**
     * Test that clicking on the OpenAI API key preference opens a dialog
     */
    @Test
    public void testOpenApiKeyClick() {
        onView(withText("OpenAI API Key"))
                .perform(click());

        onView(withText("OpenAI API Key"))
                .check(matches(isDisplayed()));

        onView(withText("Cancel"))
                .perform(click());
    }
    
    /**
     * Test that clicking on the Delete All Notes preference shows a confirmation dialog
     */
    @Test
    public void testDeleteAllNotesDialog() {
        onView(withText("Delete All Notes"))
                .perform(click());

        onView(withText("Delete All Notes"))
                .check(matches(isDisplayed()));
        
        onView(withText("Are you sure? This will permanently erase all your notes."))
                .check(matches(isDisplayed()));

        onView(withText("Cancel"))
                .perform(click());
    }
    
    /**
     * Test toggling AI mode preference
     */
    @Test
    public void testToggleAiMode() {
        onView(withText("Use AI-powered Auto-Tagging"))
                .perform(click());
        
        onView(withText("Use AI-powered Auto-Tagging"))
                .perform(click());
    }
    
    /**
     * Test navigation to Tag Color Settings screen
     */
    @Test
    public void testNavigateToTagColorSettings() {
        onView(withText("Edit Tag Colors"))
                .perform(click());

        onView(withText("No tags available"))
                .check(matches(isDisplayed()));

        pressBack();
        onView(withText("AI"))
                .check(matches(isDisplayed()));
    }

}