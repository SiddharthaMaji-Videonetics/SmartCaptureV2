package com.videonatics.smartcapture.feature.capture.presentation.view

import android.Manifest
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.videonatics.smartcapture.core.toBitmap
import com.videonatics.smartcapture.core.toFrameData
import com.videonatics.smartcapture.feature.capture.presentation.viewmodel.CaptureViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CapturePage(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val labels = loadLabels(context)
    val viewModel = hiltViewModel<CaptureViewModel>()

    val instruction = viewModel.inst.collectAsState(null)
    val focusedObj = viewModel.focusedObj.collectAsState(null)
    val (interpreter, gpuDelegate) = remember {
        val options = Interpreter.Options()
        var delegate: GpuDelegate? = null
        try {
            delegate = GpuDelegate()
            options.addDelegate(delegate)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val model = loadModelFile(context, "model1.tflite")
        val interpreter = Interpreter(model, options)

        val inputTensor = interpreter.getInputTensor(0)
        val outputTensor = interpreter.getOutputTensor(0)

        Log.d("CapturePage", "Input shape: ${inputTensor.shape().contentToString()}")
        Log.d("CapturePage", "Input type: ${inputTensor.dataType()}")
        Log.d("CapturePage", "Output shape: ${outputTensor.shape().contentToString()}")
        Log.d("CapturePage", "Output type: ${outputTensor.dataType()}")

        Pair(interpreter, delegate)
    }
    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )
    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }
    when {
        cameraPermissionState.status.isGranted -> {
            CapturePageComponent(
                interpreter = interpreter,
                focusedObj = focusedObj.value,
                inst = instruction.value,
                updateCanvasSize ={w,h->
                    viewModel.updateCanvasSize(w, h)
                },
                onFocusRectRect = {
                    viewModel.setFocusBox(it)
                }
            ){



                val text = buildString {
                    appendLine("Detected ${it.size} object(s):\n")

                    it.forEach {
                        val label = labels[it.classId]
                        appendLine(
                            "$label → ${"%.2f".format(it.score)} | " +
                                    "[${it.rect.left.toInt()}, ${it.rect.top.toInt()}, " +
                                    "${it.rect.right.toInt()}, ${it.rect.bottom.toInt()}]"
                        )
                    }
                }

                viewModel.setDetection(it)
                Log.e("detection", text)

                //onResult(prep.paddedBitmap, text, final)
            }
        }

        cameraPermissionState.status.shouldShowRationale -> {
            Text("Camera permission is required")
        }

        else -> {
            Text("Requesting permission...")
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            interpreter.close()
            gpuDelegate?.close()
        }
    }

}

@Composable
fun CapturePageComponent(modifier: Modifier = Modifier,
                         interpreter :  Interpreter? = null,
                         focusedObj: RectF? = null,
                         inst: AlignmentVector? = null,
                         updateCanvasSize:(Float, Float) -> Unit={ _, _->}, onFocusRectRect:(RectF)-> Unit = {}, onDetectionChanges: (List<Detection>) -> Unit = {}) {
    Scaffold (
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            AnimatedVisibility(visible = inst?.h == 0f && inst.v == 0f) {
                FloatingActionButton(onClick = {

                }) {
                    Icon(painterResource(R.drawable.ic_bounding_box),
                        contentDescription = "Edit Bounding box",)
                }
            }

        }
    ){ paddingValues ->

        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()){
            CameraDetectionView(interpreter,
                focusedObj,
                inst,
                updateCanvasSize,
                onFocusRectRect,
                onDetectionChanges)
        }

    }
}

@Composable
fun CameraDetectionView(interpreter: Interpreter?,
                        focusedObj: RectF? = null,
                        inst: AlignmentVector?,
                        updateCanvasSize:(Float, Float) -> Unit,
                        onFocusRectRect:(RectF)-> Unit,
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
                                   // }

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isProcessing = false
                                    imageProxy.close()
                                }
                            //}
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