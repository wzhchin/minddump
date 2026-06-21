package com.chin.minddump.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chin.minddump.audio.AudioRecorder
import com.chin.minddump.camera.CameraManager
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.ui.statistics.StatisticsScreen
import com.chin.minddump.ui.theme.LocalAnimationDuration
import java.io.File

sealed class Screen(
    val route: String
) {
    data object Main : Screen("main")

    data object Camera : Screen("camera")

    // entryPath query arg (Uri-encoded absolute path) selects edit mode;
    // absent = new-entry mode.
    data object FullscreenEdit : Screen("fullscreen_edit?entryPath={entryPath}") {
        const val ROUTE_NO_ARG = "fullscreen_edit"

        fun routeWithEntry(entryPath: String): String = "fullscreen_edit?entryPath=${android.net.Uri.encode(entryPath)}"
    }

    data object Statistics : Screen("statistics")

    // groupPath query arg (Uri-encoded absolute path) selects which group to open.
    data object GroupDetail : Screen("group_detail?groupPath={groupPath}") {
        fun routeWithGroup(groupPath: String): String =
            "group_detail?groupPath=${android.net.Uri.encode(groupPath)}"
    }
}

@Composable
fun MindDumpNavGraph(
    viewModel: MindDumpViewModel,
    cameraManager: CameraManager,
    audioRecorder: AudioRecorder,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val animDuration = LocalAnimationDuration.current

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(
                animationSpec = tween(animDuration.medium),
            ) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(animDuration.medium),
            )
        },
        exitTransition = {
            fadeOut(
                animationSpec = tween(animDuration.medium),
            ) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(animDuration.medium),
            )
        },
        popEnterTransition = {
            fadeIn(
                animationSpec = tween(animDuration.medium),
            ) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(animDuration.medium),
            )
        },
        popExitTransition = {
            fadeOut(
                animationSpec = tween(animDuration.medium),
            ) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(animDuration.medium),
            )
        },
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = viewModel,
                audioRecorder = audioRecorder,
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToFullscreenEdit = { entryPath ->
                    val route = if (entryPath != null) {
                        Screen.FullscreenEdit.routeWithEntry(entryPath)
                    } else {
                        Screen.FullscreenEdit.ROUTE_NO_ARG
                    }
                    navController.navigate(route)
                },
                onNavigateToStatistics = { navController.navigate(Screen.Statistics.route) },
                onNavigateToGroupDetail = { groupPath ->
                    navController.navigate(Screen.GroupDetail.routeWithGroup(groupPath))
                },
            )
        }

        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(
                navArgument("groupPath") {
                    type = NavType.StringType
                    nullable = false
                },
            ),
        ) { backStackEntry ->
            val groupPath = backStackEntry.arguments?.getString("groupPath")?.let {
                android.net.Uri.decode(it)
            } ?: return@composable
            // Same MainScreen composable as the root feed; only currentDir differs.
            // The route carries which group is open (source of truth for back stack
            // and process restore). The Camera route is a pushed sibling, so this
            // composable stays on the back stack during capture — currentGroupDir
            // is not cleared, and capture lands in this group.
            MainScreen(
                viewModel = viewModel,
                audioRecorder = audioRecorder,
                currentDir = File(groupPath),
                onBack = { navController.popBackStack() },
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToFullscreenEdit = { entryPath ->
                    val route = if (entryPath != null) {
                        Screen.FullscreenEdit.routeWithEntry(entryPath)
                    } else {
                        Screen.FullscreenEdit.ROUTE_NO_ARG
                    }
                    navController.navigate(route)
                },
                onNavigateToGroupDetail = { childPath ->
                    navController.navigate(Screen.GroupDetail.routeWithGroup(childPath))
                },
            )
        }

        composable(Screen.Camera.route) {
            val photoFile = viewModel.getPhotoFile()
            val videoFile = viewModel.getVideoFile()
            photoFile.parentFile?.mkdirs()
            videoFile.parentFile?.mkdirs()
            cameraManager.setOutputFiles(
                photo = photoFile,
                video = videoFile,
            )
            CameraScreen(
                cameraManager = cameraManager,
                onClose = { navController.popBackStack() },
                onCaptured = {
                    viewModel.onMediaCaptured()
                    navController.popBackStack()
                },
                modifier = Modifier,
            )
        }

        composable(
            route = Screen.FullscreenEdit.route,
            arguments = listOf(
                navArgument("entryPath") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val entryPath = backStackEntry.arguments?.getString("entryPath")
            // Resolve the entry from the current set; fall back to a bare File so
            // editing still works for an entry that scrolled out of the loaded page.
            val editEntry: MindDumpEntry? = entryPath?.let { path ->
                uiState.entries.firstOrNull { it.file.absolutePath == path }
                    ?: File(path).takeIf { it.exists() }?.let { file ->
                        // Minimal entry: enough for load/save (path is the identity).
                        MindDumpEntry(
                            file = file,
                            type = com.chin.minddump.storage.EntryType.TEXT,
                            space = uiState.currentSpace,
                            monthFolder = file.parentFile?.name ?: "",
                            role = com.chin.minddump.storage.EntryRole.FILE,
                        )
                    }
            }
            FullscreenEditScreen(
                viewModel = viewModel,
                editEntry = editEntry,
                newEntryText = uiState.inputText,
                onNewEntryTextChange = { viewModel.updateInputText(it) },
                onClose = { navController.popBackStack() },
                onSubmitNewEntry = {
                    viewModel.submitText()
                    navController.popBackStack()
                },
                onSavedEdit = { navController.popBackStack() },
            )
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
