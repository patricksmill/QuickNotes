package io.github.patricksmill.quicknotes.view.compose.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag

fun Modifier.tutorialTarget(viewId: Int, testTag: String): Modifier = this
    .testTag(testTag)
    .onGloballyPositioned { coordinates ->
        TutorialTargets.bounds[viewId] = coordinates.boundsInWindow()
    }
