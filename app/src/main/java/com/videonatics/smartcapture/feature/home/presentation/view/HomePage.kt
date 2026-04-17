package com.videonatics.smartcapture.feature.home.presentation.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun HomePage(navController: NavController, modifier: Modifier = Modifier) {
    return HomePageComponent()
}

@Composable
fun HomePageComponent(modifier: Modifier = Modifier) {
    return Scaffold() {paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)){

        }
    }

}