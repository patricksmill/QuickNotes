package io.github.patricksmill.quicknotes.view.compose.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import io.github.patricksmill.quicknotes.model.tag.Tag
import io.github.patricksmill.quicknotes.model.tag.TagSettingsManager
import io.github.patricksmill.quicknotes.view.NotesUI
import io.github.patricksmill.quicknotes.view.compose.screens.SearchNotesScreen
import io.github.patricksmill.quicknotes.view.compose.screens.SettingsScreen
import io.github.patricksmill.quicknotes.view.compose.sheets.ManageNoteBottomSheet
import io.github.patricksmill.quicknotes.view.compose.sheets.ManageTagsBottomSheet

@Composable
fun QuickNotesNavHost(
    notes: List<Note>,
    tags: List<Tag>,
    revision: Int,
    searchQuery: String,
    listener: NotesUI.Listener,
    snackbarHostState: SnackbarHostState,
    tagSettingsManager: TagSettingsManager,
    modelCatalog: AiModelCatalog,
    appVersion: String,
    noteToEdit: Note?,
    openSettingsRequest: Boolean,
    onOpenSettingsConsumed: () -> Unit,
    onNoteToEditConsumed: () -> Unit,
    onRefresh: () -> Unit,
    onReplayTutorial: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    var manageTagsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(openSettingsRequest) {
        if (openSettingsRequest) {
            navController.navigate(QuickNotesRoutes.SETTINGS)
            onOpenSettingsConsumed()
        }
    }

    if (manageTagsVisible && noteToEdit == null) {
        BackHandler { manageTagsVisible = false }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = QuickNotesRoutes.SEARCH,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            enterTransition = { fadeIn() + slideInHorizontally { it / 4 } },
            exitTransition = { fadeOut() + slideOutHorizontally { -it / 4 } },
            popEnterTransition = { fadeIn() + slideInHorizontally { -it / 4 } },
            popExitTransition = { fadeOut() + slideOutHorizontally { it / 4 } }
        ) {
            composable(QuickNotesRoutes.SEARCH) {
                SearchNotesScreen(
                    notes = notes,
                    tags = tags,
                    revision = revision,
                    searchQuery = searchQuery,
                    listener = listener,
                    snackbarHostState = snackbarHostState,
                    onManageTags = { manageTagsVisible = true },
                    onOpenSettings = { navController.navigate(QuickNotesRoutes.SETTINGS) },
                    onNoteClick = { listener.onManageNotes(it) },
                    onRefresh = onRefresh
                )
            }
            composable(QuickNotesRoutes.SETTINGS) {
                SettingsScreen(
                    tagSettingsManager = tagSettingsManager,
                    modelCatalog = modelCatalog,
                    appVersion = appVersion,
                    onBack = { navController.popBackStack() },
                    onDeleteAllNotes = { listener.onDeleteAllNotes() },
                    onReplayTutorial = {
                        navController.popBackStack()
                        onReplayTutorial()
                    },
                    onOpenNotificationSettings = onOpenNotificationSettings
                )
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
                navController.navigate(QuickNotesRoutes.SETTINGS)
            },
            onRefresh = onRefresh,
            showMessage = showMessage
        )
    }

    if (manageTagsVisible) {
        ManageTagsBottomSheet(
            tags = tags,
            listener = listener,
            onDismiss = { manageTagsVisible = false }
        )
    }
}
