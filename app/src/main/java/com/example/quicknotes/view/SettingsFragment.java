package com.example.quicknotes.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.example.quicknotes.R;

import java.io.File;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);


        // OpenAI API Plugin
        EditTextPreference apiKeyPref = findPreference("openai_api_key");
        if (apiKeyPref != null) {
            apiKeyPref.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        }

        // Auto-tag limit preference
        SeekBarPreference limitPref = findPreference("auto_tag_limit");
        if (limitPref != null) {
            int current = PreferenceManager
                    .getDefaultSharedPreferences(requireContext())
                    .getInt("auto_tag_limit", 3);
            limitPref.setSummary(current + " tags per note");

            limitPref.setOnPreferenceChangeListener((pref, newValue) -> {
                int limit = (Integer) newValue;
                pref.setSummary(limit + " tags per note");
                return true;
            });
        }

        //Allow notifications preference

        SwitchPreferenceCompat notificationPref = findPreference("pref_noti");
        if (notificationPref != null){

        }


        // Delete all notes preference
        Preference deleteAll = findPreference("pref_delete_all");
        if (deleteAll != null) {
            deleteAll.setOnPreferenceClickListener(pref -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete All Notes")
                        .setMessage("Are you sure? This will permanently erase all your notes.")
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, (dlg1, which1) -> {
                            File file = new File(requireContext().getFilesDir(), "notes.json");
                            boolean deleted = file.delete();
                            if (deleted) {
                                new AlertDialog.Builder(requireContext())
                                        .setTitle("Notes Deleted")
                                        .setMessage("All notes have been deleted. The app will now exit.")
                                        .setPositiveButton(android.R.string.ok, (dlg2, which2) ->
                                                requireActivity().finishAffinity()).show();
                            } else {
                                Toast.makeText(requireContext(),
                                                "Failed to delete notes",
                                                Toast.LENGTH_SHORT).show();
                            }
                        }).show();
                return true;
            });
        }
    }

}