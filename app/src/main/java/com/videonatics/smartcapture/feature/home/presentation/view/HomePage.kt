package com.videonatics.smartcapture.feature.home.presentation.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.videonatics.smartcapture.feature.splash.presentation.view.SplashPageComponent
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun HomePage(navController: NavController, modifier: Modifier = Modifier) {
    return HomePageComponent(onCapture = {
        navController.navigate("capture")
    })
}

@Composable
fun HomePageComponent(modifier: Modifier = Modifier, onCapture: () -> Unit = {}) {
    return Scaffold() {paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize(),
            contentAlignment = Alignment.Center){
            Column() {
                ElevatedButton(
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