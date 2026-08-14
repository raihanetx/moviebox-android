package com.moviebox.downloader.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.moviebox.downloader.ui.screen.ExtractScreen
import com.moviebox.downloader.ui.screen.PlayerScreen
import com.moviebox.downloader.ui.viewmodel.MainViewModel

object Routes {
    const val EXTRACT = "extract"
    const val PLAYER = "player"
}

@Composable
fun AppNav(initialUrl: String? = null) {
    val navController = rememberNavController()
    val vm: MainViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.EXTRACT,
    ) {
        composable(Routes.EXTRACT) {
            ExtractScreen(
                vm = vm,
                initialUrl = initialUrl,
                onPlayVideo = { navController.navigate(Routes.PLAYER) },
            )
        }
        composable(Routes.PLAYER) {
            PlayerScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
