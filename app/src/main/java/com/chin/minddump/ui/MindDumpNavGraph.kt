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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chin.minddump.audio.AudioRecorder
import com.chin.minddump.camera.CameraManager
import com.chin.minddump.ui.theme.LocalAnimationDuration

sealed class Screen(val route: String) {
    data object Main : Screen("main")

    data object Camera : Screen("camera")

    data object FullscreenEdit : Screen("fullscreen_edit")
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
                onNavigateToFullscreenEdit = { navController.navigate(Screen.FullscreenEdit.route) },
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

        composable(Screen.FullscreenEdit.route) {
            FullscreenEditScreen(
                text = uiState.inputText,
                onTextChange = { viewModel.updateInputText(it) },
                onClose = { navController.popBackStack() },
                onSubmit = {
                    viewModel.submitText()
                    navController.popBackStack()
                },
            )
        }
    }
}
