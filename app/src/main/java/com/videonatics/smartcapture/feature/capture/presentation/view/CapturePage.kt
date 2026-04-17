package com.videonatics.smartcapture.feature.capture.presentation.view

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    ){
        val prep = preprocess(it)

        val output = Array(1) { Array(3549) { FloatArray(9) } }

        interpreter.run(prep.buffer, output)

        val detections = parseOutput(
            output,
            prep.ratio,
            prep.dx,
            prep.dy
        )

        val finalDetection = nmsPerClass(detections)
        if(finalDetection.isNotEmpty()){
            detectionList = finalDetection
        }


        val text = buildString {
            appendLine("Detected ${finalDetection.size} object(s):\n")

            finalDetection.forEach {
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
fun CapturePageComponent(modifier: Modifier = Modifier, detectionList: List<Detection> = emptyList(), onSnap: (Bitmap) -> Unit = {}) {

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
                    imageWidth = imageProxy.width.toFloat()
                    imageHeight = imageProxy.height.toFloat()
                    try {
                        val bmp = imageProxy.toBitmap()
                        Log.e("frame", "image frame injected")
                        // TODO: Use interpreter here
                        val bmpNew = bmp.copy(Bitmap.Config.ARGB_8888, false)
                        onSnap.invoke(bmpNew)
                    } finally {
                        imageProxy.close()
                    }
                }
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