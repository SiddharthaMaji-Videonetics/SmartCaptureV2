package com.videonatics.smartcapture.feature.splash.presentation.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun SplashPage(navController: NavController, modifier: Modifier = Modifier) {
    return SplashPageComponent()
}

@Composable
fun SplashPageComponent(modifier: Modifier = Modifier) {
    return Scaffold() {paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)){

        }
    }

}