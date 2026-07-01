package io.github.patricksmill.quicknotes.view.compose.screens

import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.model.tag.AiModelCatalog
import io.github.patricksmill.quicknotes.model.tag.TagSettingsManager
import io.github.patricksmill.quicknotes.view.compose.components.ListPickerBottomSheet
import io.github.patricksmill.quicknotes.view.compose.components.PickerItem
import io.github.patricksmill.quicknotes.view.compose.theme.QuickNotesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    tagSettingsManager: TagSettingsManager,
    modelCatalog: AiModelCatalog,
    appVersion: String,
    onBack: () -> Unit,
    onDeleteAllNotes: () -> Unit,
    onReplayTutorial: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var autoTagLimit by remember { mutableIntStateOf(tagSettingsManager.autoTagLimit) }
    var aiMode by remember { mutableStateOf(tagSettingsManager.isAiMode) }
    var confirmAi by remember { mutableStateOf(tagSettingsManager.isAiConfirmationEnabled) }
    var apiKey by remember { mutableStateOf(tagSettingsManager.apiKey.orEmpty()) }
    var model by remember { mutableStateOf(tagSettingsManager.selectedAiModelKey) }
    var baseUrl by remember { mutableStateOf(tagSettingsManager.selectedAiEndpoint) }
    var providerKey by remember {
        mutableStateOf(tagSettingsManager.selectedProvider.storageKey)
    }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showModelsSheet by remember { mutableStateOf(false) }
    var showProviderSheet by remember { mutableStateOf(false) }
    var isRefreshingModels by remember { mutableStateOf(false) }
    var modelSuggestions by remember {
        mutableStateOf(
            modelCatalog.mergedSuggestions(
                tagSettingsManager.selectedProvider,
                tagSettingsManager.selectedAiModelKey
            )
        )
    }
    fun refreshModels() {
        modelSuggestions = modelCatalog.mergedSuggestions(
            tagSettingsManager.selectedProvider,
            tagSettingsManager.selectedAiModelKey
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsSection("Notifications")
            SettingsRow("Manage notifications", "Open system notification settings") {
                onOpenNotificationSettings()
            }

            SettingsSection("Tagging")
            Text("Auto-Tag Limit: $autoTagLimit tags per note")
            Slider(
                value = autoTagLimit.toFloat(),
                onValueChange = {
                    autoTagLimit = it.toInt()
                    tagSettingsManager.setAutoTagLimit(autoTagLimit)
                },
                valueRange = 0f..5f,
                steps = 4,
                modifier = Modifier.fillMaxWidth()
            )

            SettingsSection("AI Features")
            SettingsRow(
                "AI Provider",
                TagSettingsManager.providerDisplayName(tagSettingsManager.selectedProvider)
            ) {
                showProviderSheet = true
            }
            RowSwitch("AI-powered Auto-Tagging", aiMode) {
                aiMode = it
                tagSettingsManager.setAiMode(it)
            }
            RowSwitch("Confirm AI tag suggestions", confirmAi) {
                confirmAi = it
                tagSettingsManager.setAiConfirmationEnabled(it)
            }
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    tagSettingsManager.saveApiKey(it)
                    refreshModels()
                },
                label = {
                    Text("${TagSettingsManager.providerDisplayName(tagSettingsManager.selectedProvider)} API Key")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            OutlinedTextField(
                value = model,
                onValueChange = {
                    model = it
                    tagSettingsManager.setAiModel(it)
                    refreshModels()
                },
                label = {
                    Text("${TagSettingsManager.providerDisplayName(tagSettingsManager.selectedProvider)} Model")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            SettingsRow("Model Library", modelSuggestions.take(3).joinToString(", ") { it.label }) {
                showModelsSheet = true
            }
            if (TagSettingsManager.providerForStorageKey(providerKey) == TagSettingsManager.AiProvider.CUSTOM) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        tagSettingsManager.setAiEndpoint(it)
                        refreshModels()
                    },
                    label = { Text("Custom API Base URL") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            SettingsSection("Danger Zone")
            SettingsRow("Delete All Notes", "Permanently remove all notes") {
                showDeleteDialog = true
            }

            SettingsSection("Help")
            SettingsRow("Replay Tutorial", "Go through onboarding again") {
                onReplayTutorial()
            }

            SettingsSection("About")
            Text("QuickNotes", style = MaterialTheme.typography.titleMedium)
            Text("Version: $appVersion", style = MaterialTheme.typography.bodyMedium)
        }
    }

    val sheetOpen = showProviderSheet || showModelsSheet
    BackHandler(enabled = sheetOpen) {
        when {
            showModelsSheet -> showModelsSheet = false
            showProviderSheet -> showProviderSheet = false
        }
    }

    BackHandler(enabled = showDeleteDialog) {
        showDeleteDialog = false
    }

    if (showProviderSheet) {
        val providers = listOf(
            TagSettingsManager.AiProvider.OPENAI,
            TagSettingsManager.AiProvider.ANTHROPIC,
            TagSettingsManager.AiProvider.GOOGLE,
            TagSettingsManager.AiProvider.CUSTOM
        )
        ListPickerBottomSheet(
            title = stringResource(R.string.ai_provider_title),
            items = providers.map { provider ->
                PickerItem(
                    label = TagSettingsManager.providerDisplayName(provider),
                    selected = provider.storageKey == providerKey
                )
            },
            onItemClick = { index ->
                val provider = providers[index]
                tagSettingsManager.setSelectedProvider(provider)
                providerKey = provider.storageKey
                apiKey = tagSettingsManager.apiKey.orEmpty()
                model = tagSettingsManager.selectedAiModelKey
                baseUrl = tagSettingsManager.selectedAiEndpoint
                refreshModels()
                showProviderSheet = false
            },
            onDismiss = { showProviderSheet = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete All Notes?") },
            text = { Text("This will permanently delete all notes. This action CANNOT be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAllNotes()
                    showDeleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showModelsSheet) {
        ListPickerBottomSheet(
            title = stringResource(R.string.model_library_title),
            items = modelSuggestions.map { option ->
                PickerItem(
                    label = option.label,
                    selected = option.id == model
                )
            },
            onItemClick = { index ->
                val option = modelSuggestions[index]
                tagSettingsManager.setAiModel(option.id)
                model = option.id
                refreshModels()
                showModelsSheet = false
            },
            onDismiss = { showModelsSheet = false },
            footer = {
                TextButton(
                    onClick = {
                        if (isRefreshingModels) return@TextButton
                        val provider = tagSettingsManager.selectedProvider
                        val key = tagSettingsManager.apiKeyFor(provider)
                        if (key.isNullOrBlank()) {
                            Toast.makeText(
                                context,
                                "Add an API key to refresh live model suggestions.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@TextButton
                        }
                        scope.launch {
                            isRefreshingModels = true
                            try {
                                val fetched = withContext(Dispatchers.IO) {
                                    modelCatalog.fetchSuggestions(
                                        provider = provider,
                                        endpoint = tagSettingsManager.endpointFor(provider),
                                        apiKey = key
                                    )
                                }
                                if (fetched.isNotEmpty()) {
                                    modelCatalog.saveCachedSuggestions(provider, fetched)
                                }
                                modelSuggestions = modelCatalog.mergedSuggestions(
                                    provider,
                                    tagSettingsManager.selectedAiModelKey,
                                    fetched
                                )
                                Toast.makeText(
                                    context,
                                    if (fetched.isEmpty()) {
                                        "No additional models were returned."
                                    } else {
                                        "Model suggestions updated."
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Could not refresh models: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                isRefreshingModels = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    enabled = !isRefreshingModels
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        if (isRefreshingModels) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(stringResource(R.string.refresh_from_api))
                    }
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
    HorizontalDivider()
}

@Composable
private fun SettingsRow(title: String, summary: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RowSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    QuickNotesTheme { Text("Preview requires dependencies") }
}
