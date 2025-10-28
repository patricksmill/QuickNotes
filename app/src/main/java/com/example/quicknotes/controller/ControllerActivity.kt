package com.example.quicknotes.controller

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.quicknotes.R
import com.example.quicknotes.model.Note
import com.example.quicknotes.model.NoteLibrary
import com.example.quicknotes.model.Notifier
import com.example.quicknotes.model.OnboardingManager
import com.example.quicknotes.model.OnboardingManager.OnboardingListener
import com.example.quicknotes.model.Tag
import com.example.quicknotes.model.TagColorManager.ColorOption
import com.example.quicknotes.model.TagManager
import com.example.quicknotes.model.TagSettingsManager
import com.example.quicknotes.view.MainUI
import com.example.quicknotes.view.ManageNoteFragment
import com.example.quicknotes.view.NotesUI
import com.example.quicknotes.view.SearchNotesFragment
import com.example.quicknotes.view.SettingsFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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
    private var onboardingManager: OnboardingManager? = null
    private var wasWaitingForPermission = false
    private var wasWaitingForNotificationPermission = false
    private var isOnboardingActive = false

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
        this.onboardingManager = OnboardingManager(this)
        this.mainUI = MainUI(this)
        setContentView(this.mainUI!!.getRootView())


        // Provide root view to notifier for Snackbar display
        this.notifier!!.setRootView(this.mainUI!!.getRootView())


        // Set up onboarding listener
        this.onboardingManager!!.setListener(this)

        setupFragments(savedInstanceState)
        handleNotificationIntent(intent)
        checkAlarmPermission()


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
        if (onboardingManager!!.shouldShowOnboarding()) {
            // Delay onboarding slightly to ensure UI is fully loaded
            mainUI!!.getRootView().post {
                onboardingManager!!.startOnboarding(
                    this
                )
            }
        }
    }

    // Methods used by SettingsFragment actions
    fun forceStartOnboardingFromSettings() {
        onboardingManager?.forceStartOnboarding(this)
    }

    fun requestPostNotificationsPermissionFromSettings() {
        requestPostNotificationsPermission()
    }

    /**
     * Checks and requests alarm permission if needed, with user-friendly messaging.
     */
    private fun checkAlarmPermission() {
        if (!notifier!!.canScheduleExactAlarms()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Notification Permission Required")
                .setMessage(
                    "QuickNotes needs permission to schedule exact alarms for note reminders. " +
                            "This allows you to get notified at the exact time you set for your notes."
                )
                .setPositiveButton(
                    "Grant Permission"
                ) { dialog: DialogInterface?, which: Int ->
                    wasWaitingForPermission = true
                    notifier!!.requestExactAlarmPermission()
                }
                .setNegativeButton("Later", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if permission was granted when returning from settings
        if (wasWaitingForPermission && notifier!!.canScheduleExactAlarms()) {
            wasWaitingForPermission = false
            Snackbar.make(
                mainUI!!.getRootView(), "Alarm permission granted! You can now set note reminders.",
                Snackbar.LENGTH_LONG
            ).show()
        }

        if (wasWaitingForNotificationPermission && hasPostNotificationsPermission()) {
            wasWaitingForNotificationPermission = false
            Snackbar.make(
                mainUI!!.getRootView(),
                "Notification permission granted!",
                Snackbar.LENGTH_LONG
            ).show()
        }
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
                    if (onboardingManager != null) {
                        onboardingManager!!.nextStep(this)
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

        // Check alarm permission before setting notification
        if (enabled && !notifier!!.canScheduleExactAlarms()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Permission Required")
                .setMessage("To set note reminders, QuickNotes needs permission to schedule exact alarms.")
                .setPositiveButton(
                    "Grant Permission"
                ) { dialog: DialogInterface?, which: Int ->
                    wasWaitingForPermission = true
                    notifier!!.requestExactAlarmPermission()
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
    }

    /**
     * Updates the notes view with the current state of the note library.
     */
    private fun updateNotesView() {
        if (currentSearchFragment != null) {
            currentSearchFragment!!.updateView(noteLibrary!!.getNotes())
        }
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

    override fun onShowDemoNotes() {
        // Show demo notes from onboarding
        onAddDemoNotes()
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




