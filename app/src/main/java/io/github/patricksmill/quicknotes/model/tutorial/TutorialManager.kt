package io.github.patricksmill.quicknotes.model

import android.content.Context
import android.content.SharedPreferences
import android.os.Parcel
import android.os.Parcelable
import androidx.core.content.edit
import androidx.preference.PreferenceManager

/**
 * Tutorial handles the interactive tutorial system for first-time users.
 * It provides a spotlight-style overlay that guides users through key app features.
 */
class TutorialManager(context: Context) {
    companion object {
        private const val PREF_TUTORIAL_COMPLETED = "tutorial_completed"
        private const val PREF_TUTORIAL_VERSION = "tutorial_version"
        private const val CURRENT_TUTORIAL_VERSION = 1
    }

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    private val steps: List<TutorialStep> = createTutorialSteps()
    private var listener: TutorialListener? = null
    private var currentStepIndex = 0

    /**
     * Interface for listening to tutorial events
     */
    interface TutorialListener {
        fun onOnTutorialStarted()
        fun onTutorialCompleted()
        fun onCreateFirstNote()
        fun onShowTutorialStep(step: TutorialStep)
        fun onHideTutorialOverlay()
    }

    /**
     * Represents a single step in the tutorial process
     */
    data class TutorialStep(
        val title: String,
        val description: String,
        val targetViewId: Int,
        val action: StepAction,
        val requiresUserAction: Boolean
    ) : Parcelable {
        enum class StepAction {
            NONE,
            CREATE_NOTE,
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

        companion object CREATOR : Parcelable.Creator<TutorialStep> {
            override fun createFromParcel(parcel: Parcel): TutorialStep = TutorialStep(parcel)
            override fun newArray(size: Int): Array<TutorialStep?> = arrayOfNulls(size)
        }
    }

    /**
     * Sets the listener for tutorial events
     */
    fun setListener(listener: TutorialListener?) {
        this.listener = listener
    }

    /**
     * Checks if tutorial should be shown for first-time users
     */
    fun shouldShowTutorial(): Boolean {
        return !preferences.getBoolean(PREF_TUTORIAL_COMPLETED, false) ||
               preferences.getInt(PREF_TUTORIAL_VERSION, 0) < CURRENT_TUTORIAL_VERSION
    }

    /**
     * Starts the tutorial flow
     */
    fun startTutorial() {
        listener?.onOnTutorialStarted()
        currentStepIndex = 0
        showStep()
    }

    /**
     * Forces tutorial to start (for manual trigger from settings)
     */
    fun forceStartTutorial() {
        listener?.onOnTutorialStarted()
        currentStepIndex = 0
        showStep()
    }

    /**
     * Proceeds to the next tutorial step
     */
    fun nextStep() {
        currentStepIndex++
        if (currentStepIndex >= steps.size) {
            completeTutorial()
        } else {
            showStep()
        }
    }

    /**
     * Skips the entire tutorial flow
     */
    fun skipTutorial() {
        completeTutorial()
    }

    /**
     * Marks tutorial as completed
     */
    private fun completeTutorial() {
        listener?.onHideTutorialOverlay()
        preferences.edit {
            putBoolean(PREF_TUTORIAL_COMPLETED, true)
                .putInt(PREF_TUTORIAL_VERSION, CURRENT_TUTORIAL_VERSION)
        }
        
        listener?.onTutorialCompleted()
    }

    /**
     * Shows the current tutorial step
     */
    private fun showStep() {
        if (currentStepIndex < steps.size) {
            val step = steps[currentStepIndex]
            listener?.onShowTutorialStep(step)
        }
    }

    /**
     * Handles step-specific actions
     */
    fun executeStepAction(action: TutorialStep.StepAction) {
        listener ?: return

        when (action) {
            TutorialStep.StepAction.CREATE_NOTE -> listener?.onCreateFirstNote()
            TutorialStep.StepAction.OPEN_SETTINGS -> {
                // Settings action will be handled separately if needed
            }
            TutorialStep.StepAction.NONE,
            TutorialStep.StepAction.HIGHLIGHT_SEARCH,
            TutorialStep.StepAction.HIGHLIGHT_TAGS -> {
                // No action needed for highlighting steps
            }
        }
    }

    /**
     * Creates the list of tutorial steps
     */
    private fun createTutorialSteps(): List<TutorialStep> {
        return listOf(
            // Welcome step
            TutorialStep(
                "Welcome to QuickNotes!",
                "Your intelligent note companion. Let's get you started with a quick tour of the key features.",
                -1, // No specific target view
                TutorialStep.StepAction.NONE,
                false
            ),
            
            // Create first note step
            TutorialStep(
                "Create Your First Note",
                "Tap the + button to create a new note. You can add a title, content, and tags to organize your thoughts.",
                io.github.patricksmill.quicknotes.R.id.addNoteFab,
                TutorialStep.StepAction.CREATE_NOTE,
                true
            ),
            
            // Search and organization
            TutorialStep(
                "Search Your Notes",
                "Use the search bar to quickly find notes by title, content, or tags. As you create more notes, this becomes invaluable!",
                io.github.patricksmill.quicknotes.R.id.search_bar,
                TutorialStep.StepAction.HIGHLIGHT_SEARCH,
                false
            ),
            
            // Tag filtering
            TutorialStep(
                "Filter with Tags",
                "Tags appear here when you add them to notes. Tap any tag to filter your notes and stay organized.",
                io.github.patricksmill.quicknotes.R.id.tagRecyclerView,
                TutorialStep.StepAction.HIGHLIGHT_TAGS,
                false
            ),

            // Settings and advanced features
            TutorialStep(
                "Explore Advanced Features",
                "Visit Settings to enable AI-powered auto-tagging, customize tag colors, and set up note reminders!",
                io.github.patricksmill.quicknotes.R.id.settingsButton,
                TutorialStep.StepAction.NONE,
                false
            )
        )
    }
}

