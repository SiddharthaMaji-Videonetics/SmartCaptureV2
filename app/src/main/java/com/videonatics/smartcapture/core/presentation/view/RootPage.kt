package com.videonatics.smartcapture.core.presentation.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.core.util.LocaleHelper
import com.videonatics.smartcapture.feature.auth.presentation.view.AuthPage
import com.videonatics.smartcapture.feature.capture.presentation.view.CapturePage
import com.videonatics.smartcapture.feature.details.presentation.view.CaptureDetailsPage
import com.videonatics.smartcapture.feature.home.presentation.view.HomePage
import com.videonatics.smartcapture.feature.language.presentation.view.LanguagePage
import com.videonatics.smartcapture.feature.settings.presentation.view.SettingsPage
import com.videonatics.smartcapture.feature.splash.presentation.view.SplashPage
import com.videonatics.smartcapture.feature.task.presentation.view.TaskPage
import com.videonatics.smartcapture.ui.theme.ThemeMode

@Composable
fun RootPage(onThemeSwitch: (ThemeMode) -> Unit) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main_graph"
    ) {
        navigation(
            startDestination = PageRoute.SPLASH.name,
            route = "main_graph"
        ) {
            composable(PageRoute.SPLASH.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)
                
                LocalizedPage(sharedViewModel) {
                    onThemeSwitch.invoke(sharedViewModel.themeMode)
                    SplashPage(navController)
                }
            }
            
            composable(PageRoute.AUTH.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)
                LocalizedPage(sharedViewModel) {
                    AuthPage(navController)
                }
            }

            composable(PageRoute.TASK.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)
                LocalizedPage(sharedViewModel) {
                    TaskPage(navController)
                }
            }

            composable(PageRoute.HOME.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)
                LocalizedPage(sharedViewModel) {
                    HomePage(navController)
                }
            }

            composable(PageRoute.CAPTURE.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)
                LocalizedPage(sharedViewModel) {
                    CapturePage(navController, sharedViewModel)
                }
            }

            composable(PageRoute.IMAGE_DETAILS.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)
                LocalizedPage(sharedViewModel) {
                    CaptureDetailsPage(
                        navController = navController,
                        sharedViewModel = sharedViewModel
                    )
                }
            }

            composable(PageRoute.SETTINGS.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)
                LocalizedPage(sharedViewModel) {
                    SettingsPage(navController)
                }
            }

            composable(PageRoute.LANGUAGE.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)
                LocalizedPage(sharedViewModel) {
                    LanguagePage(navController, sharedViewModel)
                }
            }
        }
    }
}

@Composable
private fun LocalizedPage(
    sharedViewModel: SharedCoreViewModel,
    content: @Composable () -> Unit
) {
    val selectedLanguage by sharedViewModel.selectedLanguage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val localizedContext = remember(selectedLanguage) {
        LocaleHelper.updateLocale(context, selectedLanguage.code)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration
    ) {
        key(selectedLanguage.code) {
            content()
        }
    }
}

enum class PageRoute {
    AUTH, SPLASH, HOME, TASK, CAPTURE, IMAGE_DETAILS, SETTINGS, DEPTH_CHECK, OCR_SELECT, LANGUAGE
}
