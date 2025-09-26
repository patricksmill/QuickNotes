package com.example.quicknotes.view;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.example.quicknotes.R;
// import com.example.quicknotes.controller.MainActivity; // No longer directly needed for these actions
import com.example.quicknotes.model.NoteViewModel;
import com.example.quicknotes.viewmodel.MainViewModel; // Added MainViewModel import

public class SettingsFragment extends PreferenceFragmentCompat {
    private NoteViewModel noteViewModel;
    private MainViewModel mainViewModel; // Added MainViewModel field
    private SharedPreferences prefs;
    private NavController navController;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        setupApiKeyPreference();
        setupAutoTagLimitPreference();
        setupModelPreference();
        setupDeleteAllPreference();
        setupReplayTutorialPreference();
        setupNotificationPermissionRequest();
        setupAiToggleDependency();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize ViewModels scoped to the Activity
        noteViewModel = new ViewModelProvider(requireActivity()).get(NoteViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        navController = NavHostFragment.findNavController(this);
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

        int current = prefs != null ? prefs.getInt("auto_tag_limit", 3) : 3;
        limitPref.setSummary(current + " tags per note");
        limitPref.setOnPreferenceChangeListener((pref, newValue) -> {
            Integer value = null;
            if (newValue instanceof Integer) {
                value = (Integer) newValue;
            } else if (newValue instanceof String) {
                try {
                    value = Integer.parseInt((String) newValue);
                } catch (NumberFormatException ignored) {
                }
            }
            if (value == null) {
                return false;
            }
            pref.setSummary(value + " tags per note");
            return true;
        });
    }

    private void setupModelPreference() {
        androidx.preference.ListPreference modelPref = findPreference("pref_ai_model");
        if (modelPref == null) return;
        modelPref.setSummaryProvider(pref -> {
            CharSequence entry = ((androidx.preference.ListPreference) pref).getEntry();
            return entry != null ? entry : "Auto";
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
        if (replayTutorialPref != null && mainViewModel != null) { // Check mainViewModel
            replayTutorialPref.setOnPreferenceClickListener(preference -> {
                mainViewModel.forceStartOnboarding(); // Use MainViewModel to start onboarding
                navController.navigateUp();
                return true;
            });
        }
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete All Notes")
                .setMessage("Are you sure? This will permanently erase all your notes.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dlg1, which1) -> {
                    noteViewModel.deleteAllNotes();
                    showExitDialog();
                }).show();
    }

    private void showExitDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Notes Deleted")
                .setMessage("All notes have been deleted. The app will now exit.")
                .setPositiveButton(android.R.string.ok, (dlg2, which2) -> requireActivity().finishAffinity())
                .show();
    }

    private void setupNotificationPermissionRequest() {
        Preference allowNotiPref = findPreference("pref_noti");
        if (allowNotiPref == null) return;
        allowNotiPref.setOnPreferenceChangeListener((pref, newValue) -> {
            boolean enabled = Boolean.TRUE.equals(newValue);
            if (enabled && mainViewModel != null) {
                mainViewModel.userWantsToRequestPostNotificationsPermission(requireActivity()); // Pass Activity context
            }

            return true;
        });
    }

    private void setupAiToggleDependency() {
        SwitchPreferenceCompat aiToggle = findPreference("pref_ai_auto_tag");
        if (aiToggle == null) {
            return;
        }
        aiToggle.setOnPreferenceChangeListener((pref, newValue) -> {
            boolean enabled = Boolean.TRUE.equals(newValue);
            setAiPreferencesEnabled(enabled);
            return true;
        });
        setAiPreferencesEnabled(aiToggle.isChecked());
    }

    private void setAiPreferencesEnabled(boolean enabled) {
        Preference modelPref = findPreference("pref_ai_model");
        Preference apiPref = findPreference("openai_api_key");
        if (modelPref != null) {
            modelPref.setEnabled(enabled);
        }
        if (apiPref != null) {
            apiPref.setEnabled(enabled);
        }
    }
}
