package com.videonatics.smartcapture.feature.capture.presentation.view

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.videonatics.smartcapture.R
import com.videonatics.smartcapture.core.domain.model.Detection
import com.videonatics.smartcapture.core.imageProxyToBitmap
import java.util.concurrent.Executors

@Composable
fun CameraPreviewWithImageProxy(
    modifier: Modifier = Modifier,
    captureTrigger: Boolean = false,
    frameIntervalMs: Long = 150L,
    onFrame: (ImageProxy) -> Unit,
    onFullImageCapture:(Float, Bitmap) -> Unit,
    onResolution: (width: Int, height: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    val executor = remember { Executors.newSingleThreadExecutor() }


    LaunchedEffect(captureTrigger) {
        if (captureTrigger) {
            val executor = ContextCompat.getMainExecutor(context)

            imageCapture?.takePicture(
                executor,
                object : ImageCapture.OnImageCapturedCallback() {

                    override fun onCaptureSuccess(image: ImageProxy) {
                        val rotation = image.imageInfo.rotationDegrees.toFloat()
                        val bitmap = imageProxyToBitmap(image)
                        onFullImageCapture(rotation, bitmap)
                    }

                    override fun onError(exception: ImageCaptureException) {
                       exception.printStackTrace()
                        Log.e("CameraPreview", "Error capturing image: ${exception.message}")
                    }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().apply {
            surfaceProvider = previewView.surfaceProvider
        }

        var lastFrameTime = 0L
        var resolutionSent = false



        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()



        imageAnalyzer.setAnalyzer(executor) { imageProxy ->

            val now = System.currentTimeMillis()

            if (!resolutionSent) {
                onResolution(imageProxy.width, imageProxy.height)
                resolutionSent = true
            }

            if (now - lastFrameTime >= frameIntervalMs) {
                try {
                    onFrame(imageProxy)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    imageProxy.close()
                }
                lastFrameTime = now
            } else {
                imageProxy.close()
            }
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()

        imageCapture = capture
        try {
            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer,
                capture
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { previewView }
    )
}

@Composable
fun BoundingBoxOverlay(
    boxes: List<Detection>,
    focusedObj: Detection? = null,
    imageWidth: Float,
    imageHeight: Float,
    modifier: Modifier = Modifier,
    boxColor: Color = Color.Red,
    strokeWidth: Float = 2f,
    onCanvasSizeChanged: (Float, Float) -> Unit = { _, _ -> }
) {
    Canvas(modifier = modifier) {

        val viewW = size.width
        val viewH = size.height

        onCanvasSizeChanged(viewW, viewH)

        // FILL_CENTER scaling
        val scale = maxOf(viewW / imageWidth, viewH / imageHeight)

        val scaledW = imageWidth * scale
        val scaledH = imageHeight * scale

        val dx = (viewW - scaledW) / 2f
        val dy = (viewH - scaledH) / 2f

        boxes.forEach { b ->

            val color = if (b == focusedObj) Color.Yellow else boxColor
            val stroke = if (b == focusedObj) 2 * strokeWidth else strokeWidth

            val path = Path()

            b.corners.forEachIndexed { index, pt ->

                // Transform to canvas space
                val x = pt.x * scale + dx
                val y = pt.y * scale + dy

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            path.close()

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = stroke)
            )
        }
    }
}

@Composable
fun ToolbarComponent(isBoundBoxEdit: Boolean,
                     onBoundBoxEdit: (Boolean) -> Unit = {},
                     onToolbarSize: (w: Int, h: Int) -> Unit = {_,_->}) {
    var isBoundBoxEdit by remember { mutableStateOf(isBoundBoxEdit) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .onSizeChanged{
                onToolbarSize.invoke(
                    it.width,
                    it.height
                )
            },
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(R.drawable.ic_bounding_box),
            modifier = Modifier
                .size(30.dp)
                .padding(5.dp)
                .clickable(
                    onClick = {
                        isBoundBoxEdit = !isBoundBoxEdit
                        onBoundBoxEdit.invoke(isBoundBoxEdit)
                    }),
            contentDescription = "Edit Bounding box",
            tint = if(isBoundBoxEdit)
                androidx.compose.ui.graphics.Color(0xFFFF9800) else
                androidx.compose.ui.graphics.Color(0xFFFFFFFF),

            )

        Box(){
            Icon(
                painterResource(R.drawable.ic_flash_on),
                modifier = Modifier.padding(5.dp),
                contentDescription = "Camera",
                tint = androidx.compose.ui.graphics.Color(0xFFFFFFFF),

                )
        }

    }
}
