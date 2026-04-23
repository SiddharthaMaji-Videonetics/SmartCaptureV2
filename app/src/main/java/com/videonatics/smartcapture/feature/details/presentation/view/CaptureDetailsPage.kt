package com.videonatics.smartcapture.feature.details.presentation.view

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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

    var classificationResult by remember { mutableStateOf<FatBoxClassifier.Result?>(null) }
    var croppedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    ImageAnalysisPageComponent(
        capturedImage = bmp,
        capturedImageBoundingBox = sharedViewModel.getCapturedImageBoundBox(),
        classificationResult = classificationResult,
        croppedImage = croppedBitmap,
        imageQuality = sharedViewModel.getImageQuality(),
        rotationAngle = sharedViewModel.getRotationAngle(),
        latitude = sharedViewModel.getLatitude(),
        longitude = sharedViewModel.getLongitude(),
        onResizedImageReady = { resized ->
            val result = classifier.classify(resized)
            classificationResult = result
            croppedBitmap = result.croppedBitmap
        },
        onClose = {
            navController.popBackStack()
        }
    )
}


@Composable
fun ImageAnalysisPageComponent(modifier: Modifier = Modifier,
                               capturedImage: Bitmap? = null,
                               capturedImageBoundingBox: RectF? = null,
                               classificationResult: FatBoxClassifier.Result? = null,
                               croppedImage: Bitmap? = null,
                               imageQuality: Float = 0f,
                               rotationAngle: Float = 0f,
                               latitude: Double = 0.0,
                               longitude: Double = 0.0,
                               onResizedImageReady: (Bitmap) -> Unit = {},
                               onClose: () -> Unit = {}) {
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
                            imageWidth = capturedImage.width.toFloat(),
                            imageHeight = capturedImage.height.toFloat(),
                            modifier = Modifier.fillMaxSize(),
                            onTransformedImage = onResizedImageReady
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
                                headlineContent = { Text("Content Type") },
                                supportingContent = { Text(classificationResult?.label ?: "Unknown") },
                                trailingContent = {
                                    classificationResult?.let {
                                        Text("${(it.confidence * 100).toInt()}%")
                                    }
                                }
                            )
                        }
                        item {
                            HorizontalDivider()
                            ListItem(
                                headlineContent = { Text("Metadata") },
                                supportingContent = {
                                    Column {
                                        Text("Quality: ${"%.2f".format(imageQuality)}")
                                        Text("Rotation: ${"%.2f".format(rotationAngle)}°")
                                        Text("Location: ${"%.4f".format(latitude)}, ${"%.4f".format(longitude)}")
                                    }
                                }
                            )
                        }
                        if (croppedImage != null) {
                            item {
                                HorizontalDivider()
                                Text(
                                    "Processed Input (1024x1024)",
                                    modifier = Modifier.padding(16.dp),
                                    color = Color.Gray
                                )
                                Image(
                                    bitmap = croppedImage.asImageBitmap(),
                                    contentDescription = "Cropped Input",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(horizontal = 16.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
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
