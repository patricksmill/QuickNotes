package io.github.patricksmill.quicknotes.view.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.patricksmill.quicknotes.model.tag.TagRepository
import io.github.patricksmill.quicknotes.view.compose.theme.QuickNotesTheme

private val CURATED_COLOR_INDICES = listOf(0, 1, 2, 5, 6, 8, 9, 11, 13, 14, 16, 18)

fun curatedColorOptions(all: List<TagRepository.ColorOption?>): List<TagRepository.ColorOption> {
    return CURATED_COLOR_INDICES.mapNotNull { index ->
        all.getOrNull(index)?.let { option -> option }
    }
}

@Composable
fun TagColorSwatch(
    colorResId: Int,
    colorName: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fill = colorResource(colorResId)
    val checkColor = if (fill.luminance() > 0.5f) Color.Black else Color.White
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(fill.copy(alpha = 0.85f))
            .then(
                if (selected) Modifier else Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    CircleShape
                )
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = colorName },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = checkColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagColorPickerSheet(
    tagName: String,
    options: List<TagRepository.ColorOption>,
    selectedResId: Int?,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "Select color for '$tagName'",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(48.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
            items(options, key = { it.resId }) { option ->
                TagColorSwatch(
                    colorResId = option.resId,
                    colorName = option.name,
                    selected = option.resId == selectedResId,
                    onClick = {
                        onColorSelected(option.resId)
                        onDismiss()
                    }
                )
            }
        }
        }
    }
}

@Preview
@Composable
private fun TagColorSwatchPreview() {
    QuickNotesTheme {
        TagColorSwatch(
            colorResId = io.github.patricksmill.quicknotes.R.color.tag_color_blue,
            colorName = "Blue",
            selected = true,
            onClick = {}
        )
    }
}
