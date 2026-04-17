package com.videonatics.smartcapture.core.presentation.view


import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.feature.home.presentation.view.HomePage
import com.videonatics.smartcapture.feature.settings.presentation.view.SettingsPage
import com.videonatics.smartcapture.feature.splash.presentation.view.SplashPage
import com.videonatics.smartcapture.ui.theme.ThemeMode

@Composable
fun RootPage(onThemeSwitch: (ThemeMode) -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController,
        startDestination = "main_graph"
    ) {
        navigation(
            startDestination = PageRoute.SPLASH.name,
            route = "main_graph"
        ){
            composable(PageRoute.SPLASH.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel =
                    hiltViewModel(parentEntry)
                onThemeSwitch.invoke(sharedViewModel.themeMode)
                SplashPage(navController)

            }
            composable(PageRoute.HOME.name) {

                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel =
                    hiltViewModel(parentEntry)
                HomePage(navController)
            }


            composable(PageRoute.SETTINGS.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel =
                    hiltViewModel(parentEntry)
                SettingsPage(navController)
            }
        }


    }

}

enum class PageRoute{
    SPLASH,HOME, CAPTURE,IMAGE_DETAILS, SETTINGS, DEPTH_CHECK, OCR_SELECT
}