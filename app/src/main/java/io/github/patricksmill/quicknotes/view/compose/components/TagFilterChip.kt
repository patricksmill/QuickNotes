package io.github.patricksmill.quicknotes.view.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.view.compose.theme.QuickNotesTheme

@Composable
fun TagFilterChip(
    name: String,
    colorResId: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = colorResource(colorResId)
    val textColor = if (baseColor.luminance() > 0.5f) Color.Black else Color.White
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(name) },
        modifier = modifier.padding(end = 8.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = baseColor,
            labelColor = textColor,
            selectedContainerColor = baseColor,
            selectedLabelColor = textColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = baseColor.copy(alpha = 0.8f)
        )
    )
}

@Preview
@Composable
private fun TagFilterChipPreview() {
    QuickNotesTheme {
        TagFilterChip(
            name = "Work",
            colorResId = R.color.tag_color_blue,
            selected = true,
            onClick = {}
        )
    }
}
