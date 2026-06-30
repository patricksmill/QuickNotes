package io.github.patricksmill.quicknotes.view.compose.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.model.tag.Tag
import io.github.patricksmill.quicknotes.model.tag.TagRepository
import io.github.patricksmill.quicknotes.view.NotesUI
import io.github.patricksmill.quicknotes.view.compose.components.ManageTagRow
import io.github.patricksmill.quicknotes.view.compose.components.TagColorPickerSheet
import io.github.patricksmill.quicknotes.view.compose.components.curatedColorOptions
import io.github.patricksmill.quicknotes.view.compose.theme.QuickNotesTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTagsBottomSheet(
    tags: List<Tag>,
    listener: NotesUI.Listener,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var actionTag by remember { mutableStateOf<Tag?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.manage_tags_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.settings))
                }
            }
            tags.forEach { tag ->
                ManageTagRow(
                    name = tag.name,
                    colorResId = listener.onGetTagColor(tag.name),
                    onClick = { actionTag = tag },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    var colorPickerTag by remember { mutableStateOf<Tag?>(null) }
    var renameTag by remember { mutableStateOf<Tag?>(null) }
    var deleteTag by remember { mutableStateOf<Tag?>(null) }
    var renameText by remember { mutableStateOf("") }
    val colorOptions = remember(listener) {
        curatedColorOptions(listener.onGetAvailableColors()?.filterNotNull().orEmpty())
    }

    actionTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { actionTag = null },
            title = { Text(tag.name) },
            text = {
                Column {
                    TextButton(onClick = {
                        actionTag = null
                        colorPickerTag = tag
                    }) { Text("Change color") }
                    TextButton(onClick = {
                        actionTag = null
                        renameTag = tag
                        renameText = tag.name
                    }) { Text("Rename") }
                    TextButton(onClick = {
                        actionTag = null
                        deleteTag = tag
                    }) { Text("Delete") }
                }
            },
            confirmButton = {
                TextButton(onClick = { actionTag = null }) { Text("Close") }
            }
        )
    }

    colorPickerTag?.let { tag ->
        TagColorPickerSheet(
            tagName = tag.name,
            options = colorOptions,
            selectedResId = listener.onGetTagColor(tag.name),
            onColorSelected = { resId ->
                listener.onSetTagColor(tag.name, resId)
            },
            onDismiss = { colorPickerTag = null }
        )
    }

    renameTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { renameTag = null },
            title = { Text(tag.name) },
            text = {
                Column {
                    TextButton(onClick = {
                        renameTag = null
                        colorPickerTag = tag
                    }) { Text("Change color") }
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("Rename tag") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newName = renameText.trim()
                    if (newName.isNotEmpty() && !newName.equals(tag.name, true)) {
                        listener.onRenameTag(tag.name, newName)
                    }
                    renameTag = null
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = {
                    deleteTag = tag
                    renameTag = null
                }) { Text("Delete") }
            }
        )
    }

    deleteTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { deleteTag = null },
            title = { Text("Delete tag") },
            text = { Text("Remove '${tag.name}' from all notes?") },
            confirmButton = {
                TextButton(onClick = {
                    listener.onDeleteTag(tag.name)
                    deleteTag = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTag = null }) { Text("Cancel") }
            }
        )
    }
}

@Preview
@Composable
private fun ManageTagsBottomSheetPreview() {
    QuickNotesTheme {
        Text("Preview requires listener")
    }
}
