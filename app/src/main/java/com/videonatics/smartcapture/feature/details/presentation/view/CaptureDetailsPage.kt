package com.videonatics.smartcapture.feature.details.presentation.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun CaptureDetailsPage(modifier: Modifier = Modifier,
                       navController: NavController,
                       sharedViewModel: SharedCoreViewModel) {

    ImageAnalysisPageComponent(capturedImage = sharedViewModel.getCapturedImage(),
        capturedImageBoundingBox = sharedViewModel.getCapturedImageBoundBox(),)
}


@Preview(showBackground = true)
@Composable
private fun ImageAnalysisPageComponentPreview() {
    SmartCaptureTheme {
        ImageAnalysisPageComponent()
    }
}
