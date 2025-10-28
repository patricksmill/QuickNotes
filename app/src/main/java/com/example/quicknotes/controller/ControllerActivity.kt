package com.example.quicknotes.controller;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.quicknotes.R;
import com.example.quicknotes.model.Note;
import com.example.quicknotes.model.NoteLibrary;
import com.example.quicknotes.model.Notifier;
import com.example.quicknotes.model.OnboardingManager;
import com.example.quicknotes.model.TagManager;
import com.example.quicknotes.view.MainUI;
import com.example.quicknotes.view.ManageNoteFragment;
import com.example.quicknotes.view.NotesUI;
import com.example.quicknotes.view.SearchNotesFragment;
import com.example.quicknotes.view.SettingsFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * ControllerActivity acts as the Controller in the MVC architecture
 * It mediates between the NoteLibrary (Model) and the various UI Fragments (View),
 * handling user actions and updating the model or view as appropriate.
 */
public class ControllerActivity extends AppCompatActivity implements NotesUI.Listener, OnboardingManager.OnboardingListener {
    private MainUI mainUI;
    private NoteLibrary noteLibrary;
    private SearchNotesFragment currentSearchFragment;
    private Notifier notifier;
    private OnboardingManager onboardingManager;
    private boolean wasWaitingForPermission = false;
    private boolean wasWaitingForNotificationPermission = false;
    private boolean isOnboardingActive = false;

    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001;

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.noteLibrary = new NoteLibrary(getApplicationContext());
        this.notifier = new Notifier(this);
        this.onboardingManager = new OnboardingManager(this);
        this.mainUI = new MainUI(this);
        setContentView(this.mainUI.getRootView());
        
        // Provide root view to notifier for Snackbar display
        this.notifier.setRootView(this.mainUI.getRootView());
        
        // Set up onboarding listener
        this.onboardingManager.setListener(this);

        setupFragments(savedInstanceState);
        handleNotificationIntent(getIntent());
        checkAlarmPermission();
        
        // Start onboarding for first-time users (after UI is set up)
        checkAndStartOnboarding();

        // Notify if offline and AI tagging enabled: fallback will be used
        notifyOfflineIfAiEnabled();
    }

    private void notifyOfflineIfAiEnabled() {
        try {
            boolean aiEnabled = noteLibrary.getManageTags().isAiMode();
            boolean hasKey = new com.example.quicknotes.model.TagSettingsManager(this).hasValidApiKey();
            if (aiEnabled && hasKey && !isOnline()) {
                Snackbar.make(mainUI.getRootView(), "You're offline. AI tagging will use keyword matching.", Snackbar.LENGTH_LONG).show();
            }
        } catch (Exception ignored) { }
    }

    private boolean isOnline() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;
        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /**
     * Checks if onboarding should be shown and starts it if needed.
     */
    private void checkAndStartOnboarding() {
        if (onboardingManager.shouldShowOnboarding()) {
            // Delay onboarding slightly to ensure UI is fully loaded
            mainUI.getRootView().post(() -> onboardingManager.startOnboarding(this, (ViewGroup) mainUI.getRootView()));
        }
    }

    /**
     * Checks and requests alarm permission if needed, with user-friendly messaging.
     */
    private void checkAlarmPermission() {
        if (!notifier.canScheduleExactAlarms()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Notification Permission Required")
                    .setMessage("QuickNotes needs permission to schedule exact alarms for note reminders. " +
                               "This allows you to get notified at the exact time you set for your notes.")
                    .setPositiveButton("Grant Permission", (dialog, which) -> {
                        wasWaitingForPermission = true;
                        notifier.requestExactAlarmPermission();
                    })
                    .setNegativeButton("Later", null)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if permission was granted when returning from settings
        if (wasWaitingForPermission && notifier.canScheduleExactAlarms()) {
            wasWaitingForPermission = false;
            Snackbar.make(mainUI.getRootView(), "Alarm permission granted! You can now set note reminders.", 
                         Snackbar.LENGTH_LONG).show();
        }

        if (wasWaitingForNotificationPermission && hasPostNotificationsPermission()) {
            wasWaitingForNotificationPermission = false;
            Snackbar.make(mainUI.getRootView(), "Notification permission granted!", Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Sets up initial fragments based on saved state.
     */
    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            showSearchFragment();
        } else {
            Fragment existing = getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
            if (existing instanceof SearchNotesFragment) {
                currentSearchFragment = (SearchNotesFragment) existing;
                currentSearchFragment.setListener(this);
            } else if (existing instanceof ManageNoteFragment) {
                ((ManageNoteFragment) existing).setListener(this);
            }
        }
    }

    /**
     * Creates and displays a new SearchNotesFragment.
     */
    private void showSearchFragment() {
        SearchNotesFragment searchFrag = new SearchNotesFragment();
        searchFrag.setListener(this);
        currentSearchFragment = searchFrag;
        this.mainUI.displayFragment(searchFrag, false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    /**
     * Handles intents from notifications to view specific notes.
     */
    private void handleNotificationIntent(Intent intent) {
        if (intent == null || !"viewNote".equals(intent.getStringExtra("action"))) return;

        String noteId = intent.getStringExtra("noteId");
        if (noteId == null) return;

        Note noteToView = noteLibrary.getNotes().stream()
                .filter(note -> noteId.equals(note.getId()))
                .findFirst()
                .orElse(null);

        if (noteToView != null) {
            onBrowseNotes();
            onManageNotes(noteToView);
        } else {
            Snackbar.make(mainUI.getRootView(), "Note not found", Snackbar.LENGTH_LONG).show();
        }
    }


    /**
     * Adds demo notes to the library and updates the view.
     */
    public void onAddDemoNotes() {
        String[] titles = {"Meeting", "Shopping List", "Ideas for Presentation", "Reminder", "Workout routine"};
        String[] topics = {
                "discuss the new project timeline and deliverables. Ensure we increase shareholder value",
                "groceries, household items, and birthday gifts.",
                "emphasize key points, statistics, and visual aids.",
                "call the doctor's office to schedule the a physical",
                "4 sets of 10 push-ups, 10 sit-ups, 10 squats, and 10 lunges."
        };
        
        for (int i = 0; i < topics.length; i++) {
            noteLibrary.addNote(new Note(titles[i], topics[i], new LinkedHashSet<>()));
        }
        updateNotesView();
        Snackbar.make(mainUI.getRootView(), "Demo notes added", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onNewNote() {
        onManageNotes(new Note("", "", new LinkedHashSet<>()));
    }

    /**
     * Saves a note to the library and updates the view.
     * @param note the note to be saved
     * @param isNewNote whether this is a new note or an existing one being edited
     */
    @Override
    public void onSaveNote(@NonNull Note note, boolean isNewNote) {
        if (isNewNote) {
            noteLibrary.addNote(note);
            noteLibrary.getManageTags().cleanupUnusedTags();
            
            // If onboarding is active and this is the user's first note, advance to next step
            if (isOnboardingActive && noteLibrary.getNotes().size() == 1) {
                // Small delay to let the note save animation complete
                mainUI.getRootView().postDelayed(() -> {
                    if (onboardingManager != null) {
                        onboardingManager.nextStep(this, (ViewGroup) mainUI.getRootView());
                    }
                }, 500);
            }
        }
        updateNotesView();
    }

    /**
     * Deletes the specified note from the library and updates the view.
     *
     * @param note the note to be deleted
     */
    @Override
    public void onDeleteNote(@NonNull Note note) {
        notifier.cancelNotification(note);
        noteLibrary.deleteNote(note);
        updateNotesView();
    }

    /**
     * Restores the most recently deleted note, if possible, and updates the view.
     */
    @Override
    public void onUndoDelete() {
        if (noteLibrary.undoDelete() && currentSearchFragment != null) {
            currentSearchFragment.updateView(noteLibrary.getNotes());
        }
    }


    /**
     * Toggles the pin status of the specified note and updates the view.
     *
     * @param note The note to pin or unpin
     */
    @Override
    public void onTogglePin(@NonNull Note note) {
        noteLibrary.togglePin(note);
        updateNotesView();
    }

    /**
     * Navigates back to the main notes browsing fragment.
     */
    @Override
    public void onBrowseNotes() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
        if (!(current instanceof SearchNotesFragment)) {
            showSearchFragment();
        }
    }

    /**
     * Retrieves all user notes from the library.
     *
     * @return A list containing all notes
     */
    @Override
    public List<Note> onGetNotes() {
        return noteLibrary.getNotes();
    }

    /**
     * Navigates to the ManageNoteFragment for the specified note.
     *
     * @param note The note to be managed
     */
    @Override
    public void onManageNotes(@NonNull Note note) {
        ManageNoteFragment fragment = new ManageNoteFragment();
        fragment.setListener(this);
        fragment.setNoteToEdit(note);
        fragment.show(getSupportFragmentManager(), "ManageNote");
    }

    /**
     * Returns the ManageTags instance for tag management.
     *
     * @return the ManageTags instance
     */
    @Override
    public TagManager onManageTags() {
        return noteLibrary.getManageTags();
    }

    @Override
    public void onSetTags(@NonNull Note note, @NonNull java.util.List<String> tags) {
        noteLibrary.getManageTags().setTags(note, tags);
        updateNotesView();
    }

    @Override
    public void onSetNotification(@NonNull Note note, boolean enabled, Date date) {
        // Android 13+ requires runtime POST_NOTIFICATIONS permission
        if (enabled && !hasPostNotificationsPermission()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Notifications Permission Required")
                    .setMessage("To show reminders, QuickNotes needs notification permission.")
                    .setPositiveButton("Allow", (dialog, which) -> {
                        wasWaitingForNotificationPermission = true;
                        requestPostNotificationsPermission();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> Snackbar.make(mainUI.getRootView(), "Notification not set - permission required",
                                     Snackbar.LENGTH_SHORT).show())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }

        // Check alarm permission before setting notification
        if (enabled && !notifier.canScheduleExactAlarms()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Permission Required")
                    .setMessage("To set note reminders, QuickNotes needs permission to schedule exact alarms.")
                    .setPositiveButton("Grant Permission", (dialog, which) -> {
                        wasWaitingForPermission = true;
                        notifier.requestExactAlarmPermission();
                        // Note: The notification will be set when user returns from settings
                        // and onResume() is called, if they grant permission
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> Snackbar.make(mainUI.getRootView(), "Notification not set - permission required",
                                 Snackbar.LENGTH_SHORT).show())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }
        
        notifier.updateNotification(note, enabled, date);
        noteLibrary.updateNoteNotificationSettings(note, enabled, date);
        updateNotesView();
    }

    @Override
    public void onDeleteAllNotes() {
        noteLibrary.deleteAllNotes();
        updateNotesView();
        Snackbar.make(mainUI.getRootView(), "All notes deleted", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public java.util.Set<com.example.quicknotes.model.Tag> onGetAllTags() {
        return noteLibrary.getManageTags().getAllTags();
    }

    @Override
    public java.util.List<com.example.quicknotes.model.TagColorManager.ColorOption> onGetAvailableColors() {
        return noteLibrary.getManageTags().getAvailableColors();
    }

    @Override
    public void onSetTagColor(@NonNull String tagName, int colorResId) {
        noteLibrary.getManageTags().setTagColor(tagName, colorResId);
    }

    // ===== Tag management operations =====
    @Override
    public void onRenameTag(@NonNull String oldName, @NonNull String newName) {
        noteLibrary.getManageTags().renameTag(oldName, newName);
        updateNotesView();
    }

    @Override
    public void onDeleteTag(@NonNull String tagName) {
        noteLibrary.getManageTags().deleteTag(tagName);
        updateNotesView();
    }

    @Override
    public void onMergeTags(@NonNull java.util.List<String> sources, @NonNull String target) {
        noteLibrary.getManageTags().mergeTags(sources, target);
        updateNotesView();
    }

    // ===== AI suggestion controls =====
    @Override
    public boolean onIsAiTaggingConfigured() {
        return noteLibrary.getManageTags().isAiMode() && new com.example.quicknotes.model.TagSettingsManager(this).hasValidApiKey();
    }

    @Override
    public boolean onShouldConfirmAiSuggestions() {
        return new com.example.quicknotes.model.TagSettingsManager(this).isAiConfirmationEnabled();
    }

    @Override
    public void onAiSuggestTags(@NonNull Note note, int limit,
                                @NonNull java.util.function.Consumer<java.util.List<String>> onSuggestions,
                                @NonNull java.util.function.Consumer<String> onError) {
        noteLibrary.getManageTags().aiSuggestTags(note, limit, onSuggestions, onError);
    }

    @Override
    public boolean onValidateNotificationDate(@NonNull Date date) {
        return notifier.isValidNotificationDate(date);
    }

    @Override
    public boolean onShouldShowNotificationIcon(@NonNull Note note) {
        return note.isNotificationsEnabled() && note.getNotificationDate() != null && note.getNotificationDate().after(new Date());
    }

    /**
     * Handles a search query by delegating to the model's searchNotes method and updating the view.
     *
     * @param query   The search query
     * @param title   Whether to search in titles
     * @param content Whether to search in content
     * @param tag     Whether to search in tags
     */
    @Override
    public void onSearchNotes(@NonNull String query, boolean title, boolean content, boolean tag) {
        List<Note> searchResults = noteLibrary.searchNotes(query, title, content, tag);
        if (currentSearchFragment != null) {
            currentSearchFragment.updateView(searchResults);
        }
    }

    /**
     * Navigates to the SettingsFragment.
     */
    @Override
    public void onOpenSettings() {
        mainUI.displayFragment(new SettingsFragment(), true);
    }

    /**
     * Updates the notes view with the current state of the note library.
     */
    private void updateNotesView() {
        if (currentSearchFragment != null) {
            currentSearchFragment.updateView(noteLibrary.getNotes());
        }
    }

    // OnboardingListener implementation
    @Override
    public void onOnboardingStarted() {
        isOnboardingActive = true;
    }

    @Override
    public void onOnboardingCompleted() {
        isOnboardingActive = false;
        // Onboarding finished - user can now use the app normally
        Snackbar.make(mainUI.getRootView(), "Tutorial complete! You're ready to start taking notes.", 
                     Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onCreateFirstNote() {
        // Trigger note creation from onboarding
        onNewNote();
    }

    @Override
    public void onShowDemoNotes() {
        // Show demo notes from onboarding
        onAddDemoNotes();
    }

    /**
     * Triggers onboarding manually (for settings menu)
     */
    public void startOnboardingTutorial() {
        if (onboardingManager != null) {
            onboardingManager.forceStartOnboarding(this, (ViewGroup) mainUI.getRootView());
        }
    }

    private boolean hasPostNotificationsPermission() {
        if (android.os.Build.VERSION.SDK_INT < 33) return true;
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void requestPostNotificationsPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_CODE_POST_NOTIFICATIONS);
        }
    }

    /**
     * Exposed for settings screen to trigger a notification permission request when the user
     * enables notifications from preferences on Android 13+.
     */
    public void requestNotificationPermissionFromSettings() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && !hasPostNotificationsPermission()) {
            wasWaitingForNotificationPermission = true;
            requestPostNotificationsPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            boolean granted = hasPostNotificationsPermission();
            if (granted) {
                Snackbar.make(mainUI.getRootView(), "Notification permission granted!", Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(mainUI.getRootView(), "Notification permission denied - notifications may not appear.", Snackbar.LENGTH_LONG).show();
            }
        }
    }
}




