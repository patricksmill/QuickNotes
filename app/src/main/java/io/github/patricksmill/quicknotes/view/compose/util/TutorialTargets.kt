package io.github.patricksmill.quicknotes.view.compose.util

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect
import io.github.patricksmill.quicknotes.R

object TutorialTargets {
    val bounds = mutableStateMapOf<Int, Rect>()

    fun tagForViewId(viewId: Int): String? = when (viewId) {
        R.id.addNoteFab -> "addNoteFab"
        R.id.search_bar -> "search_bar"
        R.id.tagRecyclerView -> "tagRecyclerView"
        R.id.settingsButton -> "settingsButton"
        else -> null
    }
}
