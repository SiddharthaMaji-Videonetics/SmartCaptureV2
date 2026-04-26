package com.videonatics.smartcapture.feature.details.presentation.view

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.videonatics.smartcapture.core.cropBitmap
import com.videonatics.smartcapture.core.presentation.view.PageRoute
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.core.toNormalized
import com.videonatics.smartcapture.core.toPixel
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
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val qualityState by viewModel.qualityState.collectAsState()
    val classificationState by viewModel.classificationState.collectAsState()

    val sourceWidth = sharedViewModel.getSourceImageWidth()
    val sourceHeight = sharedViewModel.getSourceImageHeight()

    LaunchedEffect(uploadState) {
        when (uploadState) {
            is CaptureDetailsViewModel.UploadState.Success -> {
                Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                navController.popBackStack(PageRoute.HOME.name, inclusive = false)
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
        imageWidth = sourceWidth,
        imageHeight = sourceHeight,
        isUploading = uploadState is CaptureDetailsViewModel.UploadState.Loading,
        uploadProgress = uploadProgress,
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
    imageWidth: Float = 480f,
    imageHeight: Float = 640f,
    isUploading: Boolean = false,
    uploadProgress: Float = 0f,
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
            if(it.confidence > 0.8) {
                contentType = if (it.label == "FatBoxNot") "Not FatBox" else "FatBox"
                isContentTypeValid = it.label != "FatBoxNot"
            } else {
                contentType = "Not FatBox"
                isContentTypeValid = false
            }
        }
    }

    val isUploadEnabled = (isContentTypeValid || contentTypeOverrideReason.isNotBlank()) &&
            (isQualityValid || qualityOverrideReason.isNotBlank()) &&
            contentType != "Analyzing..." && !isUploading

    Scaffold(
        bottomBar = {
            BottomAppBar(
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (isUploading) {
                            Text(
                                text = "Uploading... ${(uploadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            LinearProgressIndicator(
                                progress = { uploadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        } else {
                            Button(
                                onClick = onUploadClick,
                                enabled = isUploadEnabled,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Upload")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                // Image Preview Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .background(Color.Black)
                ) {
                    if (capturedImage != null && capturedImageBoundingBox != null) {
//
//                        val croppedImage = cropBitmapFromNormalizedRect(
//                            capturedImage,
//                            capturedImageBoundingBox
//                        )


                        val normalizedRect = capturedImageBoundingBox.toNormalized(imageWidth,imageHeight)
                       val properRect = normalizedRect.toPixel(capturedImage.width.toFloat(),capturedImage.height.toFloat())
                        val croppedBmp = cropBitmap(
                            capturedImage,
                            properRect
                        )
                         croppedBmp?.apply {

                            onAnalyze(croppedBmp)
                            ZoomableImageWithBoundingBox(
                                bitmap = capturedImage,
                                rect = properRect,
                                modifier = Modifier.fillMaxSize()
                            )
                        }


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
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Image Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    AnalysisField(
                        label = "Content Type",
                        value = contentType,
                        isValid = isContentTypeValid,
                        overrideReason = contentTypeOverrideReason,
                        onOverrideReasonChange = { contentTypeOverrideReason = it }
                    )

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

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
