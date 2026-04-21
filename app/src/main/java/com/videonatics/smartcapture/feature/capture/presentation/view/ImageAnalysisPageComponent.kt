package com.videonatics.smartcapture.feature.capture.presentation.view

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun ImageAnalysisPageComponent(modifier: Modifier = Modifier, capturedImage: Bitmap?=null) {
    Box(modifier = modifier.fillMaxSize()){
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,

        ) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0xFF262626))
                .weight(1f)){

                if(capturedImage!=null) {
                    Image(
                        bitmap = capturedImage.asImageBitmap(),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

            }
            Box(modifier = Modifier.fillMaxSize().weight(1f)){
                Text("Image Analysis Page")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ImageAnalysisPageComponentPreview() {
    SmartCaptureTheme {
        ImageAnalysisPageComponent()
    }
}
