package io.github.patricksmill.quicknotes.view.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import io.github.patricksmill.quicknotes.model.note.Note
import io.github.patricksmill.quicknotes.view.NotesUI
import io.github.patricksmill.quicknotes.view.compose.components.NoteListItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeNoteRow(
    note: Note,
    listener: NotesUI.Listener,
    snackbarHostState: SnackbarHostState,
    onNoteClick: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                listener.onDeleteNote(note)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Note deleted",
                        actionLabel = "Undo"
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        listener.onUndoDelete()
                    }
                }
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete note",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        modifier = modifier,
        content = {
            NoteListItem(
                note = note,
                showNotificationIcon = listener.onShouldShowNotificationIcon(note),
                onClick = { onNoteClick(note) },
                onPinClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val wasPinned = note.isPinned
                    listener.onTogglePin(note)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Note ${if (wasPinned) "unpinned" else "pinned"}"
                        )
                    }
                }
            )
        }
    )
}
