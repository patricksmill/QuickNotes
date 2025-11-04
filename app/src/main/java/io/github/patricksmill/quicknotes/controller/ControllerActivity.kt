package io.github.patricksmill.quicknotes.controller

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.model.Notifier
import io.github.patricksmill.quicknotes.model.TutorialManager
import io.github.patricksmill.quicknotes.model.TutorialManager.OnboardingListener
import io.github.patricksmill.quicknotes.model.note.Note
import io.github.patricksmill.quicknotes.model.note.NoteLibrary
import io.github.patricksmill.quicknotes.model.tag.Tag
import io.github.patricksmill.quicknotes.model.tag.TagManager
import io.github.patricksmill.quicknotes.model.tag.TagRepository.ColorOption
import io.github.patricksmill.quicknotes.model.tag.TagSettingsManager
import io.github.patricksmill.quicknotes.view.MainUI
import io.github.patricksmill.quicknotes.view.ManageNoteFragment
import io.github.patricksmill.quicknotes.view.NotesUI
import io.github.patricksmill.quicknotes.view.SearchNotesFragment
import io.github.patricksmill.quicknotes.view.SettingsFragment
import io.github.patricksmill.quicknotes.view.TutorialOverlayFragment
import java.util.Date
import java.util.function.Consumer

/**
 * ControllerActivity acts as the Controller in the MVC architecture
 * It mediates between the NoteLibrary (Model) and the various UI Fragments (View),
 * handling user actions and updating the model or view as appropriate.
 */
class ControllerActivity : AppCompatActivity(), NotesUI.Listener, OnboardingListener {
    private var mainUI: MainUI? = null
    private var noteLibrary: NoteLibrary? = null
    private var currentSearchFragment: SearchNotesFragment? = null
    private var notifier: Notifier? = null
    private var tutorialManager: TutorialManager? = null
    private var wasWaitingForNotificationPermission = false
    private var onboardingOverlay: TutorialOverlayFragment? = null
    private var isOnboardingActive = false
    private val backStackListener = FragmentManager.OnBackStackChangedListener {
        syncFragmentListeners()
    }
    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(
            fm: FragmentManager,
            f: androidx.fragment.app.Fragment,
            v: android.view.View,
            savedInstanceState: Bundle?
        ) {
            when (f) {
                is SearchNotesFragment -> {
                    f.setListener(this@ControllerActivity)
                    currentSearchFragment = f
                    noteLibrary?.let { library ->
                        if (f.view != null) {
                            f.updateView(library.getNotes())
                        }
                    }
                }
                is ManageNoteFragment -> f.setListener(this@ControllerActivity)
            }
        }

        override fun onFragmentDestroyed(fm: FragmentManager, f: androidx.fragment.app.Fragment) {
            if (f === currentSearchFragment) {
                currentSearchFragment = null
            }
        }
    }

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied in [.onSaveInstanceState].  ***Note: Otherwise it is null.***
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.noteLibrary = NoteLibrary(applicationContext)
        this.notifier = Notifier(this)
        this.tutorialManager = TutorialManager(this)
        this.mainUI = MainUI(this)
        setContentView(this.mainUI!!.getRootView())


        // Provide root view to notifier for Snackbar display
        this.notifier!!.setRootView(this.mainUI!!.getRootView())


        // Set up onboarding listener
        this.tutorialManager!!.setListener(this)

        supportFragmentManager.addOnBackStackChangedListener(backStackListener)
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true)

        setupFragments(savedInstanceState)
        syncFragmentListeners()
        handleNotificationIntent(intent)


        // Start onboarding for first-time users (after UI is set up)
        checkAndStartOnboarding()

        // Notify if offline and AI tagging enabled: fallback will be used
        notifyOfflineIfAiEnabled()
    }

    private fun notifyOfflineIfAiEnabled() {
        try {
            val aiEnabled = noteLibrary!!.manageTags.isAiMode
            val hasKey = TagSettingsManager(this).hasValidApiKey()
            if (aiEnabled && hasKey && !this.isOnline) {
                Snackbar.make(
                    mainUI!!.getRootView(),
                    "You're offline. AI tagging will use keyword matching.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        } catch (_: Exception) {
        }
    }

    private val isOnline: Boolean
        get() {
            val cm =
                getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (cm == null) return false
            val network = cm.activeNetwork
            if (network == null) return false
            val caps = cm.getNetworkCapabilities(network)
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

    /**
     * Checks if onboarding should be shown and starts it if needed.
     */
    private fun checkAndStartOnboarding() {
        if (tutorialManager!!.shouldShowOnboarding()) {
            // Delay onboarding slightly to ensure UI is fully loaded
            mainUI!!.getRootView().post {
                tutorialManager!!.startOnboarding()
            }
        }
    }

    // Methods used by SettingsFragment actions
    fun forceStartOnboardingFromSettings() {
        tutorialManager?.forceStartOnboarding()
    }




    /**
     * Sets up initial fragments based on saved state.
     */
    private fun setupFragments(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            showSearchFragment()
        } else {
            val existing = supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
            if (existing is SearchNotesFragment) {
                currentSearchFragment = existing
                currentSearchFragment!!.setListener(this)
            } else if (existing is ManageNoteFragment) {
                existing.setListener(this)
            }
        }
    }

    /**
     * Creates and displays a new SearchNotesFragment.
     */
    private fun showSearchFragment() {
        val searchFrag = SearchNotesFragment()
        searchFrag.setListener(this)
        currentSearchFragment = searchFrag
        this.mainUI!!.displayFragment(searchFrag, false)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    /**
     * Handles intents from notifications to view specific notes.
     */
    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null || "viewNote" != intent.getStringExtra("action")) return

        val noteId = intent.getStringExtra("noteId")
        if (noteId == null) return

        val noteToView = noteLibrary!!.getNotes().stream()
            .filter { note: Note? -> noteId == note!!.id }
            .findFirst()
            .orElse(null)

        if (noteToView != null) {
            onBrowseNotes()
            onManageNotes(noteToView)
        } else {
            Snackbar.make(mainUI!!.getRootView(), "Note not found", Snackbar.LENGTH_LONG).show()
        }
    }


    /**
     * Adds demo notes to the library and updates the view.
     */
    override fun onAddDemoNotes() {
        val titles = arrayOf<String?>(
            "Meeting",
            "Shopping List",
            "Ideas for Presentation",
            "Reminder",
            "Workout routine"
        )
        val topics = arrayOf<String?>(
            "discuss the new project timeline and deliverables. Ensure we increase shareholder value",
            "groceries, household items, and birthday gifts.",
            "emphasize key points, statistics, and visual aids.",
            "call the doctor's office to schedule the a physical",
            "4 sets of 10 push-ups, 10 sit-ups, 10 squats, and 10 lunges."
        )

        for (i in topics.indices) {
            noteLibrary!!.addNote(Note(titles[i] ?: "", topics[i] ?: "", null))
        }
        updateNotesView()
        Snackbar.make(mainUI!!.getRootView(), "Demo notes added", Snackbar.LENGTH_SHORT).show()
    }

    override fun onNewNote() {
        onManageNotes(Note("", "", null))
    }

    /**
     * Saves a note to the library and updates the view.
     * @param note the note to be saved
     * @param isNewNote whether this is a new note or an existing one being edited
     */
    override fun onSaveNote(note: Note, isNewNote: Boolean) {
        if (isNewNote) {
            noteLibrary!!.addNote(note)
            noteLibrary!!.manageTags.cleanupUnusedTags()


            // If onboarding is active and this is the user's first note, advance to next step
            if (isOnboardingActive && noteLibrary!!.getNotes().size == 1) {
                // Small delay to let the note save animation complete
                mainUI!!.getRootView().postDelayed({
                    if (tutorialManager != null) {
                        tutorialManager!!.nextStep()
                    }
                }, 500)
            }
        }
        updateNotesView()
    }

    /**
     * Deletes the specified note from the library and updates the view.
     *
     * @param note the note to be deleted
     */
    override fun onDeleteNote(note: Note) {
        notifier!!.cancelNotification(note)
        noteLibrary!!.deleteNote(note)
        updateNotesView()
    }

    /**
     * Restores the most recently deleted note, if possible, and updates the view.
     */
    override fun onUndoDelete() {
        if (noteLibrary!!.undoDelete() && currentSearchFragment != null) {
            currentSearchFragment!!.updateView(noteLibrary!!.getNotes())
        }
    }


    /**
     * Toggles the pin status of the specified note and updates the view.
     *
     * @param note The note to pin or unpin
     */
    override fun onTogglePin(note: Note) {
        noteLibrary!!.togglePin(note)
        updateNotesView()
    }

    /**
     * Navigates back to the main notes browsing fragment.
     */
    override fun onBrowseNotes() {
        val current = supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
        if (current !is SearchNotesFragment) {
            showSearchFragment()
        }
    }

    /**
     * Retrieves all user notes from the library.
     *
     * @return A list containing all notes
     */
    override fun onGetNotes(): List<Note> {
        return noteLibrary!!.getNotes()
    }

    /**
     * Navigates to the ManageNoteFragment for the specified note.
     *
     * @param note The note to be managed
     */
    override fun onManageNotes(note: Note) {
        val fragment = ManageNoteFragment()
        fragment.setListener(this)
        fragment.setNoteToEdit(note)
        fragment.show(supportFragmentManager, "ManageNote")
    }

    /**
     * Returns the ManageTags instance for tag management.
     *
     * @return the ManageTags instance
     */
    override fun onManageTags(): TagManager? {
        return noteLibrary!!.manageTags
    }

    override fun onSetTags(note: Note, tags: MutableList<String?>) {
        noteLibrary!!.manageTags.setTags(note, tags)
        updateNotesView()
    }

    override fun onSetNotification(note: Note, enabled: Boolean, date: Date?) {
        // Android 13+ requires runtime POST_NOTIFICATIONS permission
        if (enabled && !hasPostNotificationsPermission()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Notifications Permission Required")
                .setMessage("To show reminders, QuickNotes needs notification permission.")
                .setPositiveButton(
                    "Allow"
                ) { dialog: DialogInterface?, which: Int ->
                    wasWaitingForNotificationPermission = true
                    requestPostNotificationsPermission()
                }
                .setNegativeButton(
                    "Cancel"
                ) { dialog: DialogInterface?, which: Int ->
                    Snackbar.make(
                        mainUI!!.getRootView(), "Notification not set - permission required",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
            return
        }

        // Exact alarm not required; proceed directly

        notifier!!.updateNotification(note, enabled, date)
        noteLibrary!!.updateNoteNotificationSettings(note, enabled, date)
        updateNotesView()
    }

    override fun onDeleteAllNotes() {
        noteLibrary!!.deleteAllNotes()
        updateNotesView()
        Snackbar.make(mainUI!!.getRootView(), "All notes deleted", Snackbar.LENGTH_SHORT).show()
    }

    override fun onGetAllTags(): MutableSet<Tag> {
        return noteLibrary!!.manageTags.allTags
    }

    override fun onGetAvailableColors(): MutableList<ColorOption?>? {
        return noteLibrary!!.manageTags.availableColors
    }

    override fun onSetTagColor(tagName: String, colorResId: Int) {
        noteLibrary!!.manageTags.setTagColor(tagName, colorResId)
    }

    // ===== Tag management operations =====
    override fun onRenameTag(oldName: String, newName: String) {
        noteLibrary!!.manageTags.renameTag(oldName, newName)
        updateNotesView()
    }

    override fun onDeleteTag(tagName: String) {
        noteLibrary!!.manageTags.deleteTag(tagName)
        updateNotesView()
    }

    override fun onMergeTags(sources: MutableList<String?>, target: String) {
        noteLibrary!!.manageTags.mergeTags(sources, target)
        updateNotesView()
    }

    // ===== AI suggestion controls =====
    override fun onIsAiTaggingConfigured(): Boolean {
        return noteLibrary!!.manageTags.isAiMode && TagSettingsManager(this).hasValidApiKey()
    }

    override fun onShouldConfirmAiSuggestions(): Boolean {
        return TagSettingsManager(this).isAiConfirmationEnabled
    }

    override fun onAiSuggestTags(
        note: Note, limit: Int,
        onSuggestions: Consumer<MutableList<String?>?>,
        onError: Consumer<String?>
    ) {
        noteLibrary!!.manageTags.aiSuggestTags(note, limit, onSuggestions, onError)
    }

    override fun onValidateNotificationDate(date: Date): Boolean {
        return notifier!!.isValidNotificationDate(date)
    }

    override fun onShouldShowNotificationIcon(note: Note): Boolean {
        return note.isNotificationsEnabled && note.notificationDate != null && note.notificationDate!!.after(
            Date()
        )
    }

    /**
     * Handles a search query by delegating to the model's searchNotes method and updating the view.
     *
     * @param query   The search query
     * @param title   Whether to search in titles
     * @param content Whether to search in content
     * @param tag     Whether to search in tags
     */
    override fun onSearchNotes(query: String, title: Boolean, content: Boolean, tag: Boolean) {
        val searchResults: List<Note> =
            noteLibrary!!.searchNotes(query, title, content, tag)
        if (currentSearchFragment != null) {
            currentSearchFragment!!.updateView(searchResults)
        }
    }

    /**
     * Navigates to the SettingsFragment.
     */
    override fun onOpenSettings() {
        mainUI!!.displayFragment(SettingsFragment(), true)
        syncFragmentListeners()
    }

    /**
     * Updates the notes view with the current state of the note library.
     */
    private fun updateNotesView() {
        val library = noteLibrary ?: return
        val search = currentSearchFragment ?: findSearchFragment()
        currentSearchFragment = search
        search?.updateView(library.getNotes())
    }

    private fun findSearchFragment(): SearchNotesFragment? {
        return supportFragmentManager.fragments.firstOrNull { it is SearchNotesFragment } as? SearchNotesFragment
    }

    private fun syncFragmentListeners() {
        val library = noteLibrary ?: return
        var visibleSearch: SearchNotesFragment? = null

        fun bindSearch(fragment: SearchNotesFragment) {
            fragment.setListener(this)
            if (fragment.view != null) {
                fragment.updateView(library.getNotes())
            }
            if (fragment.isVisible) {
                visibleSearch = fragment
            } else if (visibleSearch == null) {
                visibleSearch = fragment
            }
        }

        supportFragmentManager.fragments.forEach { fragment ->
            when (fragment) {
                is SearchNotesFragment -> bindSearch(fragment)
                is ManageNoteFragment -> fragment.setListener(this)
            }
        }

        currentSearchFragment = visibleSearch ?: currentSearchFragment
    }

    override fun onDestroy() {
        supportFragmentManager.removeOnBackStackChangedListener(backStackListener)
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
        super.onDestroy()
    }

    // Expose a public refresh to update notes list and tag chips immediately
    fun refreshNotesAndTags() {
        updateNotesView()
    }

    // OnboardingListener implementation
    override fun onOnboardingStarted() {
        isOnboardingActive = true
    }

    override fun onOnboardingCompleted() {
        isOnboardingActive = false
        // Onboarding finished - user can now use the app normally
        Snackbar.make(
            mainUI!!.getRootView(), "Tutorial complete! You're ready to start taking notes.",
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onCreateFirstNote() {
        // Trigger note creation from onboarding
        onNewNote()
    }



    override fun onShowOnboardingStep(step: TutorialManager.OnboardingStep) {
        // Remove any existing overlay first
        onHideOnboardingOverlay()
        val fragment = TutorialOverlayFragment.newInstance(step)
        fragment.setCallbacks(object : TutorialOverlayFragment.Callbacks {
            override fun onAction(action: TutorialManager.OnboardingStep.StepAction) {
                tutorialManager?.executeStepAction(action)
            }

            override fun onNext() {
                tutorialManager?.nextStep()
            }

            override fun onSkip() {
                tutorialManager?.skipOnboarding()
            }
        })
        onboardingOverlay = fragment
        supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment, "OnboardingOverlay")
            .commitAllowingStateLoss()
    }

    override fun onHideOnboardingOverlay() {
        val fragment = onboardingOverlay ?: return
        onboardingOverlay = null
        fragment.animateOut {
            if (!supportFragmentManager.isStateSaved) {
                supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss()
            }
        }
    }

    private fun hasPostNotificationsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_POST_NOTIFICATIONS
            )
        }
    }


    fun openSystemNotificationSettings() {
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
            wasWaitingForNotificationPermission = true
        } catch (_: Exception) {
            // Fallback to general app settings
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData("package:$packageName".toUri())
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            val granted = hasPostNotificationsPermission()
            if (granted) {
                Snackbar.make(
                    mainUI!!.getRootView(),
                    "Notification permission granted!",
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                Snackbar.make(
                    mainUI!!.getRootView(),
                    "Notification permission denied - notifications may not appear.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    }
}




