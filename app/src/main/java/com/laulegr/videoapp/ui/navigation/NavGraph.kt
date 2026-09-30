package com.laulegr.videoapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.laulegr.videoapp.ui.editor.EditorScreen
import com.laulegr.videoapp.ui.editor.EditorViewModel
import com.laulegr.videoapp.ui.home.HomeScreen

private object Routes {
    const val HOME = "home"
    const val EDITOR = "editor"
}

@Composable
fun VideoAppNavGraph() {
    val navController = rememberNavController()
    // Single ViewModel shared by both screens: avoids passing picked video
    // Uris through nav arguments and keeps the timeline alive while editing.
    val editorViewModel: EditorViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = editorViewModel,
                onClipsPicked = { navController.navigate(Routes.EDITOR) },
            )
        }
        composable(Routes.EDITOR) {
            EditorScreen(
                viewModel = editorViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
