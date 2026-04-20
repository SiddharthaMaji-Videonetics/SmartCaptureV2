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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
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
import com.videonatics.smartcapture.feature.capture.presentation.viewmodel.CaptureViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CapturePage(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val labels = loadLabels(context)
    val viewModel = hiltViewModel<CaptureViewModel>()
    val objectInsideFocusBoxCount = viewModel.objectInsideFocusBoxCount.collectAsState(0)

    val instruction = viewModel.inst.collectAsState(null)
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
                objectInBoxCount = objectInsideFocusBoxCount.value,
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
fun CapturePageComponent(modifier: Modifier = Modifier, interpreter :  Interpreter? = null, objectInBoxCount: Int = 0,
                         inst: AlignmentVector? = null, updateCanvasSize:(Float, Float) -> Unit={ _, _->}, onFocusRectRect:(RectF)-> Unit = {}, onDetectionChanges: (List<Detection>) -> Unit = {}) {
    Scaffold { paddingValues ->

        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()){
            CameraDetectionView(interpreter,objectInBoxCount, inst, updateCanvasSize ,onFocusRectRect,onDetectionChanges)
        }

    }
}

@Composable
fun CameraDetectionView(interpreter: Interpreter?,
                        objectInBoxCount: Int,
                        inst: AlignmentVector?,
                        updateCanvasSize:(Float, Float) -> Unit,
                        onFocusRectRect:(RectF)-> Unit,
                        onDetectionChanges: (List<Detection>)-> Unit) {
    var imageWidth by remember { mutableStateOf(0f) }
    var imageHeight by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    var detectionList by remember { mutableStateOf<List<Detection>>(emptyList()) }

    @Composable
    fun BoxScope.toolbarComponent() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(R.drawable.ic_flash_on),
                modifier = Modifier.padding(5.dp),
                contentDescription = "Camera",
                tint = androidx.compose.ui.graphics.Color(0xFFFFFFFF),

                )

            Box(){
                Text(objectInBoxCount.toString(), style = TextStyle(
                    color = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                    fontSize = 20.sp
                ),
                    modifier = Modifier.padding(5.dp))
            }

        }
    }

    return Box(){

        var focusBox by remember { mutableStateOf<RectF?>(null) }
        AnimatedContent(targetState = (interpreter !=null)) {
            if(it){
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    CameraPreviewWithImageProxy(
                        frameIntervalMs = 500,
                        onFrame = { imageProxy ->
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

                                val finalDetections = nmsPerClass(detections).filter { it.score > 0.9 }

                                val mergedDetections = mergeToFittedBoxes(finalDetections, 5f)
                                // IMPORTANT: use preprocess source dims (rotation-aware)
                                imageWidth = prep.srcWidth.toFloat()
                                imageHeight = prep.srcHeight.toFloat()

                                detectionList = mergedDetections
                                onDetectionChanges.invoke(mergedDetections)

                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                imageProxy.close() // closed AFTER all usage
                            }
                            //}

                        },
                    )
                    BoundingBoxOverlay(
                        boxes = detectionList.map { d-> d.rect },
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        modifier = Modifier.fillMaxSize(),
                        onCanvasSizeChanged = { w, h ->
                            updateCanvasSize.invoke(w, h)
                        }
                    )
                    if(focusBox!=null) {
                        InstructionGuideOverlay(
                            modifier = Modifier
                                .fillMaxSize(),
                            focusBox!!,
                            inst,
                            imageWidth,
                            imageHeight
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

        toolbarComponent()
        if(imageWidth>0 && imageHeight >0){
        FocusBoxOverlay(
            modifier = Modifier.fillMaxSize(),
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            isEditable = true,
            isCenterGuideVisible = true,
            onRectChanged = {
                focusBox = it
                onFocusRectRect.invoke(it)
            }
        )}
    }



}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartCaptureTheme {
        CapturePageComponent()
    }
}