package com.example.quicknotes.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.example.quicknotes.view.OnboardingOverlayFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * OnboardingManager handles the interactive tutorial system for first-time users.
 * It provides a spotlight-style overlay that guides users through key app features.
 */
public class OnboardingManager {
    private static final String PREF_ONBOARDING_COMPLETED = "onboarding_completed";
    private static final String PREF_ONBOARDING_VERSION = "onboarding_version";
    private static final int CURRENT_ONBOARDING_VERSION = 1;

    private final SharedPreferences preferences;
    private final List<OnboardingStep> steps;
    private OnboardingOverlayFragment currentOverlay;
    private OnboardingListener listener;
    private int currentStepIndex = 0;

    /**
     * Interface for listening to onboarding events
     */
    public interface OnboardingListener {
        void onOnboardingStarted();
        void onOnboardingCompleted();
        void onCreateFirstNote();
        void onShowDemoNotes();
    }

    /**
         * Represents a single step in the onboarding process
         */
        public record OnboardingStep(String title, String description, int targetViewId,
                                     OnboardingManager.OnboardingStep.StepAction action,
                                     boolean requiresUserAction) {
            public enum StepAction {
                NONE,
                CREATE_NOTE,
                SHOW_DEMO_NOTES,
                OPEN_SETTINGS,
                HIGHLIGHT_SEARCH,
                HIGHLIGHT_TAGS
            }

    }

    public OnboardingManager(@NonNull Context context) {
        Context context1 = context.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context1);
        this.steps = createOnboardingSteps();
    }

    /**
     * Sets the listener for onboarding events
     */
    public void setListener(OnboardingListener listener) {
        this.listener = listener;
    }

    /**
     * Checks if onboarding should be shown for first-time users
     */
    public boolean shouldShowOnboarding() {
        return !preferences.getBoolean(PREF_ONBOARDING_COMPLETED, false) ||
               preferences.getInt(PREF_ONBOARDING_VERSION, 0) < CURRENT_ONBOARDING_VERSION;
    }

    /**
     * Starts the onboarding flow
     */
    public void startOnboarding(@NonNull FragmentActivity activity, @NonNull ViewGroup rootView) {
        if (listener != null) {
            listener.onOnboardingStarted();
        }
        currentStepIndex = 0;
        showStep(activity, rootView);
    }

    /**
     * Forces onboarding to start (for manual trigger from settings)
     */
    public void forceStartOnboarding(@NonNull FragmentActivity activity, @NonNull ViewGroup rootView) {
        if (listener != null) {
            listener.onOnboardingStarted();
        }
        currentStepIndex = 0;
        showStep(activity, rootView);
    }

    /**
     * Proceeds to the next onboarding step
     */
    public void nextStep(@NonNull FragmentActivity activity, @NonNull ViewGroup rootView) {
        currentStepIndex++;
        if (currentStepIndex >= steps.size()) {
            completeOnboarding();
        } else {
            showStep(activity, rootView);
        }
    }

    /**
     * Skips the entire onboarding flow
     */
    public void skipOnboarding() {
        completeOnboarding();
    }

    /**
     * Marks onboarding as completed
     */
    private void completeOnboarding() {
        hideCurrentOverlay();
        preferences.edit()
                .putBoolean(PREF_ONBOARDING_COMPLETED, true)
                .putInt(PREF_ONBOARDING_VERSION, CURRENT_ONBOARDING_VERSION)
                .apply();
        
        if (listener != null) {
            listener.onOnboardingCompleted();
        }
    }

    /**
     * Shows the current onboarding step
     */
    private void showStep(@NonNull FragmentActivity activity, @NonNull ViewGroup rootView) {
        hideCurrentOverlay();
        
        if (currentStepIndex < steps.size()) {
            OnboardingStep step = steps.get(currentStepIndex);
            currentOverlay = new OnboardingOverlayFragment(activity, rootView, step, this);
            currentOverlay.show();
        }
    }

    /**
     * Hides the current overlay if it exists
     */
    private void hideCurrentOverlay() {
        if (currentOverlay != null) {
            currentOverlay.hide();
            currentOverlay = null;
        }
    }

    /**
     * Handles step-specific actions
     */
    public void executeStepAction(OnboardingStep.StepAction action) {
        if (listener == null) return;

        switch (action) {
            case CREATE_NOTE:
                listener.onCreateFirstNote();
                break;
            case SHOW_DEMO_NOTES:
                listener.onShowDemoNotes();
                break;
            case OPEN_SETTINGS:
                // Settings action will be handled separately if needed
                break;
            case NONE:
            case HIGHLIGHT_SEARCH:
            case HIGHLIGHT_TAGS:
            default:
                // No action needed for highlighting steps
                break;
        }
    }

    /**
     * Creates the list of onboarding steps
     */
    private List<OnboardingStep> createOnboardingSteps() {
        List<OnboardingStep> stepList = new ArrayList<>();
        
        // Welcome step
        stepList.add(new OnboardingStep(
            "Welcome to QuickNotes!",
            "Your intelligent note companion. Let's get you started with a quick tour of the key features.",
            -1, // No specific target view
            OnboardingStep.StepAction.NONE,
            false
        ));

        // Create first note step
        stepList.add(new OnboardingStep(
            "Create Your First Note",
            "Tap the + button to create a new note. You can add a title, content, and tags to organize your thoughts.",
            com.example.quicknotes.R.id.addNoteFab,
            OnboardingStep.StepAction.CREATE_NOTE,
            true
        ));

        // Search and organization
        stepList.add(new OnboardingStep(
            "Search Your Notes",
            "Use the search bar to quickly find notes by title, content, or tags. As you create more notes, this becomes invaluable!",
            com.example.quicknotes.R.id.search_bar,
            OnboardingStep.StepAction.HIGHLIGHT_SEARCH,
            false
        ));

        // Tag filtering
        stepList.add(new OnboardingStep(
            "Filter with Tags",
            "Tags appear here when you add them to notes. Tap any tag to filter your notes and stay organized.",
            com.example.quicknotes.R.id.tagRecyclerView,
            OnboardingStep.StepAction.HIGHLIGHT_TAGS,
            false
        ));

        // Demo notes
        stepList.add(new OnboardingStep(
            "Try Demo Notes",
            "Want to see the app in action? Long-press the + button to add some example notes you can play with.",
            com.example.quicknotes.R.id.addNoteFab,
            OnboardingStep.StepAction.SHOW_DEMO_NOTES,
            false
        ));

        // Settings and advanced features
        stepList.add(new OnboardingStep(
            "Explore Advanced Features",
            "Visit Settings to enable AI-powered auto-tagging, customize tag colors, and set up note reminders!",
            com.example.quicknotes.R.id.settingsButton,
            OnboardingStep.StepAction.NONE,
            false
        ));

        return stepList;
    }

}