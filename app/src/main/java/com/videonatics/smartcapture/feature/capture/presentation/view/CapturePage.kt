package com.videonatics.smartcapture.feature.capture.presentation.view

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.videonatics.smartcapture.core.toBitmap
import com.videonatics.smartcapture.feature.splash.presentation.view.SplashPageComponent
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun CapturePage(navController: NavController, modifier: Modifier = Modifier) {
    return CapturePageComponent()
}

@Composable
fun CapturePageComponent(modifier: Modifier = Modifier) {
    return Scaffold() {paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize(),
            contentAlignment = Alignment.Center){
            CameraPreviewWithImageProxy(
                frameIntervalMs = 500,
                onFrame = {
                    try{
                val bmp = it.toBitmap()
                Log.e("frame","image frame injected")
                    }finally {
                        it.close()
                    }

            })
        }
    }

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartCaptureTheme {
        CapturePageComponent()
    }
}