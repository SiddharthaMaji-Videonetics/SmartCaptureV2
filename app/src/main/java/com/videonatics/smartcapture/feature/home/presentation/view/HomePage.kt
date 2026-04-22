package com.videonatics.smartcapture.feature.home.presentation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.videonatics.smartcapture.core.presentation.view.UserHeader
import com.videonatics.smartcapture.feature.home.presentation.HomeTab
import com.videonatics.smartcapture.feature.home.presentation.viewmodel.HomeViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun HomePage(navController: NavController, modifier: Modifier = Modifier) {
    return HomePageComponent(onSettings = {
        navController.navigate("settings")
    },onCapture = {
        navController.navigate("capture")
    },
        onTaskOpen = {
            navController.navigate("task")
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageComponent(
    onSettings: () -> Unit = {},
    onCapture: () -> Unit = {},
    onTaskOpen: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val userName by viewModel.userName.collectAsState()
    val userType by viewModel.userType.collectAsState()

    val items = listOf(
        HomeTab.Dashboard,
        HomeTab.MyTask,
        HomeTab.Profile
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    UserHeader(userName = userName, userType = userType)
                },

            )

        },
        bottomBar = {
            NavigationBar {
                val currentRoute =
                    navController.currentBackStackEntryAsState().value?.destination?.route

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
                            Icon(tab.icon, contentDescription = tab.label)
                        },
                        label = {
                            Text(tab.label)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = HomeTab.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(HomeTab.Dashboard.route) { DashboardScreen() }
            composable(HomeTab.MyTask.route) { MyTaskScreen(onTaskOpen = onTaskOpen) }
            composable(HomeTab.Profile.route) { ProfileScreen() }
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
                Text("No Capture data available")
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
