package com.example.quicknotes.controller; // Assuming the package name remains if it's just a rename

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.quicknotes.R;
import com.example.quicknotes.model.Note;
import com.example.quicknotes.model.NoteViewModel;
import com.example.quicknotes.model.OnboardingManager;
import com.example.quicknotes.view.MainUI;
import com.example.quicknotes.view.SearchNotesFragmentDirections;
import com.example.quicknotes.viewmodel.MainViewModel;
import com.example.quicknotes.viewmodel.MainViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.LinkedHashSet;

// Renamed from ControllerActivity to MainActivity
public class MainActivity extends AppCompatActivity implements OnboardingManager.OnboardingListener {
    private MainUI mainUI;
    private NoteViewModel noteViewModel;
    private MainViewModel mainViewModel;
    private OnboardingManager onboardingManager; // For Onboarding UI interaction

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        MainViewModelFactory factory = new MainViewModelFactory(getApplication());
        mainViewModel = new ViewModelProvider(this, factory).get(MainViewModel.class);

        this.onboardingManager = new OnboardingManager(this);
        this.onboardingManager.setListener(this);

        this.mainUI = new MainUI(this);
        setContentView(this.mainUI.getRootView());
        
        mainViewModel.setNotifierRootView(this, this.mainUI.getRootView()); 

        handleNotificationIntent(getIntent());
        mainViewModel.checkPermissionsOnStart(this); 
        mainViewModel.checkShouldShowOnboarding(this); // Pass Activity context

        setupObservers();
    }

    private void setupObservers() {
        noteViewModel.getSnackbarMessage().observe(this, message -> {
            if (message != null) {
                Snackbar.make(mainUI.getRootView(), message, Snackbar.LENGTH_SHORT).show();
            }
        });

        noteViewModel.getUiEvents().observe(this, event -> {
            NoteViewModel.UiAction action = event.getContentIfNotHandled();
            if (action == null) return;
            NavController nav = obtainNavController();
            if (nav == null) return;
            switch (action) {
                case OPEN_SETTINGS -> nav.navigate(R.id.action_searchNotesFragment_to_settingsFragment);
                case BROWSE_NOTES -> nav.popBackStack();
            }
        });

        noteViewModel.navigateToManageNoteFromIntent.observe(this, event -> {
            Note noteToView = event.getContentIfNotHandled();
            if (noteToView != null) {
                NavController nav = obtainNavController();
                if (nav != null) {
                    nav.navigate(SearchNotesFragmentDirections.actionSearchNotesFragmentToManageNoteFragment(noteToView));
                }
            }
        });

        mainViewModel.showOnboarding.observe(this, show -> {
            if (show != null && show) {
                if (mainUI != null) {
                    mainUI.getRootView();
                    mainUI.getRootView().post(() -> onboardingManager.startOnboarding(this, (ViewGroup) mainUI.getRootView()));
                }
            }
        });

        mainViewModel.snackbarMessage.observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                Snackbar.make(mainUI.getRootView(), message, Snackbar.LENGTH_LONG).show();
            }
        });

        mainViewModel.showExactAlarmPermissionDialog.observe(this, event -> {
            if (event.getContentIfNotHandled() != null) {
                displayExactAlarmPermissionDialog();
            }
        });
        
        mainViewModel.signalRequestPostNotificationsPermission.observe(this, event -> {
            if (event.getContentIfNotHandled() != null) {
                mainViewModel.triggerRequestPostNotificationsPermission(this); 
            }
        });

        mainViewModel.navigateToNewNote.observe(this, event -> {
            if (event.getContentIfNotHandled() != null) {
                NavController nav = obtainNavController();
                if (nav != null) {
                    nav.navigate(SearchNotesFragmentDirections.actionSearchNotesFragmentToManageNoteFragment(new Note("", "", new LinkedHashSet<>())));
                }
            }
        });

        mainViewModel.triggerAddDemoNotes.observe(this, event -> {
            if (event.getContentIfNotHandled() != null) {
                noteViewModel.addDemoNotes();
            }
        });
    }

    private void displayExactAlarmPermissionDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Notification Permission Required")
            .setMessage("QuickNotes needs permission to schedule exact alarms for note reminders. " +
                       "This allows you to get notified at the exact time you set for your notes.")
            .setPositiveButton("Grant Permission", (dialog, which) ->
                    mainViewModel.exactAlarmPermissionDialogPositiveClicked(this))
            .setNegativeButton("Later", null)
            .setIcon(android.R.drawable.ic_dialog_info)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainViewModel.checkExactAlarmPermissionOnResume(this); 
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && "viewNote".equals(intent.getStringExtra("action"))) {
            String noteId = intent.getStringExtra("noteId");
            if (noteId != null) {
                noteViewModel.processViewNoteIntent(noteId);
            }
        }
    }

    @Override
    public void onOnboardingStarted() {
        mainViewModel.onboardingStarted();
    }

    @Override
    public void onOnboardingCompleted() {
        mainViewModel.onboardingCompleted(this); // Pass Activity context
    }

    @Override
    public void onCreateFirstNote() {
        mainViewModel.userWantsToCreateFirstNote();
    }

    @Override
    public void onShowDemoNotes() {
        mainViewModel.userWantsToShowDemoNotes();
    }

    public void startOnboardingTutorial() {
        mainViewModel.forceStartOnboarding();
    }

    public void userWantsToRequestPostNotificationPermission() {
         mainViewModel.userWantsToRequestPostNotificationsPermission(this); 
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mainViewModel.handleRequestPermissionsResult(this, requestCode, grantResults); // Pass Activity context and corrected params
    }
    
    @Nullable
    private NavController obtainNavController() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        return navHostFragment != null ? navHostFragment.getNavController() : null;
    }
}
