package io.github.patricksmill.quicknotes.controller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import io.github.patricksmill.quicknotes.model.Notifier
import io.github.patricksmill.quicknotes.model.TutorialManager
import io.github.patricksmill.quicknotes.model.TutorialManager.TutorialListener
import io.github.patricksmill.quicknotes.model.note.Note
import io.github.patricksmill.quicknotes.model.note.NoteLibrary
import io.github.patricksmill.quicknotes.model.tag.AiModelCatalog
import io.github.patricksmill.quicknotes.model.tag.Tag
import io.github.patricksmill.quicknotes.model.tag.TagManager
import io.github.patricksmill.quicknotes.model.tag.TagRepository.ColorOption
import io.github.patricksmill.quicknotes.model.tag.TagSettingsManager
import io.github.patricksmill.quicknotes.view.NotesUI
import io.github.patricksmill.quicknotes.view.compose.components.NotificationPermissionDialog
import io.github.patricksmill.quicknotes.view.compose.navigation.QuickNotesNavHost
import io.github.patricksmill.quicknotes.view.compose.overlay.TutorialOverlay
import io.github.patricksmill.quicknotes.view.compose.theme.QuickNotesTheme
import kotlinx.coroutines.launch
import java.util.Date
import java.util.function.Consumer

class ControllerActivity : AppCompatActivity(), NotesUI.Listener, TutorialListener {
    private data class NotesUiState(
        val notes: List<Note> = emptyList(),
        val tags: List<Tag> = emptyList(),
        /** Bumped on each refresh so in-place note mutations trigger recomposition. */
        val revision: Int = 0
    )

    private data class PendingNotification(
        val note: Note,
        val enabled: Boolean,
        val date: Date?
    )

    private var noteLibrary: NoteLibrary? = null
    private var notifier: Notifier? = null
    private var tutorialManager: TutorialManager? = null
    private var isTutorialActive = false

    private var uiState by mutableStateOf(NotesUiState())
    private var searchQuery by mutableStateOf("")
    private var noteToEdit by mutableStateOf<Note?>(null)
    private var tutorialStep by mutableStateOf<TutorialManager.TutorialStep?>(null)
    private var openSettingsRequest by mutableStateOf(false)
    private var showNotificationPermissionDialog by mutableStateOf(false)
    private var pendingNotification by mutableStateOf<PendingNotification?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        noteLibrary = NoteLibrary(applicationContext)
        notifier = Notifier(this)
        tutorialManager = TutorialManager(this)
        tutorialManager?.setListener(this)
        notifier?.setRootView(window.decorView)
        refreshNotes()

        setContent {
            QuickNotesTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val tagSettingsManager = remember { TagSettingsManager(this@ControllerActivity) }
                val modelCatalog = remember { AiModelCatalog(this@ControllerActivity) }
                val appVersion = remember {
                    try {
                        packageManager.getPackageInfo(packageName, 0).versionName ?: ""
                    } catch (_: Exception) {
                        ""
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    showNotificationPermissionDialog = false
                    pendingNotification?.let { pending ->
                        if (granted) {
                            applyNotification(pending.note, pending.enabled, pending.date)
                        }
                        pendingNotification = null
                    }
                }

                fun showMessage(message: String) {
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    QuickNotesNavHost(
                        notes = uiState.notes,
                        tags = uiState.tags,
                        revision = uiState.revision,
                        searchQuery = searchQuery,
                        listener = this@ControllerActivity,
                        snackbarHostState = snackbarHostState,
                        tagSettingsManager = tagSettingsManager,
                        modelCatalog = modelCatalog,
                        appVersion = appVersion,
                        noteToEdit = noteToEdit,
                        openSettingsRequest = openSettingsRequest,
                        onOpenSettingsConsumed = { openSettingsRequest = false },
                        onNoteToEditConsumed = { noteToEdit = null },
                        onRefresh = { refreshNotes() },
                        onReplayTutorial = { forceStartTutorial() },
                        onOpenNotificationSettings = { openSystemNotificationSettings() },
                        showMessage = ::showMessage
                    )

                    tutorialStep?.let { step ->
                        BackHandler { tutorialManager?.skipTutorial() }
                        TutorialOverlay(
                            step = step,
                            onAction = { tutorialManager?.executeStepAction(it) },
                            onNext = { tutorialManager?.nextStep() },
                            onSkip = { tutorialManager?.skipTutorial() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (showNotificationPermissionDialog) {
                        NotificationPermissionDialog(
                            onAllow = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            onDismiss = {
                                showNotificationPermissionDialog = false
                                pendingNotification = null
                            }
                        )
                    }
                }
            }
        }

        handleNotificationIntent(intent)
        if (tutorialManager!!.shouldShowTutorial()) {
            window.decorView.post { tutorialManager!!.startTutorial() }
        }
    }

    fun refreshNotes() {
        val notes = if (searchQuery.isEmpty()) {
            noteLibrary!!.getNotes()
        } else {
            noteLibrary!!.searchNotes(searchQuery, title = true, content = true, tag = true)
        }
        uiState = NotesUiState(
            notes = notes,
            tags = noteLibrary!!.manageTags.allTags.sortedBy { it.name },
            revision = uiState.revision + 1
        )
    }

    fun forceStartTutorial() {
        tutorialManager?.forceStartTutorial()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null || intent.getStringExtra("action") != "viewNote") return
        val noteId = intent.getStringExtra("noteId") ?: return
        noteLibrary!!.getNotes().firstOrNull { it.id == noteId }?.let { onManageNotes(it) }
    }

    override fun onAddDemoNotes() {
        val titles = arrayOf("Meeting", "Shopping List", "Ideas for Presentation", "Reminder", "Workout routine")
        val topics = arrayOf(
            "discuss the new project timeline and deliverables. Ensure we increase shareholder value",
            "groceries, household items, and birthday gifts.",
            "emphasize key points, statistics, and visual aids.",
            "call the doctor's office to schedule the a physical",
            "4 sets of 10 push-ups, 10 sit-ups, 10 squats, and 10 lunges."
        )
        for (i in topics.indices) {
            noteLibrary!!.addNote(Note(titles[i], topics[i], null))
        }
        refreshNotes()
    }

    override fun onNewNote() {
        noteToEdit = Note("", "", null)
    }

    override fun onSaveNote(note: Note, isNewNote: Boolean) {
        if (isNewNote) {
            noteLibrary!!.addNote(note)
            noteLibrary!!.manageTags.cleanupUnusedTags()
            if (isTutorialActive && noteLibrary!!.getNotes().size == 1) {
                window.decorView.postDelayed({ tutorialManager?.nextStep() }, 500)
            }
        }
        refreshNotes()
    }

    override fun onDeleteNote(note: Note) {
        notifier!!.cancelNotification(note)
        noteLibrary!!.deleteNote(note)
        refreshNotes()
    }

    override fun onUndoDelete() {
        if (noteLibrary!!.undoDelete()) refreshNotes()
    }

    override fun onTogglePin(note: Note) {
        noteLibrary!!.togglePin(note)
        refreshNotes()
    }

    override fun onGetNotes(): List<Note> = noteLibrary!!.getNotes()

    override fun onManageNotes(note: Note) {
        noteToEdit = note
    }

    override fun onManageTags(): TagManager? = noteLibrary!!.manageTags

    override fun onSetTags(note: Note, tags: MutableList<String?>) {
        noteLibrary!!.manageTags.setTags(note, tags)
        refreshNotes()
    }

    override fun onSetNotification(note: Note, enabled: Boolean, date: Date?) {
        if (enabled && !hasPostNotificationsPermission()) {
            pendingNotification = PendingNotification(note, enabled, date)
            showNotificationPermissionDialog = true
            return
        }
        applyNotification(note, enabled, date)
    }

    private fun applyNotification(note: Note, enabled: Boolean, date: Date?) {
        notifier!!.updateNotification(note, enabled, date)
        noteLibrary!!.updateNoteNotificationSettings(note, enabled, date)
        refreshNotes()
    }

    override fun onDeleteAllNotes() {
        noteLibrary!!.deleteAllNotes()
        refreshNotes()
    }

    override fun onGetAllTags(): MutableSet<Tag> = noteLibrary!!.manageTags.allTags

    override fun onGetAvailableColors(): MutableList<ColorOption?>? = noteLibrary!!.manageTags.availableColors

    override fun onSetTagColor(tagName: String, colorResId: Int) {
        noteLibrary!!.manageTags.setTagColor(tagName, colorResId)
        refreshNotes()
    }

    override fun onGetTagColor(tagName: String): Int =
        noteLibrary!!.manageTags.getTagColorRes(tagName)

    override fun onRenameTag(oldName: String, newName: String) {
        noteLibrary!!.manageTags.renameTag(oldName, newName)
        refreshNotes()
    }

    override fun onDeleteTag(tagName: String) {
        noteLibrary!!.manageTags.deleteTag(tagName)
        refreshNotes()
    }

    override fun onMergeTags(sources: MutableList<String?>, target: String) {
        noteLibrary!!.manageTags.mergeTags(sources, target)
        refreshNotes()
    }

    override fun onIsAiTaggingConfigured(): Boolean =
        noteLibrary!!.manageTags.isAiMode && TagSettingsManager(this).hasValidApiKey()

    override fun onShouldConfirmAiSuggestions(): Boolean =
        TagSettingsManager(this).isAiConfirmationEnabled

    override fun onAiSuggestTags(
        note: Note,
        limit: Int,
        onSuggestions: Consumer<MutableList<String?>?>,
        onError: Consumer<String?>
    ) {
        noteLibrary!!.manageTags.aiSuggestTags(note, limit, onSuggestions, onError)
    }

    override fun onValidateNotificationDate(date: Date): Boolean =
        notifier!!.isValidNotificationDate(date)

    override fun onShouldShowNotificationIcon(note: Note): Boolean =
        note.isNotificationsEnabled && note.notificationDate != null && note.notificationDate!!.after(Date())

    override fun onSearchNotes(query: String, title: Boolean, content: Boolean, tag: Boolean) {
        searchQuery = query.trim()
        uiState = uiState.copy(
            notes = noteLibrary!!.searchNotes(searchQuery, title, content, tag),
            revision = uiState.revision + 1
        )
    }

    override fun onOpenSettings() {
        openSettingsRequest = true
    }

    override fun onOnTutorialStarted() {
        isTutorialActive = true
    }

    override fun onTutorialCompleted() {
        isTutorialActive = false
        tutorialStep = null
    }

    override fun onCreateFirstNote() {
        onNewNote()
    }

    override fun onShowTutorialStep(step: TutorialManager.TutorialStep) {
        tutorialStep = step
    }

    override fun onHideTutorialOverlay() {
        tutorialStep = null
    }

    fun openSystemNotificationSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        } catch (_: Exception) {
            try {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData("package:$packageName".toUri())
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun hasPostNotificationsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
