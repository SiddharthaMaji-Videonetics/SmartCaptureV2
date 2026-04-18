package com.videonatics.smartcapture.feature.capture.presentation.view

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.videonatics.smartcapture.core.domain.model.Detection
import com.videonatics.smartcapture.core.loadLabels
import com.videonatics.smartcapture.core.loadModelFile
import com.videonatics.smartcapture.core.nmsPerClass
import com.videonatics.smartcapture.core.parseOutput
import com.videonatics.smartcapture.core.preprocess
import com.videonatics.smartcapture.core.toBitmap
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate

@Composable
fun CapturePage(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val labels = loadLabels(context)

    var detectionList by remember { mutableStateOf<List<Detection>>(emptyList()) }

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

    DisposableEffect(Unit) {
        onDispose {
            interpreter.close()
            gpuDelegate?.close()
        }
    }
    CapturePageComponent(
        detectionList = detectionList,
        interpreter = interpreter
    ){

        if(it.isNotEmpty()){
            detectionList = it
        }


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

        Log.e("detection", text)

        //onResult(prep.paddedBitmap, text, final)
    }
}

@Composable
fun CapturePageComponent(modifier: Modifier = Modifier, detectionList: List<Detection> = emptyList(),  interpreter :  Interpreter? = null, onDetectionChanges: ( List<Detection>) -> Unit = {}) {

    val scope = rememberCoroutineScope()
    var imageWidth by remember { mutableStateOf(0f) }
    var imageHeight by remember { mutableStateOf(0f) }


    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CameraPreviewWithImageProxy(
                frameIntervalMs = 200,
                onFrame = { imageProxy ->
                    scope.launch(Dispatchers.Default) {
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

                            val finalDetections = nmsPerClass(detections)

                            // IMPORTANT: use preprocess source dims (rotation-aware)
                            imageWidth = prep.srcWidth.toFloat()
                            imageHeight = prep.srcHeight.toFloat()

                            onDetectionChanges.invoke(finalDetections)

                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            imageProxy.close() // closed AFTER all usage
                        }
                    }

                },
            )

            BoundingBoxOverlay(
                boxes = detectionList.map { d-> d.rect },
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                modifier = Modifier.fillMaxSize()
            )
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