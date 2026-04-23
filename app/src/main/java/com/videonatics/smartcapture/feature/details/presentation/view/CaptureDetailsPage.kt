package com.videonatics.smartcapture.feature.details.presentation.view

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.videonatics.smartcapture.core.loadFatBoxLabels
import com.videonatics.smartcapture.core.loadFatBoxModel
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.feature.details.FatBoxClassifier
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun CaptureDetailsPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    sharedViewModel: SharedCoreViewModel
) {
    val context = LocalContext.current
    val bmp = sharedViewModel.getCapturedImage()
    val classifier = remember {
        FatBoxClassifier(
            interpreter = loadFatBoxModel(context),
            labels = loadFatBoxLabels(context)
        )
    }

    ImageAnalysisPageComponent(
        capturedImage = bmp,
        capturedImageBoundingBox = sharedViewModel.getCapturedImageBoundBox(),
        classifier = classifier
    ) {
        navController.popBackStack()
    }
}

@Composable
fun ImageAnalysisPageComponent(
    modifier: Modifier = Modifier,
    capturedImage: Bitmap? = null,
    capturedImageBoundingBox: RectF? = null,
    classifier: FatBoxClassifier? = null,
    onClose: () -> Unit = {}
) {
    var contentType by remember { mutableStateOf("Analyzing...") }
    var quality by remember { mutableStateOf("Analyzing...") }
    var isContentTypeValid by remember { mutableStateOf(true) }
    var isQualityValid by remember { mutableStateOf(true) }
    
    var contentTypeOverrideReason by remember { mutableStateOf("") }
    var qualityOverrideReason by remember { mutableStateOf("") }

    val isUploadEnabled = (isContentTypeValid || contentTypeOverrideReason.isNotBlank()) &&
            (isQualityValid || qualityOverrideReason.isNotBlank()) &&
            contentType != "Analyzing..."

    Scaffold(
        bottomBar = {
            BottomAppBar() {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { /* Handle Upload */ },
                        enabled = isUploadEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Upload")
                    }
                }
            }

        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Image Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                if (capturedImage != null && capturedImageBoundingBox != null) {
                    ZoomableImageWithBoundingBox(
                        bitmap = capturedImage,
                        rect = capturedImageBoundingBox,
                        imageWidth = 480f,
                        imageHeight = 640f,
                        modifier = Modifier.fillMaxSize(),
                        onTransformedImage = { transformedBmp ->
                            val result = classifier?.classify(transformedBmp)
                            result?.let {
                                contentType = if (it.label == "FatBoxNot") "Not FatBox" else "FatBox"
                                isContentTypeValid = it.label != "FatBoxNot"
                                
                                quality = it.quality
                                isQualityValid = it.quality == "good"
                            }
                        }
                    )
                }

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                        .clickable { onClose() }
                )
            }

            // Analysis Results Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Image Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    AnalysisField(
                        label = "Content Type",
                        value = contentType,
                        isValid = isContentTypeValid,
                        overrideReason = contentTypeOverrideReason,
                        onOverrideReasonChange = { contentTypeOverrideReason = it }
                    )
                }

                item { HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f)) }

                item {
                    val qualityDisplay = when(quality) {
                        "good" -> "Good"
                        "blurry" -> "Blurry"
                        "underlit" -> "Underlit"
                        "blurry_underlit" -> "Blurry & Underlit"
                        else -> quality
                    }
                    AnalysisField(
                        label = "Image Quality",
                        value = qualityDisplay,
                        isValid = isQualityValid,
                        overrideReason = qualityOverrideReason,
                        onOverrideReasonChange = { qualityOverrideReason = it }
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisField(
    label: String,
    value: String,
    isValid: Boolean,
    overrideReason: String,
    onOverrideReasonChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isValid) Color.Unspecified else MaterialTheme.colorScheme.error
                )
            }
            
            if (value != "Analyzing...") {
                Icon(
                    imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = if (isValid) "Pass" else "Fail",
                    tint = if (isValid) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (!isValid && value != "Analyzing...") {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = overrideReason,
                onValueChange = onOverrideReasonChange,
                label = { Text("Override Reason", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = overrideReason.isBlank()
            )
            if (overrideReason.isBlank()) {
                Text(
                    text = "Reason is required to override",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
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
