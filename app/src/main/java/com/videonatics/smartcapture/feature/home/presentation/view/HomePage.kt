package com.videonatics.smartcapture.feature.home.presentation.view

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.videonatics.smartcapture.core.presentation.view.PageRoute
import com.videonatics.smartcapture.core.presentation.view.UserHeader
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.feature.home.presentation.HomeTab
import com.videonatics.smartcapture.feature.home.presentation.viewmodel.HomeViewModel
import com.videonatics.smartcapture.feature.settings.presentation.view.SettingsPage
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun HomePage(navController: NavController, modifier: Modifier = Modifier) {
    return HomePageComponent(
        rootNavController = navController,
        onNavigateToLanguage = {
            navController.navigate(PageRoute.LANGUAGE.name)
        },
        onSettings = {
            navController.navigate("settings")
        },
        onCapture = {
            navController.navigate("capture")
        },
        onTaskOpen = { taskId ->
            navController.navigate("${PageRoute.TASK.name}/$taskId")
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageComponent(
    onNavigateToLanguage: () -> Unit = {},
    onSettings: () -> Unit = {},
    onCapture: () -> Unit = {},
    onTaskOpen: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    rootNavController: NavController = rememberNavController()
) {
    val navController = rememberNavController()
    val userName by viewModel.userName.collectAsState()
    val userType by viewModel.userType.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val items = listOf(
        HomeTab.Dashboard,
        HomeTab.MyTask,
        HomeTab.Settings
    )

    val parentEntry = remember(rootNavController.currentBackStackEntry) {
        rootNavController.getBackStackEntry("main_graph")
    }
    val sharedViewModel: SharedCoreViewModel = hiltViewModel(parentEntry)

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData(sharedViewModel = sharedViewModel)
        viewModel.loadUserMe(sharedViewModel = sharedViewModel)
    }

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                items.forEach { tab ->
                    NavigationRailItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(tab.icon, contentDescription = stringResource(tab.labelRes))
                        },
                        label = {
                            Text(stringResource(tab.labelRes))
                        }
                    )
                }
            }

            Scaffold(
                topBar = {
                    if (currentRoute != HomeTab.Settings.route) {
                        TopAppBar(
                            title = {
                                UserHeader(userName = userName, userType = userType)
                            }
                        )
                    }
                }
            ) { innerPadding ->
                HomeNavHost(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding),
                    onTaskOpen = onTaskOpen,
                    onNavigateToLanguage = onNavigateToLanguage,
                    rootNavController = rootNavController,
                    sharedViewModel = sharedViewModel
                )
            }
        }
    } else {
        Scaffold(
            topBar = {
                if (currentRoute != HomeTab.Settings.route) {
                    TopAppBar(
                        title = {
                            UserHeader(userName = userName, userType = userType)
                        }
                    )
                }
            },
            bottomBar = {
                NavigationBar {
                    items.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(tab.icon, contentDescription = stringResource(tab.labelRes))
                            },
                            label = {
                                Text(stringResource(tab.labelRes))
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            HomeNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                onTaskOpen = onTaskOpen,
                onNavigateToLanguage = onNavigateToLanguage,
                rootNavController = rootNavController,
                sharedViewModel = sharedViewModel
            )
        }
    }
}

@Composable
fun HomeNavHost(
    navController: androidx.navigation.NavHostController,
    modifier: Modifier = Modifier,
    onTaskOpen: (String) -> Unit,
    onNavigateToLanguage: () -> Unit,
    rootNavController: NavController,
    sharedViewModel: SharedCoreViewModel
) {
    NavHost(
        navController = navController,
        startDestination = HomeTab.Dashboard.route,
        modifier = modifier
    ) {
        composable(HomeTab.Dashboard.route) { DashboardScreen(sharedViewModel = sharedViewModel) }
        composable(HomeTab.MyTask.route) { MyTaskScreen(onTaskOpen = onTaskOpen) }
        composable(HomeTab.Settings.route) { 
            SettingsPage(navController = rootNavController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageComponentS(modifier: Modifier = Modifier, onSettings: ()-> Unit = {}, onCapture: () -> Unit = {}) {
    return Scaffold(
        topBar = {
            TopAppBar(
                title = {

                },
                actions = {
                    IconButton(onClick = {
                        onSettings.invoke()
                    }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) {paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize(),
            contentAlignment = Alignment.Center){
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = stringResource(id = com.videonatics.smartcapture.R.string.no_data_available))
                ElevatedButton(
                    modifier = Modifier.padding(top = 16.dp),
                    onClick = {
                        onCapture.invoke()
                    }) {
                    Text("Capture")
                }
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartCaptureTheme {
        HomePageComponent()
    }
}
