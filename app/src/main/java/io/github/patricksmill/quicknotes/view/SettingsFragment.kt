package io.github.patricksmill.quicknotes.view

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.model.tag.AiModelCatalog
import io.github.patricksmill.quicknotes.model.tag.TagSettingsManager
import java.util.concurrent.Executors

class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var tagSettingsManager: TagSettingsManager
    private lateinit var providerDataStore: ProviderAwarePreferenceDataStore
    private lateinit var modelCatalog: AiModelCatalog
    private val aiSettingsHandler = Handler(Looper.getMainLooper())
    private val aiSettingsExecutor = Executors.newSingleThreadExecutor()
    private var currentModelSuggestions: List<AiModelCatalog.ModelOption> = emptyList()

    override fun onDestroy() {
        aiSettingsExecutor.shutdownNow()
        super.onDestroy()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        tagSettingsManager = TagSettingsManager(requireContext())
        modelCatalog = AiModelCatalog(requireContext())
        providerDataStore = ProviderAwarePreferenceDataStore(
            tagSettingsManager,
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        preferenceManager.preferenceDataStore = providerDataStore
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

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
        val providerPref = findPreference<ListPreference>("pref_ai_provider")
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

        val apiKeyPref = findPreference<EditTextPreference>("pref_ai_api_key")
        apiKeyPref?.setOnPreferenceChangeListener { _, newValue ->
            tagSettingsManager.saveApiKey(newValue as String)
            loadModelSuggestions()
            if (!tagSettingsManager.apiKey.isNullOrBlank()) {
                refreshModelSuggestionsFromApi()
            }
            true
        }

        val modelPref = findPreference<EditTextPreference>("pref_ai_model")
        modelPref?.setOnPreferenceChangeListener { _, newValue ->
            tagSettingsManager.setAiModel(newValue as String)
            loadModelSuggestions()
            true
        }

        val modelSuggestionsPref = findPreference<Preference>("pref_ai_model_suggestions")
        modelSuggestionsPref?.setOnPreferenceClickListener {
            showModelSuggestionsDialog()
            true
        }

        val endpointPref = findPreference<EditTextPreference>("pref_ai_base_url")
        endpointPref?.setOnPreferenceChangeListener { _, newValue ->
            val endpoint = newValue as String
            tagSettingsManager.setAiEndpoint(endpoint)
            loadModelSuggestions()
            refreshModelSuggestionsFromApi()
            true
        }

        providerPref?.setOnPreferenceChangeListener { _, newValue ->
            tagSettingsManager.setSelectedProvider(
                TagSettingsManager.providerForStorageKey(newValue as String)
            )
            loadModelSuggestions()
            refreshModelSuggestionsFromApi()
            true
        }

        loadModelSuggestions()
    }

    private fun refreshAiPreferenceLabels() {
        val provider = tagSettingsManager.selectedProvider
        val providerName = TagSettingsManager.providerDisplayName(provider)

        findPreference<ListPreference>("pref_ai_provider")?.summary = providerName

        findPreference<EditTextPreference>("pref_ai_api_key")?.apply {
            title = "$providerName API Key"
            summary = if (tagSettingsManager.apiKey.isNullOrBlank()) "Not set" else "Saved"
        }

        findPreference<EditTextPreference>("pref_ai_model")?.apply {
            title = "$providerName Model"
            summary = tagSettingsManager.selectedAiModelKey
        }

        findPreference<Preference>("pref_ai_model_suggestions")?.apply {
            isVisible = true
            summary = formatModelSuggestionSummary(currentModelSuggestions, provider)
        }

        findPreference<EditTextPreference>("pref_ai_base_url")?.apply {
            isVisible = provider == TagSettingsManager.AiProvider.CUSTOM
            summary = tagSettingsManager.selectedAiEndpoint
        }
    }

    private fun loadModelSuggestions() {
        val provider = tagSettingsManager.selectedProvider
        val currentModel = tagSettingsManager.selectedAiModelKey
        currentModelSuggestions = modelCatalog.mergedSuggestions(provider, currentModel)
        refreshAiPreferenceLabels()
    }

    private fun showModelSuggestionsDialog() {
        val provider = tagSettingsManager.selectedProvider
        val options = if (currentModelSuggestions.isNotEmpty()) {
            currentModelSuggestions
        } else {
            modelCatalog.mergedSuggestions(provider, tagSettingsManager.selectedAiModelKey)
        }

        if (options.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Model Library")
                .setMessage("No built-in models are available yet. Enter a model manually below.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val labels = options.map { it.label }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Model Library")
            .setItems(labels) { _, which ->
                tagSettingsManager.setAiModel(options[which].id)
                loadModelSuggestions()
            }
            .setNeutralButton("Refresh from API") { _, _ ->
                refreshModelSuggestionsFromApi()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun refreshModelSuggestionsFromApi() {
        val provider = tagSettingsManager.selectedProvider
        val apiKey = tagSettingsManager.apiKeyFor(provider)

        if (apiKey.isNullOrBlank()) {
            aiSettingsHandler.post {
                Toast.makeText(
                    requireContext(),
                    "Add an API key to refresh live model suggestions.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        aiSettingsExecutor.execute {
            try {
                val fetched = modelCatalog.fetchSuggestions(
                    provider = provider,
                    endpoint = tagSettingsManager.endpointFor(provider),
                    apiKey = apiKey
                )
                if (fetched.isNotEmpty()) {
                    modelCatalog.saveCachedSuggestions(provider, fetched)
                }

                aiSettingsHandler.post {
                    if (provider == tagSettingsManager.selectedProvider) {
                        currentModelSuggestions = modelCatalog.mergedSuggestions(
                            provider = provider,
                            currentModel = tagSettingsManager.selectedAiModelKey,
                            liveSuggestions = fetched
                        )
                        refreshAiPreferenceLabels()
                    }

                    Toast.makeText(
                        requireContext(),
                        if (fetched.isEmpty()) {
                            "No additional models were returned."
                        } else {
                            "Model suggestions updated from the API."
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                aiSettingsHandler.post {
                    Toast.makeText(
                        requireContext(),
                        "Could not refresh models: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun formatModelSuggestionSummary(
        suggestions: List<AiModelCatalog.ModelOption>,
        provider: TagSettingsManager.AiProvider
    ): String {
        if (suggestions.isEmpty()) {
            return "Curated models available"
        }

        val preview = suggestions.take(3).joinToString(", ") { it.label }
        return if (suggestions.size > 3) {
            "$preview..."
        } else {
            preview
        } + " for ${TagSettingsManager.providerDisplayName(provider)}"
    }

    private class ProviderAwarePreferenceDataStore(
        private val settings: TagSettingsManager,
        private val sharedPreferences: SharedPreferences
    ) : PreferenceDataStore() {
        override fun putString(key: String?, value: String?) {
            when (key) {
                "pref_ai_provider" -> {
                    if (!value.isNullOrBlank()) {
                        settings.setSelectedProvider(
                            TagSettingsManager.providerForStorageKey(value)
                        )
                    }
                }
                "pref_ai_api_key" -> settings.saveApiKey(value.orEmpty())
                "pref_ai_model" -> settings.setAiModel(value.orEmpty())
                "pref_ai_base_url" -> settings.setAiEndpoint(value.orEmpty())
                else -> if (key != null) sharedPreferences.edit().putString(key, value).apply()
            }
        }

        override fun getString(key: String?, defValue: String?): String? {
            return when (key) {
                "pref_ai_provider" ->
                    sharedPreferences.getString(key, null) ?: settings.selectedProvider.storageKey
                "pref_ai_api_key" -> settings.apiKey.orEmpty()
                "pref_ai_model" -> settings.selectedAiModelKey
                "pref_ai_base_url" -> settings.selectedAiEndpoint
                else -> sharedPreferences.getString(key, defValue)
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            if (key != null) {
                sharedPreferences.edit().putBoolean(key, value).apply()
            }
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return if (key != null) sharedPreferences.getBoolean(key, defValue) else defValue
        }

        override fun putInt(key: String?, value: Int) {
            if (key != null) {
                sharedPreferences.edit().putInt(key, value).apply()
            }
        }

        override fun getInt(key: String?, defValue: Int): Int {
            return if (key != null) sharedPreferences.getInt(key, defValue) else defValue
        }
    }
}
