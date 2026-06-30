package io.github.patricksmill.quicknotes.view.compose.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.patricksmill.quicknotes.model.note.Note
import io.github.patricksmill.quicknotes.model.tag.AiModelCatalog
import io.github.patricksmill.quicknotes.model.tag.TagSettingsManager
import io.github.patricksmill.quicknotes.view.NotesUI
import io.github.patricksmill.quicknotes.view.compose.screens.SearchNotesScreen
import io.github.patricksmill.quicknotes.view.compose.screens.SettingsScreen
import io.github.patricksmill.quicknotes.view.compose.sheets.ManageNoteBottomSheet
import io.github.patricksmill.quicknotes.view.compose.sheets.ManageTagsBottomSheet

object Routes {
    const val SEARCH = "search"
    const val SETTINGS = "settings"
}

@Composable
fun QuickNotesNavHost(
    notes: List<Note>,
    listener: NotesUI.Listener,
    snackbarHostState: SnackbarHostState,
    tagSettingsManager: TagSettingsManager,
    modelCatalog: AiModelCatalog,
    appVersion: String,
    noteToEdit: Note?,
    showManageTags: Boolean,
    openSettingsRequest: Boolean,
    onOpenSettingsConsumed: () -> Unit,
    onNoteToEditConsumed: () -> Unit,
    onManageTagsDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onReplayTutorial: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    var manageTagsVisible by remember { mutableStateOf(showManageTags) }

    if (showManageTags) manageTagsVisible = true

    LaunchedEffect(openSettingsRequest) {
        if (openSettingsRequest) {
            navController.navigate(Routes.SETTINGS)
            onOpenSettingsConsumed()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = Routes.SEARCH,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        tween(300)
                    ) + fadeIn(tween(300))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        tween(300)
                    ) + fadeOut(tween(300))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        tween(300)
                    ) + fadeIn(tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        tween(300)
                    ) + fadeOut(tween(300))
                }
            ) {
                composable(Routes.SEARCH) {
                    SearchNotesScreen(
                        notes = notes,
                        tags = listener.onGetAllTags(),
                        listener = listener,
                        snackbarHostState = snackbarHostState,
                        onManageTags = { manageTagsVisible = true },
                        onOpenSettings = { listener.onOpenSettings() },
                        onNoteClick = { listener.onManageNotes(it) }
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        tagSettingsManager = tagSettingsManager,
                        modelCatalog = modelCatalog,
                        appVersion = appVersion,
                        onBack = { navController.popBackStack() },
                        onDeleteAllNotes = { listener.onDeleteAllNotes() },
                        onReplayTutorial = {
                            navController.popBackStack(Routes.SEARCH, inclusive = false)
                            onReplayTutorial()
                        },
                        onOpenNotificationSettings = onOpenNotificationSettings
                    )
                }
            }
        }
    }

    noteToEdit?.let { note ->
        val isNew = note.title.isEmpty() && note.content.isEmpty()
        ManageNoteBottomSheet(
            note = note,
            isNewNote = isNew,
            listener = listener,
            onDismiss = onNoteToEditConsumed,
            onSaved = onRefresh,
            onOpenSettings = {
                onNoteToEditConsumed()
                navController.navigate(Routes.SETTINGS)
            },
            onRefresh = onRefresh,
            showMessage = showMessage
        )
    }

    if (manageTagsVisible) {
        ManageTagsBottomSheet(
            tags = listener.onGetAllTags().sortedBy { it.name },
            listener = listener,
            onDismiss = {
                manageTagsVisible = false
                onManageTagsDismiss()
            },
            onTagsChanged = onRefresh
        )
    }
}
