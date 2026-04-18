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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.videonatics.smartcapture.feature.splash.presentation.view.SplashPageComponent
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun HomePage(navController: NavController, modifier: Modifier = Modifier) {
    return HomePageComponent(onSettings = {
        navController.navigate("settings")
    },onCapture = {
        navController.navigate("capture")
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageComponent(modifier: Modifier = Modifier, onSettings: ()-> Unit = {}, onCapture: () -> Unit = {}) {
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