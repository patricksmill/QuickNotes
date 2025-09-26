package com.example.quicknotes.viewmodel;

import android.app.Activity;
import android.view.View; 
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.quicknotes.model.OnboardingRepository;
import com.example.quicknotes.model.PermissionRepository;
import com.example.quicknotes.util.Event;

public class MainViewModel extends ViewModel {

    private final OnboardingRepository onboardingRepository;
    private final PermissionRepository permissionRepository;

    private final MutableLiveData<Boolean> _showOnboarding = new MutableLiveData<>();
    public LiveData<Boolean> showOnboarding = _showOnboarding;

    private final MutableLiveData<Event<String>> _snackbarMessage = new MutableLiveData<>();
    public LiveData<Event<String>> snackbarMessage = _snackbarMessage;

    private final MutableLiveData<Event<Boolean>> _showExactAlarmPermissionDialog = new MutableLiveData<>();
    public LiveData<Event<Boolean>> showExactAlarmPermissionDialog = _showExactAlarmPermissionDialog;

    private final MutableLiveData<Event<Boolean>> _signalRequestPostNotificationsPermission = new MutableLiveData<>();
    public LiveData<Event<Boolean>> signalRequestPostNotificationsPermission = _signalRequestPostNotificationsPermission;

    private final MutableLiveData<Event<Boolean>> _navigateToNewNote = new MutableLiveData<>();
    public LiveData<Event<Boolean>> navigateToNewNote = _navigateToNewNote;

    private final MutableLiveData<Event<Boolean>> _triggerAddDemoNotes = new MutableLiveData<>();
    public LiveData<Event<Boolean>> triggerAddDemoNotes = _triggerAddDemoNotes;

    private boolean wasWaitingForExactAlarmPermission = false;

    public MainViewModel(OnboardingRepository onboardingRepository, PermissionRepository permissionRepository) {
        this.onboardingRepository = onboardingRepository;
        this.permissionRepository = permissionRepository;
    }

    public void checkShouldShowOnboarding(Activity activity) {
        if (onboardingRepository.shouldShowOnboarding(activity)) {
            _showOnboarding.setValue(true);
        }
    }

    public void onboardingStarted() {
        _showOnboarding.setValue(false);
    }

    public void onboardingCompleted(Activity activity) {
        onboardingRepository.setOnboardingCompleted(activity);
        _snackbarMessage.setValue(new Event<>("Tutorial complete! You're ready to start taking notes."));
    }

    public void forceStartOnboarding() {
        _showOnboarding.setValue(true);
    }

    public void clearSnackbar() {
        _snackbarMessage.setValue(new Event<>(null));
    }

    public void checkPermissionsOnStart(Activity activity) {
        if (!permissionRepository.canScheduleExactAlarms(activity)) {
            _showExactAlarmPermissionDialog.setValue(new Event<>(true));
        }
    }

    public void exactAlarmPermissionDialogPositiveClicked(Activity activity) {
        wasWaitingForExactAlarmPermission = true;
        permissionRepository.requestExactAlarmPermission(activity);
    }

    public void checkExactAlarmPermissionOnResume(Activity activity) {
        if (wasWaitingForExactAlarmPermission && permissionRepository.canScheduleExactAlarms(activity)) {
            wasWaitingForExactAlarmPermission = false;
            _snackbarMessage.setValue(new Event<>("Alarm permission granted! You can now set note reminders."));
        }
    }

    public void userWantsToRequestPostNotificationsPermission(Activity activity) {
        if (permissionRepository.hasPostNotificationsPermission(activity)) {
            _signalRequestPostNotificationsPermission.setValue(new Event<>(true));
        }
    }

    public void triggerRequestPostNotificationsPermission(Activity activity) {
        if (permissionRepository.hasPostNotificationsPermission(activity)) {
            permissionRepository.requestPostNotificationsPermission(activity);
        }
    }

    public void handleRequestPermissionsResult(Activity activity, int requestCode, @NonNull int[] grantResults) {
        if (permissionRepository.handleRequestPermissionsResult(activity, requestCode, grantResults)) {
            _snackbarMessage.setValue(new Event<>("Notification permission granted!"));
        }
    }
    
    public void setNotifierRootView(Activity activity, View rootView) {
        permissionRepository.setNotifierRootView(activity, rootView);
    }

    public void userWantsToCreateFirstNote() {
        _navigateToNewNote.setValue(new Event<>(true));
    }

    public void userWantsToShowDemoNotes() {
        _triggerAddDemoNotes.setValue(new Event<>(true));
    }
}
