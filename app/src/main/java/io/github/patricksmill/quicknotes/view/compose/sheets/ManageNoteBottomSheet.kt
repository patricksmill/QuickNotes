package io.github.patricksmill.quicknotes.view.compose.sheets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.model.note.Note
import io.github.patricksmill.quicknotes.model.tag.TagSettingsManager
import io.github.patricksmill.quicknotes.view.NotesUI
import io.github.patricksmill.quicknotes.view.compose.components.TagColorPickerSheet
import io.github.patricksmill.quicknotes.view.compose.components.TagLabelChip
import io.github.patricksmill.quicknotes.view.compose.components.curatedColorOptions
import io.github.patricksmill.quicknotes.view.compose.theme.QuickNotesTheme
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageNoteBottomSheet(
    note: Note,
    isNewNote: Boolean,
    listener: NotesUI.Listener,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    showMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) { mutableStateOf(note.content) }
    val selectedTags = remember(note.id) {
        mutableStateListOf<String>().apply { addAll(note.tagNames) }
    }
    var tagsDirty by remember(note.id) { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var aiLoading by remember { mutableStateOf(false) }
    var notificationsEnabled by remember(note.id) { mutableStateOf(note.isNotificationsEnabled) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val cal = remember(note.id) {
        Calendar.getInstance().apply {
            note.notificationDate?.let { time = it } ?: add(Calendar.HOUR_OF_DAY, 1)
        }
    }
    var colorPickerTag by remember { mutableStateOf<String?>(null) }
    var renameTag by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTag by remember { mutableStateOf<String?>(null) }
    var aiSuggestions by remember { mutableStateOf<List<String>?>(null) }
    var aiConfigDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val colorOptions = remember(listener) {
        curatedColorOptions(listener.onGetAvailableColors()?.filterNotNull().orEmpty())
    }

    val suggestions = remember(tagInput, selectedTags, listener) {
        buildSuggestions(tagInput, selectedTags.toSet(), listener)
    }

    fun persistTags() {
        if (!tagsDirty) return
        note.tags.clear()
        listener.onSetTags(note, selectedTags.map { it as String? }.toMutableList())
        tagsDirty = false
    }

    fun addTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || selectedTags.any { it.equals(trimmed, ignoreCase = true) }) return
        selectedTags.add(trimmed)
        tagsDirty = true
        tagInput = ""
        persistTags()
    }

    fun isDirty(): Boolean {
        if (title.trim() != note.title.trim()) return true
        if (content.trim() != note.content.trim()) return true
        val currentTags = selectedTags.map { it.lowercase() }.sorted()
        val savedTags = note.tagNames.map { it.lowercase() }.sorted()
        if (currentTags != savedTags) return true
        if (notificationsEnabled != note.isNotificationsEnabled) return true
        if (notificationsEnabled) {
            val saved = note.notificationDate?.time
            if (saved == null || saved != cal.timeInMillis) return true
        }
        return false
    }

    fun requestDismiss() {
        if (isDirty()) showDiscardDialog = true else onDismiss()
    }

    BackHandler {
        when {
            aiSuggestions != null -> aiSuggestions = null
            aiConfigDialog -> aiConfigDialog = false
            deleteTag != null -> deleteTag = null
            renameTag != null -> renameTag = null
            colorPickerTag != null -> colorPickerTag = null
            showTimePicker -> showTimePicker = false
            showDatePicker -> showDatePicker = false
            showDiscardDialog -> showDiscardDialog = false
            else -> requestDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = { requestDismiss() }, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { requestDismiss() }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(android.R.string.cancel))
                }
                Button(
                    onClick = {
                        val t = title.trim()
                        val c = content.trim()
                        if (t.isEmpty() || c.isEmpty()) {
                            showMessage(context.getString(R.string.missing_item_field_error))
                            return@Button
                        }
                        note.title = t
                        note.content = c
                        if (tagsDirty) persistTags()
                        note.lastModified = Date()
                        if (isNewNote && listener.onShouldConfirmAiSuggestions() && listener.onIsAiTaggingConfigured()) {
                            listener.onAiSuggestTags(note, 5, { suggestions ->
                                if (!suggestions.isNullOrEmpty()) {
                                    aiSuggestions = suggestions.filterNotNull()
                                }
                            }, { })
                        }
                        if (handleNotificationSave(notificationsEnabled, cal, listener, note, showMessage)) {
                            listener.onSaveNote(note, isNewNote)
                            onSaved()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.save)) }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.note_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.note_content)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                minLines = 5
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text(stringResource(R.string.tags_add_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addTag(tagInput) })
                    )
                    if (tagInput.isNotBlank() && suggestions.isNotEmpty()) {
                        suggestions.take(6).forEach { suggestion ->
                            TextButton(
                                onClick = { addTag(suggestion.value) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = suggestion.label,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
                if (aiLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp))
                } else {
                    IconButton(
                        onClick = {
                            val settings = TagSettingsManager(context)
                            if (!settings.isAiMode || !settings.hasValidApiKey()) {
                                aiConfigDialog = true
                                return@IconButton
                            }
                            val temp = Note(title.trim(), content.trim(), null)
                            if (title.isBlank() && content.isBlank()) {
                                showMessage("Enter a title or content first")
                                return@IconButton
                            }
                            aiLoading = true
                            listener.onAiSuggestTags(temp, 5, { result ->
                                aiLoading = false
                                if (result.isNullOrEmpty()) showMessage("No suggestions")
                                else aiSuggestions = result.filterNotNull()
                            }, { err ->
                                aiLoading = false
                                showMessage("Suggest failed: $err")
                            })
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "AI suggest tags")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp)
            ) {
                selectedTags.forEach { tagName ->
                    TagLabelChip(
                        name = tagName,
                        colorResId = listener.onGetTagColor(tagName),
                        onClick = {
                            renameTag = tagName
                            renameText = tagName
                        },
                        onDismiss = {
                            selectedTags.remove(tagName)
                            tagsDirty = true
                            persistTags()
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.notification), modifier = Modifier.weight(1f))
                Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
            }
            if (notificationsEnabled) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}")
                    }
                    TextButton(onClick = { showTimePicker = true }) {
                        Text("${cal.get(Calendar.HOUR_OF_DAY)}:${cal.get(Calendar.MINUTE).toString().padStart(2, '0')}")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = cal.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { cal.timeInMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    cal.set(Calendar.HOUR_OF_DAY, state.hour)
                    cal.set(Calendar.MINUTE, state.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = state) }
        )
    }

    renameTag?.let { tagName ->
        AlertDialog(
            onDismissRequest = { renameTag = null },
            title = { Text(tagName) },
            text = {
                Column {
                    TextButton(onClick = {
                        renameTag = null
                        colorPickerTag = tagName
                    }) { Text("Change color") }
                    OutlinedTextField(value = renameText, onValueChange = { renameText = it }, label = { Text("Rename") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newName = renameText.trim()
                    if (newName.isNotEmpty() && !newName.equals(tagName, true)) {
                        listener.onRenameTag(tagName, newName)
                        selectedTags.remove(tagName)
                        selectedTags.add(newName)
                        tagsDirty = true
                        persistTags()
                        onRefresh()
                    }
                    renameTag = null
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTag = tagName; renameTag = null }) { Text("Delete") }
            }
        )
    }

    colorPickerTag?.let { tagName ->
        TagColorPickerSheet(
            tagName = tagName,
            options = colorOptions,
            selectedResId = listener.onGetTagColor(tagName),
            onColorSelected = { resId ->
                listener.onSetTagColor(tagName, resId)
                onRefresh()
            },
            onDismiss = { colorPickerTag = null }
        )
    }

    deleteTag?.let { tagName ->
        AlertDialog(
            onDismissRequest = { deleteTag = null },
            title = { Text("Delete tag") },
            text = { Text("Remove '$tagName' from all notes?") },
            confirmButton = {
                TextButton(onClick = {
                    listener.onDeleteTag(tagName)
                    selectedTags.remove(tagName)
                    tagsDirty = true
                    persistTags()
                    onRefresh()
                    deleteTag = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTag = null }) { Text("Cancel") } }
        )
    }

    aiSuggestions?.let { items ->
        var checked by remember(items) { mutableStateOf(items.map { false }) }
        AlertDialog(
            onDismissRequest = { aiSuggestions = null },
            title = { Text("AI tag suggestions") },
            text = {
                Column {
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { checked = checked.toMutableList().also { it[index] = !it[index] } }
                                .padding(8.dp)
                        ) {
                            Text(if (checked.getOrElse(index) { false }) "☑" else "☐")
                            Text(item, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    items.forEachIndexed { index, item -> if (checked.getOrElse(index) { false }) addTag(item) }
                    aiSuggestions = null
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { aiSuggestions = null }) { Text("Cancel") } }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes that will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onDismiss()
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") }
            }
        )
    }

    if (aiConfigDialog) {
        AlertDialog(
            onDismissRequest = { aiConfigDialog = false },
            title = { Text("AI Tagging Not Configured") },
            text = { Text("Enable AI auto-tagging and add an API key in Settings.") },
            confirmButton = {
                TextButton(onClick = {
                    aiConfigDialog = false
                    note.title = title.trim()
                    note.content = content.trim()
                    if (tagsDirty) persistTags()
                    listener.onSaveNote(note, isNewNote)
                    onDismiss()
                    onOpenSettings()
                }) { Text("Open Settings") }
            },
            dismissButton = { TextButton(onClick = { aiConfigDialog = false }) { Text("Cancel") } }
        )
    }
}

private data class TagSuggestionItem(val label: String, val value: String)

private fun buildSuggestions(
    query: String,
    selected: Set<String>,
    listener: NotesUI.Listener
): List<TagSuggestionItem> {
    val tm = listener.onManageTags() ?: return emptyList()
    val all = tm.allTags.toList()
    val lower = query.lowercase()
    val filtered = all
        .filter { tag -> selected.none { it.equals(tag.name, ignoreCase = true) } }
        .filter { lower.isEmpty() || it.name.lowercase().contains(lower) }
        .sortedBy { it.name }
        .map { TagSuggestionItem(it.name, it.name) }
        .toMutableList()
    if (query.isNotBlank() && all.none { it.name.equals(query, ignoreCase = true) }) {
        filtered.add(0, TagSuggestionItem("Create \"$query\"", query))
    }
    return filtered
}

private fun handleNotificationSave(
    enabled: Boolean,
    cal: Calendar,
    listener: NotesUI.Listener,
    note: Note,
    showMessage: (String) -> Unit
): Boolean {
    var selectedDate: Date? = null
    if (enabled) {
        selectedDate = cal.time
        if (!listener.onValidateNotificationDate(selectedDate)) {
            showMessage("Cannot set notification for past date/time. Please select a future time.")
            return false
        }
    }
    listener.onSetNotification(note, enabled, selectedDate)
    return true
}

@Preview
@Composable
private fun ManageNoteBottomSheetPreview() {
    QuickNotesTheme { Text("Preview requires listener") }
}
