package com.example.quicknotes.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

import com.example.quicknotes.R;
import com.example.quicknotes.controller.ControllerActivity;
import com.google.android.material.snackbar.Snackbar;

/**
 * Fragment for managing application settings and preferences.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements NotesUI {
    private NotesUI.Listener listener;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        setupApiKeyPreference();
        setupAutoTagLimitPreference();
        setupDeleteAllPreference();
        setupReplayTutorialPreference();
        setupNotificationPermissionRequest();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof ControllerActivity) {
            setListener((NotesUI.Listener) getActivity());
        }
    }

    private void setupApiKeyPreference() {
        EditTextPreference apiKeyPref = findPreference("openai_api_key");
        if (apiKeyPref != null) {
            apiKeyPref.setSummaryProvider(preference -> {
                String value = ((EditTextPreference) preference).getText();
                if (value == null || value.trim().isEmpty()) {
                    return "Not set";
                }
                return "Set";
            });
        }
    }

    private void setupAutoTagLimitPreference() {
        SeekBarPreference limitPref = findPreference("auto_tag_limit");
        if (limitPref == null) return;

        int current = PreferenceManager.getDefaultSharedPreferences(requireContext()).getInt("auto_tag_limit", 3);
        limitPref.setSummary(current + " tags per note");
        limitPref.setOnPreferenceChangeListener((pref, newValue) -> {
            pref.setSummary(newValue + " tags per note");
            return true;
        });
    }

    private void setupDeleteAllPreference() {
        Preference deleteAll = findPreference("pref_delete_all");
        if (deleteAll == null) return;

        deleteAll.setOnPreferenceClickListener(pref -> {
            showDeleteConfirmation();
            return true;
        });
    }

    private void setupReplayTutorialPreference() {
        Preference replayTutorialPref = findPreference("pref_replay_tutorial");
        if (replayTutorialPref != null && getActivity() instanceof ControllerActivity) {
            replayTutorialPref.setOnPreferenceClickListener(preference -> {
                ControllerActivity activity = (ControllerActivity) getActivity();
                activity.startOnboardingTutorial();
                // Navigate back to main screen for the tutorial
                if (listener != null) {
                    listener.onBrowseNotes();
                }
                return true;
            });
        }
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete All Notes")
                .setMessage("Are you sure? This will permanently erase all your notes.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dlg1, which1) -> {
                    if (listener != null) {
                        listener.onDeleteAllNotes();
                        showExitDialog();
                    } else {
                        showError();
                    }
                }).show();
    }

    private void showExitDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Notes Deleted")
                .setMessage("All notes have been deleted. The app will now exit.")
                .setPositiveButton(android.R.string.ok, (dlg2, which2) -> requireActivity().finishAffinity())
                .show();
    }

    /**
     * Shows an error message using Snackbar
     */
    private void showError() {
        View view = getView();
        if (view != null) {
            Snackbar.make(view, "Unable to delete notes at this time", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void setListener(NotesUI.Listener listener) {
        this.listener = listener;
    }

    @Override
    public void updateView(java.util.List<com.example.quicknotes.model.Note> notes) {
        // This method is not used in this fragment
    }

    private void setupNotificationPermissionRequest() {
        Preference allowNotiPref = findPreference("pref_noti");
        if (allowNotiPref == null) return;
        allowNotiPref.setOnPreferenceChangeListener((pref, newValue) -> {
            boolean enabled = Boolean.TRUE.equals(newValue);
            if (enabled && getActivity() instanceof ControllerActivity) {
                ((ControllerActivity) getActivity()).requestNotificationPermissionFromSettings();
            }
            return true;
        });
    }
}