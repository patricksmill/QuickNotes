package io.github.patricksmill.quicknotes.view.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.model.note.Note
import io.github.patricksmill.quicknotes.model.tag.Tag
import io.github.patricksmill.quicknotes.view.NotesUI
import io.github.patricksmill.quicknotes.view.compose.components.NoteListItemData
import io.github.patricksmill.quicknotes.view.compose.components.TagLabelChip
import io.github.patricksmill.quicknotes.view.compose.theme.QuickNotesTheme
import io.github.patricksmill.quicknotes.view.compose.util.tutorialTarget
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchNotesScreen(
    notes: List<Note>,
    tags: Collection<Tag>,
    listener: NotesUI.Listener,
    snackbarHostState: SnackbarHostState,
    onManageTags: () -> Unit,
    onOpenSettings: () -> Unit,
    onNoteClick: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var sortByDate by remember { mutableStateOf(true) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filteredNotes = remember(notes, selectedTag, sortByDate) {
        var result = notes.toList()
        selectedTag?.let { tag ->
            result = result.filter { note -> note.tags.any { it.name == tag } }
        }
        result.sortedWith { a, b ->
            when {
                a.isPinned != b.isPinned -> if (a.isPinned) -1 else 1
                sortByDate -> b.lastModified.compareTo(a.lastModified)
                else -> a.title.compareTo(b.title, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { listener.onNewNote() },
                modifier = Modifier.tutorialTarget(R.id.addNoteFab, "addNoteFab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = {
                                searchQuery = it
                                listener.onSearchNotes(it, true, true, true)
                            },
                            onSearch = {
                                listener.onSearchNotes(it, true, true, true)
                                searchActive = false
                            },
                            expanded = searchActive,
                            onExpandedChange = { searchActive = it },
                            placeholder = { Text("Search notes") },
                            modifier = Modifier.testTag("search_notes_field")
                        )
                    },
                    expanded = searchActive,
                    onExpandedChange = { searchActive = it },
                    modifier = Modifier
                        .weight(1f)
                        .tutorialTarget(R.id.search_bar, "search_bar")
                ) {}
                IconButton(onClick = { showSortDialog = true }) {
                    Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.sort))
                }
                IconButton(onClick = onManageTags) {
                    Icon(Icons.Filled.Label, contentDescription = stringResource(R.string.manage_tags))
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.tutorialTarget(R.id.settingsButton, "settingsButton")
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .tutorialTarget(R.id.tagRecyclerView, "tagRecyclerView"),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(tags.toList(), key = { it.name }) { tag ->
                    TagLabelChip(
                        name = tag.name,
                        colorResId = tag.colorResId,
                        onClick = {
                            selectedTag = if (selectedTag == tag.name) null else tag.name
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }

            if (filteredNotes.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_notes_msg),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .testTag("empty_state"),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        SwipeNoteRow(
                            note = note,
                            listener = listener,
                            snackbarHostState = snackbarHostState,
                            onNoteClick = onNoteClick
                        )
                    }
                }
            }
        }
    }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Sort Notes") },
            text = {
                Column {
                    TextButtonRow("Sort by Date") { sortByDate = true; showSortDialog = false }
                    TextButtonRow("Sort by Title") { sortByDate = false; showSortDialog = false }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showSortDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TextButtonRow(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}

@Preview
@Composable
private fun SearchNotesScreenPreview() {
    QuickNotesTheme {
        Box {
            Text("Preview requires listener")
        }
    }
}
