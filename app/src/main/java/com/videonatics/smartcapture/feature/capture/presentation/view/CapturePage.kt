package com.videonatics.smartcapture.feature.capture.presentation.view

import android.Manifest
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.videonatics.smartcapture.R
import com.videonatics.smartcapture.core.domain.model.AlignmentVector
import com.videonatics.smartcapture.core.domain.model.Detection
import com.videonatics.smartcapture.core.loadLabels
import com.videonatics.smartcapture.core.loadModelFile
import com.videonatics.smartcapture.core.mergeToFittedBoxes
import com.videonatics.smartcapture.core.nmsPerClass
import com.videonatics.smartcapture.core.parseOutput
import com.videonatics.smartcapture.core.preprocess
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.feature.capture.presentation.viewmodel.CaptureViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CapturePage(
    navController: NavController,
    sharedViewModel: SharedCoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<CaptureViewModel>()

    val instruction by viewModel.inst.collectAsState(null)
    val focusedObj by viewModel.focusedObj.collectAsState(null)
    val isCapturable by viewModel.isCapturable.collectAsState(false)
    val captureTrigger by viewModel.captureTrigger.collectAsState(false)

    // 🔥 Model + GPU (async loading)
    var interpreter by remember { mutableStateOf<Interpreter?>(null) }
    var gpuDelegate by remember { mutableStateOf<GpuDelegate?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {

            val options = Interpreter.Options()
            var delegate: GpuDelegate? = null

            try {
                delegate = GpuDelegate()
                options.addDelegate(delegate)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val model = loadModelFile(context, "model1.tflite")
            val interp = Interpreter(model, options)

            withContext(Dispatchers.Main) {
                interpreter = interp
                gpuDelegate = delegate
            }
        }
    }

    // 🔐 Camera permission
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    // ⏳ Loading state
    if (interpreter == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val labels = remember { loadLabels(context) }

    when {
        cameraPermissionState.status.isGranted -> {
                CapturePageComponent(
                    interpreter = interpreter!!,
                    focusedObj = focusedObj,
                    inst = instruction,
                    captureTrigger = captureTrigger,
                    isCapturable = isCapturable,
                    onCaptureImage = {
                        viewModel.captureImage()
                    },
                    onFullImageCapture = {
                        viewModel.captureReset()
                        sharedViewModel.saveCapturedImage(it)
                        focusedObj?.apply {
                            sharedViewModel.saveCapturedImageBoundBox(this)
                        }
                        navController.navigate("image_details")
                    },
                    updateCanvasSize = { w, h ->
                        viewModel.updateCanvasSize(w, h)
                    },
                    onFocusRectRect = {
                        viewModel.setFocusBox(it)

                    },
                    onImageDimensionsCaptured = { w, h ->
                        sharedViewModel.saveSourceImageDimensions(w, h)
                    }
                ) { detections ->

                    //removed heavy logging per frame
                    viewModel.setDetection(detections)
                }


        }

        cameraPermissionState.status.shouldShowRationale -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Camera permission is required")
            }
        }

        else -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Requesting permission...")
            }
        }
    }

    // 🧹 Cleanup
    DisposableEffect(Unit) {
        onDispose {
            interpreter?.close()
            gpuDelegate?.close()
        }
    }
}
@Composable
fun CapturePageComponent(modifier: Modifier = Modifier,
                         interpreter :  Interpreter? = null,
                         focusedObj: RectF? = null,
                         captureTrigger: Boolean = false,
                         inst: AlignmentVector? = null,
                         isCapturable: Boolean = false,
                         onFullImageCapture:(Bitmap)-> Unit ={},
                         onCaptureImage:()-> Unit ={},
                         updateCanvasSize:(Float, Float) -> Unit={ _, _->},
                         onFocusRectRect:(RectF)-> Unit = {},
                         onImageDimensionsCaptured: (Float, Float) -> Unit = { _, _ -> },
                         onDetectionChanges: (List<Detection>) -> Unit = {}) {
    Scaffold (
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            AnimatedVisibility(visible = isCapturable) {
                FloatingActionButton(onClick = {
                    onCaptureImage.invoke()
                }) {
                    Icon(painterResource(R.drawable.ic_camera),
                        contentDescription = "Edit Bounding box",)
                }
            }

        }
    ){ paddingValues ->

        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()){
            CameraDetectionView(interpreter,
                focusedObj,
                captureTrigger,
                inst,
                updateCanvasSize,
                onFocusRectRect,
                onFullImageCapture = onFullImageCapture,
                onImageDimensionsCaptured = onImageDimensionsCaptured,
                onDetectionChanges = onDetectionChanges)
        }

    }
}

@Composable
fun CameraDetectionView(interpreter: Interpreter?,
                        focusedObj: RectF? = null,
                        captureTrigger: Boolean,
                        inst: AlignmentVector?,
                        updateCanvasSize:(Float, Float) -> Unit,
                        onFocusRectRect:(RectF)-> Unit,
                        onFullImageCapture:(Bitmap)-> Unit ={},
                        onImageDimensionsCaptured: (Float, Float) -> Unit = { _, _ -> },
                        onDetectionChanges: (List<Detection>)-> Unit) {
    var imageWidth by remember { mutableStateOf(0f) }
    var imageHeight by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    var isBoundBoxEdit by remember { mutableStateOf(false) }
    var detectionList by remember { mutableStateOf<List<Detection>>(emptyList()) }


    return Box(){
        var focusBox by remember { mutableStateOf<RectF?>(null) }
        AnimatedContent(targetState = (interpreter !=null)) {
            if(it){
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    var isProcessing = false
                    CameraPreviewWithImageProxy(
                        captureTrigger = captureTrigger,
                        frameIntervalMs = 500,
                        onFrame = { imageProxy ->
                            if (isProcessing) {
                                imageProxy.close()
                                return@CameraPreviewWithImageProxy
                            }

                            isProcessing = true
                           //Async processing
                            //scope.launch(Dispatchers.Default) {
                                try {
                                    val prep = preprocess(imageProxy)

                                    val output = Array(1) { Array(3549) { FloatArray(9) } }

                                    interpreter?.run(prep.buffer, output)

                                    val detections = parseOutput(
                                        output,
                                        prep.ratio,
                                        prep.dx,
                                        prep.dy
                                    )

                                    val finalDetections =
                                        nmsPerClass(detections).filter { it.score > 0.9 }

                                    val merged = mergeToFittedBoxes(finalDetections, 5f)

                                  //  withContext(Dispatchers.Main) {
                                        detectionList = merged.toList()
                                        onDetectionChanges.invoke(merged)
                                        imageWidth = prep.srcWidth.toFloat()
                                        imageHeight = prep.srcHeight.toFloat()
                                        onImageDimensionsCaptured(imageWidth, imageHeight)
                                    Log.e("img","w=${imageProxy.width}, h=${imageProxy.height}")
                                   // }

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isProcessing = false
                                    imageProxy.close()
                                }
                            //}
                        },
                        onFullImageCapture = {
                            onFullImageCapture.invoke(it)
                        }
                    )
                    BoundingBoxOverlay(
                        boxes = detectionList,
                        focusedObj = focusedObj,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        modifier = Modifier.fillMaxSize(),
                        onCanvasSizeChanged = { w, h ->
                            updateCanvasSize.invoke(w, h)
                        },

                    )
                    if(focusBox!=null) {
                        InstructionGuideOverlay(
                            modifier = Modifier
                                .fillMaxSize(),
                            focusBox!!,
                            inst,
                            )
                    }

                }
            }else{
                Box(modifier = Modifier.fillMaxSize().background(
                    color = androidx.compose.ui.graphics.Color(0xFF000000)
                ),
                    contentAlignment = Alignment.Center){
                    Icon(
                        painterResource(R.drawable.ic_videocamera),
                        contentDescription = "Camera",
                        tint = androidx.compose.ui.graphics.Color(0xFF606060),
                        modifier = Modifier
                            .padding(16.dp)
                            .width(64.dp)
                            .height(64.dp)

                    )
                }
            }

        }

        Box() {

            if(imageWidth>0 && imageHeight >0){
                FocusBoxOverlay(
                    modifier = Modifier.fillMaxSize(),
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    isEditable = isBoundBoxEdit,
                    //isCenterGuideVisible = true,
                    onImageRectChanged = {

                        onFocusRectRect.invoke(it)
                    },
                    onScreenRectChanged = {
                        focusBox = it
                    }
                )}

            ToolbarComponent(isBoundBoxEdit, onBoundBoxEdit = {
                isBoundBoxEdit = it
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
