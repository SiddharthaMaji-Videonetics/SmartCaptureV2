package com.videonatics.smartcapture.feature.details.presentation.view

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.videonatics.smartcapture.core.loadFatBoxLabels
import com.videonatics.smartcapture.core.loadFatBoxModel
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.feature.details.FatBoxClassifier
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun CaptureDetailsPage(modifier: Modifier = Modifier,
                       navController: NavController,
                       sharedViewModel: SharedCoreViewModel) {

    val context = LocalContext.current
    val bmp = sharedViewModel.getCapturedImage()
    val classifier = remember {
        FatBoxClassifier(
            interpreter = loadFatBoxModel(context),
            labels = loadFatBoxLabels(context)
        )
    }





    ImageAnalysisPageComponent(capturedImage = bmp,
        capturedImageBoundingBox = sharedViewModel.getCapturedImageBoundBox(),
        classifier = classifier){
        navController.popBackStack()
    }
}


@Composable
fun ImageAnalysisPageComponent(modifier: Modifier = Modifier,
                               capturedImage: Bitmap?=null,
                               capturedImageBoundingBox: RectF?=null,
                               classifier: FatBoxClassifier?=null,
                               onClose:()-> Unit={}) {
    var contentType by remember { mutableStateOf("Unknown") }
    Scaffold() {
        Box(modifier = modifier.padding(it).fillMaxSize()){
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,

                ) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color(0xFF262626))
                    .weight(1f)){

                    if(capturedImage!=null && capturedImageBoundingBox!=null) {
                        ZoomableImageWithBoundingBox(
                            bitmap = capturedImage,
                            rect = capturedImageBoundingBox,
                            imageWidth = 480f,
                            imageHeight = 640f,
                            modifier = Modifier.fillMaxSize(),
                            onTransformedImage = {
                                val result = classifier?.classify(it)
                                result?.apply {
                                    Log.e("class",result.label)
                                    contentType = if(result.label == "FatBoxNot") "Not FatBox" else "FatBox"
                                }

                            }
                        )

                    }

                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF606060),
                        modifier = Modifier.padding(5.dp)
                            .align(Alignment.TopEnd).clickable {
                                onClose.invoke()
                            }
                    )

                }
                Box(modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)){
                    LazyColumn() {
                        item {
                            ListItem(
                                headlineContent = {
                                    Text("Content Type")
                                },
                                supportingContent = {
                                    Text(contentType)
                                }
                            )
                        }
                    }
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
