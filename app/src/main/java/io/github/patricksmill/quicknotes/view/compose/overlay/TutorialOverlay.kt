package io.github.patricksmill.quicknotes.view.compose.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.model.TutorialManager
import io.github.patricksmill.quicknotes.view.compose.theme.QuickNotesTheme

@Composable
fun TutorialOverlay(
    step: TutorialManager.TutorialStep,
    onAction: (TutorialManager.TutorialStep.StepAction) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp)
                .fillMaxWidth(),
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
                            if (step.requiresUserAction) {
                                onAction(step.action)
                            } else {
                                onAction(step.action)
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
