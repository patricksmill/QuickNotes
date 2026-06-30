package io.github.patricksmill.quicknotes.view.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.view.compose.theme.QuickNotesTheme

@Composable
fun TagLabelChip(
    name: String,
    colorResId: Int,
    onClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = colorResource(colorResId)
    AssistChip(
        onClick = onClick ?: {},
        label = { Text(name) },
        modifier = modifier,
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(accent, CircleShape)
            )
        },
        trailingIcon = if (onDismiss != null) {
            {
                androidx.compose.material3.IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(18.dp)
                ) {
                    Text("×", style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            null
        },
        enabled = onClick != null || onDismiss != null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

@Preview
@Composable
private fun TagLabelChipPreview() {
    QuickNotesTheme {
        TagLabelChip(name = "Ideas", colorResId = R.color.tag_color_teal, onDismiss = {})
    }
}

@Composable
fun ManageTagRow(
    name: String,
    colorResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(colorResource(colorResId), CircleShape)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )
    }
}

@Preview
@Composable
private fun ManageTagRowPreview() {
    QuickNotesTheme {
        ManageTagRow(name = "Personal", colorResId = R.color.tag_color_green, onClick = {})
    }
}

@Composable
fun TagSuggestionRow(
    text: String,
    colorResId: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (colorResId != null) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(colorResource(colorResId), CircleShape)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = if (colorResId != null) 12.dp else 0.dp)
        )
    }
}

@Preview
@Composable
private fun TagSuggestionRowPreview() {
    QuickNotesTheme {
        TagSuggestionRow(text = "Create \"newtag\"", colorResId = null)
    }
}
