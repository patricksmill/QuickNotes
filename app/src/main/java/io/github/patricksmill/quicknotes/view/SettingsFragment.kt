package io.github.patricksmill.quicknotes.view

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.model.tag.TagSettingsManager

class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var tagSettingsManager: TagSettingsManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        tagSettingsManager = TagSettingsManager(requireContext())

        setupVersionPref()
        setupDeleteAllPref()
        setupReplayTutorialPref()
        setupNotifications()
        setupAiPrefs()
    }

    private fun setupVersionPref() {
        val pm = requireContext().packageManager
        val pkg = requireContext().packageName
        val version = try {
            val info = pm.getPackageInfo(pkg, 0)
            info.versionName ?: ""
        } catch (_: Exception) { "" }
        findPreference<Preference>("app_version")?.summary = "Version: $version"
    }

    private fun setupDeleteAllPref() {
        findPreference<Preference>("pref_delete_all")?.setOnPreferenceClickListener {
            showDeleteAllConfirmationDialog()
            true
        }
    }

    private fun showDeleteAllConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete All Notes?")
            .setMessage("This will permanently delete all notes. This action CANNOT be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                (activity as? io.github.patricksmill.quicknotes.controller.ControllerActivity)?.onDeleteAllNotes()
            }
            .show()
    }

    private fun setupReplayTutorialPref() {
        findPreference<Preference>("pref_replay_tutorial")?.setOnPreferenceClickListener {
            (activity as? io.github.patricksmill.quicknotes.controller.ControllerActivity)?.forceStartTutorialFromSettings()
            true
        }
    }

    private fun setupNotifications() {
        findPreference<Preference>("pref_manage_notifications")?.setOnPreferenceClickListener {
            (activity as? io.github.patricksmill.quicknotes.controller.ControllerActivity)?.openSystemNotificationSettings()
            true
        }
    }

    private fun setupAiPrefs() {
        val autoTagLimitPref = findPreference<SeekBarPreference>("auto_tag_limit")
        autoTagLimitPref?.summary = tagSettingsManager.autoTagLimit.toString()
        autoTagLimitPref?.setOnPreferenceChangeListener { _, newValue ->
            val limit = newValue as Int
            tagSettingsManager.setAutoTagLimit(limit)
            autoTagLimitPref.summary = limit.toString()
            true
        }

        val aiModePref = findPreference<SwitchPreferenceCompat>("pref_ai_auto_tag")
        aiModePref?.isChecked = tagSettingsManager.isAiMode
        aiModePref?.setOnPreferenceChangeListener { _, newValue ->
            tagSettingsManager.setAiMode(newValue as Boolean)
            true
        }

        val confirmAiPref = findPreference<SwitchPreferenceCompat>("pref_ai_confirm")
        confirmAiPref?.isChecked = tagSettingsManager.isAiConfirmationEnabled
        confirmAiPref?.setOnPreferenceChangeListener { _, newValue ->
            tagSettingsManager.setAiConfirmationEnabled(newValue as Boolean)
            true
        }


        val apiKeyPref = findPreference<EditTextPreference>("openai_api_key")
        apiKeyPref?.setOnPreferenceChangeListener { _, newValue ->
            tagSettingsManager.setApiKey(newValue as String)
            true
        }
    }
}