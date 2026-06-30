package io.github.patricksmill.quicknotes.view.compose.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.patricksmill.quicknotes.model.note.Note
import io.github.patricksmill.quicknotes.model.tag.AiModelCatalog
import io.github.patricksmill.quicknotes.model.tag.Tag
import io.github.patricksmill.quicknotes.model.tag.TagSettingsManager
import io.github.patricksmill.quicknotes.view.NotesUI
import io.github.patricksmill.quicknotes.view.compose.screens.SearchNotesScreen
import io.github.patricksmill.quicknotes.view.compose.screens.SettingsScreen
import io.github.patricksmill.quicknotes.view.compose.sheets.ManageNoteBottomSheet
import io.github.patricksmill.quicknotes.view.compose.sheets.ManageTagsBottomSheet

@Composable
fun QuickNotesNavHost(
    notes: List<Note>,
    tags: List<Tag>,
    revision: Int,
    searchQuery: String,
    listener: NotesUI.Listener,
    snackbarHostState: SnackbarHostState,
    tagSettingsManager: TagSettingsManager,
    modelCatalog: AiModelCatalog,
    appVersion: String,
    noteToEdit: Note?,
    openSettingsRequest: Boolean,
    onOpenSettingsConsumed: () -> Unit,
    onNoteToEditConsumed: () -> Unit,
    onRefresh: () -> Unit,
    onReplayTutorial: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }
    var manageTagsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(openSettingsRequest) {
        if (openSettingsRequest) {
            showSettings = true
            onOpenSettingsConsumed()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (showSettings) {
                SettingsScreen(
                    tagSettingsManager = tagSettingsManager,
                    modelCatalog = modelCatalog,
                    appVersion = appVersion,
                    onBack = { showSettings = false },
                    onDeleteAllNotes = { listener.onDeleteAllNotes() },
                    onReplayTutorial = {
                        showSettings = false
                        onReplayTutorial()
                    },
                    onOpenNotificationSettings = onOpenNotificationSettings
                )
            } else {
                SearchNotesScreen(
                    notes = notes,
                    tags = tags,
                    revision = revision,
                    searchQuery = searchQuery,
                    listener = listener,
                    snackbarHostState = snackbarHostState,
                    onManageTags = { manageTagsVisible = true },
                    onOpenSettings = { showSettings = true },
                    onNoteClick = { listener.onManageNotes(it) }
                )
            }
        }
    }

    noteToEdit?.let { note ->
        val isNew = note.title.isEmpty() && note.content.isEmpty()
        ManageNoteBottomSheet(
            note = note,
            isNewNote = isNew,
            listener = listener,
            onDismiss = onNoteToEditConsumed,
            onSaved = onRefresh,
            onOpenSettings = {
                onNoteToEditConsumed()
                showSettings = true
            },
            onRefresh = onRefresh,
            showMessage = showMessage
        )
    }

    if (manageTagsVisible) {
        ManageTagsBottomSheet(
            tags = tags,
            listener = listener,
            onDismiss = { manageTagsVisible = false }
        )
    }
}
