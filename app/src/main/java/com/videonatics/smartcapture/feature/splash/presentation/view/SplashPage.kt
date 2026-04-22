package com.videonatics.smartcapture.feature.splash.presentation.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.videonatics.smartcapture.R
import com.videonatics.smartcapture.core.presentation.view.RootPage
import com.videonatics.smartcapture.feature.splash.presentation.viewmodel.SplashViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun SplashPage(navController: NavController, modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<SplashViewModel>()
    val splashLoad = viewModel.loadSplash.collectAsState(false).value
    if(splashLoad){
            navController.navigate("auth")
        }

    return SplashPageComponent()
}

@Composable
fun SplashPageComponent(modifier: Modifier = Modifier) {
    return Scaffold() {paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize(),
            contentAlignment = Alignment.Center){
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                contentScale = ContentScale.Inside
            )
        }
    }

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartCaptureTheme {
        SplashPageComponent()
    }
}