package io.github.patricksmill.quicknotes.view.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.view.compose.theme.QuickNotesTheme

enum class NotesEmptyStateKind {
    NoNotes,
    NoResults
}

@Composable
fun NotesEmptyState(
    kind: NotesEmptyStateKind,
    onCreateNote: () -> Unit,
    onClearFilters: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector
    val titleRes: Int
    val bodyRes: Int
    when (kind) {
        NotesEmptyStateKind.NoNotes -> {
            icon = Icons.Outlined.EditNote
            titleRes = R.string.empty_state_no_notes_title
            bodyRes = R.string.empty_state_no_notes_body
        }
        NotesEmptyStateKind.NoResults -> {
            icon = Icons.Outlined.SearchOff
            titleRes = R.string.empty_state_no_results_title
            bodyRes = R.string.empty_state_no_results_body
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp)
            .testTag("empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        if (kind == NotesEmptyStateKind.NoNotes) {
            Button(onClick = onCreateNote) {
                Text(stringResource(R.string.empty_state_create_note))
            }
        }
        if (kind == NotesEmptyStateKind.NoResults && onClearFilters != null) {
            TextButton(onClick = onClearFilters) {
                Text(stringResource(R.string.empty_state_clear_filters))
            }
        }
    }
}

@Preview
@Composable
private fun NotesEmptyStateNoNotesPreview() {
    QuickNotesTheme {
        NotesEmptyState(
            kind = NotesEmptyStateKind.NoNotes,
            onCreateNote = {},
            onClearFilters = null
        )
    }
}

@Preview
@Composable
private fun NotesEmptyStateNoResultsPreview() {
    QuickNotesTheme {
        NotesEmptyState(
            kind = NotesEmptyStateKind.NoResults,
            onCreateNote = {},
            onClearFilters = {}
        )
    }
}
