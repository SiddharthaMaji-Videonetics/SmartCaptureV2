package com.videonatics.smartcapture.feature.details.presentation.view

import android.graphics.Bitmap
import android.graphics.RectF
import android.widget.Toast
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.feature.details.FatBoxClassifier
import com.videonatics.smartcapture.feature.details.data.model.ImageQualityResult
import com.videonatics.smartcapture.feature.details.presentation.viewmodel.CaptureDetailsViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun CaptureDetailsPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    sharedViewModel: SharedCoreViewModel,
    viewModel: CaptureDetailsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val bmp = sharedViewModel.getCapturedImage()
    val stageId = sharedViewModel.getCurrentStageId() ?: ""
    val uploadState by viewModel.uploadState.collectAsState()
    val qualityState by viewModel.qualityState.collectAsState()
    val classificationState by viewModel.classificationState.collectAsState()

    LaunchedEffect(uploadState) {
        when (uploadState) {
            is CaptureDetailsViewModel.UploadState.Success -> {
                Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            is CaptureDetailsViewModel.UploadState.Error -> {
                Toast.makeText(context, (uploadState as CaptureDetailsViewModel.UploadState.Error).message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    ImageAnalysisPageComponent(
        capturedImage = bmp,
        capturedImageBoundingBox = sharedViewModel.getCapturedImageBoundBox(),
        isUploading = uploadState is CaptureDetailsViewModel.UploadState.Loading,
        qualityResult = (qualityState as? CaptureDetailsViewModel.QualityState.Success)?.result,
        classificationResult = (classificationState as? CaptureDetailsViewModel.ClassificationState.Success)?.result,
        onAnalyze = { bitmap ->
            viewModel.analyzeQuality(bitmap)
            viewModel.classifyImage(bitmap)
        },
        onUploadClick = {
            bmp?.let { viewModel.uploadImage(stageId, it) }
        },
        onClose = {
            navController.popBackStack()
        }
    )
}

@Composable
fun ImageAnalysisPageComponent(
    modifier: Modifier = Modifier,
    capturedImage: Bitmap? = null,
    capturedImageBoundingBox: RectF? = null,
    isUploading: Boolean = false,
    qualityResult: ImageQualityResult? = null,
    classificationResult: FatBoxClassifier.Result? = null,
    onAnalyze: (Bitmap) -> Unit = {},
    onUploadClick: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    var contentType by remember { mutableStateOf("Analyzing...") }
    var quality by remember { mutableStateOf("Analyzing...") }
    var isContentTypeValid by remember { mutableStateOf(true) }
    var isQualityValid by remember { mutableStateOf(true) }
    
    var contentTypeOverrideReason by remember { mutableStateOf("") }
    var qualityOverrideReason by remember { mutableStateOf("") }

    LaunchedEffect(qualityResult) {
        qualityResult?.let { r ->
            quality = when {
                r.isBlurry && r.isUnderlit -> "Blurry & Underlit"
                r.isBlurry -> "Blurry"
                r.isUnderlit -> "Underlit"
                r.hasIssue -> r.issues.joinToString(" & ") { it.lowercase().replaceFirstChar(Char::uppercase) }
                else -> "Good"
            }
            isQualityValid = !r.hasIssue && !r.isBlurry && !r.isUnderlit
        }
    }

    LaunchedEffect(classificationResult) {
        classificationResult?.let { it ->
            contentType = if (it.label == "FatBoxNot") "Not FatBox" else "FatBox"
            isContentTypeValid = it.label != "FatBoxNot"
        }
    }

    val isUploadEnabled = (isContentTypeValid || contentTypeOverrideReason.isNotBlank()) &&
            (isQualityValid || qualityOverrideReason.isNotBlank()) &&
            contentType != "Analyzing..." && !isUploading

    Scaffold(
        bottomBar = {
            BottomAppBar() {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = onUploadClick,
                        enabled = isUploadEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
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
                            onAnalyze(transformedBmp)
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
                    AnalysisField(
                        label = "Image Quality",
                        value = quality,
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
