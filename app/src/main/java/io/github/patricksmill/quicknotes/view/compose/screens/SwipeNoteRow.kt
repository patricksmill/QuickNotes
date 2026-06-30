package io.github.patricksmill.quicknotes.view.compose.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.patricksmill.quicknotes.model.note.Note
import io.github.patricksmill.quicknotes.view.NotesUI
import io.github.patricksmill.quicknotes.view.compose.components.NoteListItem
import io.github.patricksmill.quicknotes.view.compose.components.NoteListItemData
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
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
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
        backgroundContent = {},
        modifier = modifier,
        content = {
            NoteListItem(
                note = NoteListItemData(
                    title = note.title,
                    content = note.content,
                    tagNames = note.tagNames.toList(),
                    lastModified = note.lastModified,
                    isPinned = note.isPinned,
                    showNotificationIcon = listener.onShouldShowNotificationIcon(note)
                ),
                onClick = { onNoteClick(note) },
                onPinClick = {
                    listener.onTogglePin(note)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Note ${if (note.isPinned) "unpinned" else "pinned"}"
                        )
                    }
                }
            )
        }
    )
}
