package io.github.patricksmill.quicknotes.view.compose.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.model.TutorialManager
import io.github.patricksmill.quicknotes.view.compose.theme.QuickNotesTheme
import io.github.patricksmill.quicknotes.view.compose.util.TutorialTargets

private val SpotlightPadding = 12.dp
private val CardGap = 16.dp
private val EdgePadding = 16.dp
private val EstimatedCardHeight = 220.dp

private data class SpotlightDp(
    val top: Dp,
    val bottom: Dp,
    val left: Dp,
    val right: Dp
)

@Composable
fun TutorialOverlay(
    step: TutorialManager.TutorialStep,
    onAction: (TutorialManager.TutorialStep.StepAction) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val rawSpotlight = if (step.targetViewId != -1) {
        TutorialTargets.bounds[step.targetViewId]
    } else {
        null
    }
    val spotlightDp = rawSpotlight?.let { rect ->
        with(density) {
            SpotlightDp(
                top = rect.top.toDp() - SpotlightPadding,
                bottom = rect.bottom.toDp() + SpotlightPadding,
                left = rect.left.toDp() - SpotlightPadding,
                right = rect.right.toDp() + SpotlightPadding
            )
        }
    }
    val spotlightPx = rawSpotlight?.let { rect ->
        with(density) {
            Rect(
                left = rect.left - SpotlightPadding.toPx(),
                top = rect.top - SpotlightPadding.toPx(),
                right = rect.right + SpotlightPadding.toPx(),
                bottom = rect.bottom + SpotlightPadding.toPx()
            )
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Color.Black.copy(alpha = 0.7f))
            spotlightPx?.let { rect ->
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
            }
        }

        val cardModifier = if (spotlightDp != null) {
            val spaceBelow = maxHeight - spotlightDp.bottom - CardGap
            val spaceAbove = spotlightDp.top - CardGap
            val placeBelow = spaceBelow >= EstimatedCardHeight ||
                (spaceBelow >= spaceAbove && spaceBelow >= 80.dp)
            val yOffset = if (placeBelow) {
                spotlightDp.bottom + CardGap
            } else {
                (spotlightDp.top - EstimatedCardHeight - CardGap).coerceAtLeast(EdgePadding)
            }
            Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = EdgePadding)
                .offset(y = yOffset)
                .fillMaxWidth(0.9f)
        } else {
            Modifier
                .align(Alignment.Center)
                .padding(EdgePadding)
                .fillMaxWidth(0.9f)
        }

        Card(
            modifier = cardModifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Row(modifier = Modifier.align(Alignment.End)) {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.skip))
                    }
                    Button(
                        onClick = {
                            onAction(step.action)
                            if (!step.requiresUserAction) {
                                onNext()
                            }
                        },
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Text(
                            stringResource(
                                if (step.requiresUserAction) R.string.try_it else R.string.next
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun TutorialOverlayPreview() {
    QuickNotesTheme {
        TutorialOverlay(
            step = TutorialManager.TutorialStep(
                title = "Welcome",
                description = "Let's tour the app.",
                targetViewId = -1,
                action = TutorialManager.TutorialStep.StepAction.NONE,
                requiresUserAction = false
            ),
            onAction = {},
            onNext = {},
            onSkip = {}
        )
    }
}
