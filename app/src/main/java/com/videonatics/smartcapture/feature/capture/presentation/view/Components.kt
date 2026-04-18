package com.videonatics.smartcapture.feature.capture.presentation.view

import android.graphics.RectF
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun CameraPreviewWithImageProxy(
    modifier: Modifier = Modifier,
    frameIntervalMs: Long = 150L,
    onFrame: (ImageProxy) -> Unit,
    onResolution: (width: Int, height: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    //  Keep executor stable
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->

            val previewView = PreviewView(ctx).apply {
                //  FULLSCREEN
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            cameraProviderFuture.addListener({

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

                    // ✅ Send resolution only once
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
                            imageProxy.close() // 🚨 ALWAYS
                        }
                        lastFrameTime = now
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

@Composable
fun BoundingBoxOverlay(
    boxes: List<RectF>,
    imageWidth: Float,
    imageHeight: Float,
    modifier: Modifier = Modifier,
    boxColor: Color = Color.Red,
    strokeWidth: Float = 4f
) {
    Canvas(modifier = modifier) {

        val viewW = size.width
        val viewH = size.height

        // IMPORTANT: FILL_CENTER uses max scale
        val scale = maxOf(viewW / imageWidth, viewH / imageHeight)

        val scaledW = imageWidth * scale
        val scaledH = imageHeight * scale

        val dx = (viewW - scaledW) / 2f
        val dy = (viewH - scaledH) / 2f

        boxes.forEach { rect ->

            val isNormalized = rect.right <= 1f

            val leftSrc = if (isNormalized) rect.left * imageWidth else rect.left
            val topSrc = if (isNormalized) rect.top * imageHeight else rect.top
            val rightSrc = if (isNormalized) rect.right * imageWidth else rect.right
            val bottomSrc = if (isNormalized) rect.bottom * imageHeight else rect.bottom

            val left = leftSrc * scale + dx
            val top = topSrc * scale + dy
            val right = rightSrc * scale + dx
            val bottom = bottomSrc * scale + dy

            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}