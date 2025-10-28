package com.example.quicknotes.model

import android.content.Context
import android.content.SharedPreferences
import android.os.Parcel
import android.os.Parcelable
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.example.quicknotes.view.OnboardingOverlayFragment

/**
 * OnboardingManager handles the interactive tutorial system for first-time users.
 * It provides a spotlight-style overlay that guides users through key app features.
 */
class OnboardingManager(context: Context) {
    companion object {
        private const val PREF_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val PREF_ONBOARDING_VERSION = "onboarding_version"
        private const val CURRENT_ONBOARDING_VERSION = 1
    }

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    private val steps: List<OnboardingStep> = createOnboardingSteps()
    
    private var currentOverlay: OnboardingOverlayFragment? = null
    private var listener: OnboardingListener? = null
    private var currentStepIndex = 0

    /**
     * Interface for listening to onboarding events
     */
    interface OnboardingListener {
        fun onOnboardingStarted()
        fun onOnboardingCompleted()
        fun onCreateFirstNote()
        fun onShowDemoNotes()
    }

    /**
     * Represents a single step in the onboarding process
     */
    data class OnboardingStep(
        val title: String,
        val description: String,
        val targetViewId: Int,
        val action: StepAction,
        val requiresUserAction: Boolean
    ) : Parcelable {
        enum class StepAction {
            NONE,
            CREATE_NOTE,
            SHOW_DEMO_NOTES,
            OPEN_SETTINGS,
            HIGHLIGHT_SEARCH,
            HIGHLIGHT_TAGS
        }

        constructor(parcel: Parcel) : this(
            title = parcel.readString() ?: "",
            description = parcel.readString() ?: "",
            targetViewId = parcel.readInt(),
            action = StepAction.valueOf(parcel.readString() ?: StepAction.NONE.name),
            requiresUserAction = parcel.readByte().toInt() != 0
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(title)
            parcel.writeString(description)
            parcel.writeInt(targetViewId)
            parcel.writeString(action.name)
            parcel.writeByte(if (requiresUserAction) 1 else 0)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<OnboardingStep> {
            override fun createFromParcel(parcel: Parcel): OnboardingStep = OnboardingStep(parcel)
            override fun newArray(size: Int): Array<OnboardingStep?> = arrayOfNulls(size)
        }
    }

    /**
     * Sets the listener for onboarding events
     */
    fun setListener(listener: OnboardingListener?) {
        this.listener = listener
    }

    /**
     * Checks if onboarding should be shown for first-time users
     */
    fun shouldShowOnboarding(): Boolean {
        return !preferences.getBoolean(PREF_ONBOARDING_COMPLETED, false) ||
               preferences.getInt(PREF_ONBOARDING_VERSION, 0) < CURRENT_ONBOARDING_VERSION
    }

    /**
     * Starts the onboarding flow
     */
    fun startOnboarding(activity: FragmentActivity) {
        listener?.onOnboardingStarted()
        currentStepIndex = 0
        showStep(activity)
    }

    /**
     * Forces onboarding to start (for manual trigger from settings)
     */
    fun forceStartOnboarding(activity: FragmentActivity) {
        listener?.onOnboardingStarted()
        currentStepIndex = 0
        showStep(activity)
    }

    /**
     * Proceeds to the next onboarding step
     */
    fun nextStep(activity: FragmentActivity) {
        currentStepIndex++
        if (currentStepIndex >= steps.size) {
            completeOnboarding()
        } else {
            showStep(activity)
        }
    }

    /**
     * Skips the entire onboarding flow
     */
    fun skipOnboarding() {
        completeOnboarding()
    }

    /**
     * Marks onboarding as completed
     */
    private fun completeOnboarding() {
        hideCurrentOverlay()
        preferences.edit {
            putBoolean(PREF_ONBOARDING_COMPLETED, true)
                .putInt(PREF_ONBOARDING_VERSION, CURRENT_ONBOARDING_VERSION)
        }
        
        listener?.onOnboardingCompleted()
    }

    /**
     * Shows the current onboarding step
     */
    private fun showStep(activity: FragmentActivity) {
        hideCurrentOverlay()
        if (currentStepIndex < steps.size) {
            val step = steps[currentStepIndex]
            val fragment = OnboardingOverlayFragment.newInstance(step)
            fragment.setOnboardingManager(this)
            currentOverlay = fragment
            val fm = activity.supportFragmentManager
            fm.beginTransaction()
                .add(android.R.id.content, fragment, "OnboardingOverlay")
                .commitAllowingStateLoss()
        }
    }

    /**
     * Hides the current overlay if it exists
     */
    private fun hideCurrentOverlay() {
        val fragment = currentOverlay ?: return
        val activity = fragment.activity ?: run { currentOverlay = null; return }
        val fm = activity.supportFragmentManager
        fragment.animateOut {
            if (!fm.isStateSaved) {
                fm.beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss()
            }
        }
        currentOverlay = null
    }

    /**
     * Handles step-specific actions
     */
    fun executeStepAction(action: OnboardingStep.StepAction) {
        listener ?: return

        when (action) {
            OnboardingStep.StepAction.CREATE_NOTE -> listener?.onCreateFirstNote()
            OnboardingStep.StepAction.SHOW_DEMO_NOTES -> listener?.onShowDemoNotes()
            OnboardingStep.StepAction.OPEN_SETTINGS -> {
                // Settings action will be handled separately if needed
            }
            OnboardingStep.StepAction.NONE,
            OnboardingStep.StepAction.HIGHLIGHT_SEARCH,
            OnboardingStep.StepAction.HIGHLIGHT_TAGS -> {
                // No action needed for highlighting steps
            }
        }
    }

    /**
     * Creates the list of onboarding steps
     */
    private fun createOnboardingSteps(): List<OnboardingStep> {
        return listOf(
            // Welcome step
            OnboardingStep(
                "Welcome to QuickNotes!",
                "Your intelligent note companion. Let's get you started with a quick tour of the key features.",
                -1, // No specific target view
                OnboardingStep.StepAction.NONE,
                false
            ),
            
            // Create first note step
            OnboardingStep(
                "Create Your First Note",
                "Tap the + button to create a new note. You can add a title, content, and tags to organize your thoughts.",
                com.example.quicknotes.R.id.addNoteFab,
                OnboardingStep.StepAction.CREATE_NOTE,
                true
            ),
            
            // Search and organization
            OnboardingStep(
                "Search Your Notes",
                "Use the search bar to quickly find notes by title, content, or tags. As you create more notes, this becomes invaluable!",
                com.example.quicknotes.R.id.search_bar,
                OnboardingStep.StepAction.HIGHLIGHT_SEARCH,
                false
            ),
            
            // Tag filtering
            OnboardingStep(
                "Filter with Tags",
                "Tags appear here when you add them to notes. Tap any tag to filter your notes and stay organized.",
                com.example.quicknotes.R.id.tagRecyclerView,
                OnboardingStep.StepAction.HIGHLIGHT_TAGS,
                false
            ),
            
            // Demo notes
            OnboardingStep(
                "Try Demo Notes",
                "Want to see the app in action? Long-press the + button to add some example notes you can play with.",
                com.example.quicknotes.R.id.addNoteFab,
                OnboardingStep.StepAction.SHOW_DEMO_NOTES,
                false
            ),
            
            // Settings and advanced features
            OnboardingStep(
                "Explore Advanced Features",
                "Visit Settings to enable AI-powered auto-tagging, customize tag colors, and set up note reminders!",
                com.example.quicknotes.R.id.settingsButton,
                OnboardingStep.StepAction.NONE,
                false
            )
        )
    }
}

