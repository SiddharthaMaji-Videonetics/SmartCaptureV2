package com.videonatics.smartcapture.feature.auth.presentation.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun AuthPage(modifier: Modifier = Modifier) {
    return AuthPageComponent()
}

@Composable
fun AuthPageComponent(modifier: Modifier = Modifier) {
    return Scaffold() {paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize(),
            contentAlignment = Alignment.Center){
            Text(text = "Auth Page")
        }
        }
    }

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartCaptureTheme {
        AuthPageComponent()
    }
}