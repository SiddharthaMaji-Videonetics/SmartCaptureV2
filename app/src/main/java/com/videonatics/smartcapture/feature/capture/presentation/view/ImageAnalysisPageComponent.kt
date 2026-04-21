package com.videonatics.smartcapture.feature.capture.presentation.view

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun ImageAnalysisPageComponent(modifier: Modifier = Modifier,
                               capturedImage: Bitmap?=null,
                               onClose:()-> Unit={}) {
    Scaffold() {
        Box(modifier = modifier.padding(it).fillMaxSize()){
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

                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = androidx.compose.ui.graphics.Color(0xFF606060),
                        modifier = Modifier.padding(5.dp)
                            .align(Alignment.TopEnd).clickable {
                                onClose.invoke()
                            }
                    )

                }
                Box(modifier = Modifier.fillMaxSize().weight(1f)){
                    Text("Image Analysis Page")
                }
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
