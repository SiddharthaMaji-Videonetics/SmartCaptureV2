package com.videonatics.smartcapture.core.presentation.view

import android.net.Uri
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.core.toNormalized
import com.videonatics.smartcapture.core.toPixel
import com.videonatics.smartcapture.core.uriToBitmap
import com.videonatics.smartcapture.core.util.LocaleHelper
import com.videonatics.smartcapture.feature.analysis.presenation.view.AnalysisPage
import com.videonatics.smartcapture.feature.analysis.presenation.view.AttemptDetailPage
import com.videonatics.smartcapture.feature.auth.presentation.view.AuthPage
import com.videonatics.smartcapture.feature.capture.presentation.view.CapturePage
import com.videonatics.smartcapture.feature.capturelist.presentation.view.ImageEditorPage
import com.videonatics.smartcapture.feature.moresettings.presentation.view.CaptureSettingPage
import com.videonatics.smartcapture.feature.details.presentation.view.CaptureDetailsPage
import com.videonatics.smartcapture.feature.home.presentation.view.HomePage
import com.videonatics.smartcapture.feature.language.presentation.view.LanguagePage
import com.videonatics.smartcapture.feature.settings.presentation.view.SettingsPage
import com.videonatics.smartcapture.feature.splash.presentation.view.SplashPage
import com.videonatics.smartcapture.feature.task.presentation.view.TaskPage
import com.videonatics.smartcapture.feature.capturelist.presentation.view.MyCapturesPage
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
                
                HandleUnauthorized(sharedViewModel, navController)

                val themeMode by sharedViewModel.themeMode.collectAsStateWithLifecycle()

                LocalizedPage(sharedViewModel) {
                    LaunchedEffect(themeMode) {
                        onThemeSwitch.invoke(themeMode)
                    }
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

            composable(
                route = "${PageRoute.TASK.name}/{taskId}",
                arguments = listOf(navArgument("taskId") { type = NavType.StringType })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("main_graph")
                }
                Log.e("task_id",taskId)
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)
                
                HandleUnauthorized(sharedViewModel, navController)

                LocalizedPage(sharedViewModel) {
                    TaskPage(navController, taskId = taskId, sharedViewModel = sharedViewModel)
                }
            }


            composable(PageRoute.HOME.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)
                
                HandleUnauthorized(sharedViewModel, navController)

                LocalizedPage(sharedViewModel) {
                    HomePage(navController)
                }
            }

            composable(PageRoute.CAPTURE.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)
                
                HandleUnauthorized(sharedViewModel, navController)

                LocalizedPage(sharedViewModel) {
                    CapturePage(navController, sharedViewModel)
                }
            }

            composable(PageRoute.IMAGE_DETAILS.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)
                
                HandleUnauthorized(sharedViewModel, navController)

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
                
                HandleUnauthorized(sharedViewModel, navController)

                LocalizedPage(sharedViewModel) {
                    SettingsPage(navController)
                }
            }

            composable(PageRoute.CAPTURE_SETTINGS.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)

                HandleUnauthorized(sharedViewModel, navController)

                LocalizedPage(sharedViewModel) {
                    CaptureSettingPage(navController)
                }
            }

            composable(PageRoute.LANGUAGE.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)
                
                HandleUnauthorized(sharedViewModel, navController)

                LocalizedPage(sharedViewModel) {
                    LanguagePage(navController, sharedViewModel)
                }
            }

            composable(PageRoute.MY_CAPTURES.name) {
                val parentEntry = remember(it) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)

                HandleUnauthorized(sharedViewModel, navController)

                LocalizedPage(sharedViewModel) {
                    MyCapturesPage(navController = navController, sharedViewModel = sharedViewModel)
                }
            }

            composable(
                route = "${PageRoute.ANALYSIS.name}/{taskId}/{stageId}",
                arguments = listOf(
                    navArgument("taskId") { type = NavType.StringType },
                    navArgument("stageId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                val stageId = backStackEntry.arguments?.getString("stageId") ?: ""
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)

                HandleUnauthorized(sharedViewModel, navController)

                LocalizedPage(sharedViewModel) {
                    AnalysisPage(
                        onBackClick = { navController.popBackStack() },
                        onRetake = {
                            sharedViewModel.saveCurrentTaskId(taskId)
                            sharedViewModel.saveCurrentStageId(stageId)
                            navController.navigate(PageRoute.CAPTURE.name)
                        },
                        onRetakeFromGallery = { uri ->
                            sharedViewModel.saveCurrentTaskId(taskId)
                            sharedViewModel.saveCurrentStageId(stageId)
                            navController.navigate("${PageRoute.IMAGE_EDITOR.name}/${Uri.encode(uri.toString())}?uploadAfterCrop=true")
                        },
                        onAttemptClick = { tId, sId, aId ->
                            navController.navigate("${PageRoute.ATTEMPT_DETAIL.name}/$tId/$sId/$aId")
                        }
                    )
                }
            }

            composable(
                route = "${PageRoute.ATTEMPT_DETAIL.name}/{taskId}/{stageId}/{attemptId}",
                arguments = listOf(
                    navArgument("taskId") { type = NavType.StringType },
                    navArgument("stageId") { type = NavType.StringType },
                    navArgument("attemptId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)

                HandleUnauthorized(sharedViewModel, navController)

                LocalizedPage(sharedViewModel) {
                    AttemptDetailPage(onBackClick = { navController.popBackStack() })
                }
            }

            composable(
                route = "${PageRoute.IMAGE_EDITOR.name}/{imgUri}?uploadAfterCrop={uploadAfterCrop}",
                arguments = listOf(
                    navArgument("imgUri") { type = NavType.StringType },
                    navArgument("uploadAfterCrop") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->

                val encodedUri = backStackEntry.arguments?.getString("imgUri")
                val imageUri = Uri.decode(encodedUri ?: "")
                val uploadAfterCrop = backStackEntry.arguments?.getBoolean("uploadAfterCrop") ?: false
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("main_graph")
                }
                val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)

                HandleUnauthorized(sharedViewModel, navController)

                LocalizedPage(sharedViewModel) {
                    ImageEditorPage(onBack = { navController.popBackStack() },
                        imgUri = imageUri.toUri(),
                        onCrop = { fullImg, croppedImg, boundBox, rotation ->
                            sharedViewModel.saveCapturedImage(croppedImg, boundBox, rotation = rotation)
                            if (uploadAfterCrop) {
                                navController.navigate(PageRoute.IMAGE_DETAILS.name)
                            } else {
                                navController.popBackStack()
                            }
                        })
                }
            }

        }
    }
}



@Composable
fun HandleUnauthorized(sharedViewModel: SharedCoreViewModel, navController: androidx.navigation.NavController) {
    val showSessionExpiredDialog by sharedViewModel.showSessionExpiredDialog.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        sharedViewModel.unauthorizedEvent.collect {
            navController.navigate(PageRoute.AUTH.name) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
            }
        }
    }

    if (showSessionExpiredDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Session Expired") },
            text = { Text("Your session has expired. Please login again.") },
            confirmButton = {
                TextButton(onClick = { sharedViewModel.dismissSessionExpiredDialog() }) {
                    Text("OK")
                }
            }
        )
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
    AUTH, SPLASH, HOME, TASK, CAPTURE, IMAGE_DETAILS, SETTINGS, CAPTURE_SETTINGS, IMAGE_EDITOR, LANGUAGE, MY_CAPTURES, ANALYSIS, ATTEMPT_DETAIL
}
