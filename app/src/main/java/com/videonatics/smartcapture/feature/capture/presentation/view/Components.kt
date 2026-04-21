package com.videonatics.smartcapture.feature.capture.presentation.view

import android.graphics.RectF
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import androidx.compose.ui.graphics.drawscope.Stroke
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

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    val executor = remember { Executors.newSingleThreadExecutor() }

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
    }

    AndroidView(
        modifier = modifier,
        factory = { previewView }
    )
}

@Composable
fun BoundingBoxOverlay(
    boxes: List<Detection>,
    focusedObj: RectF? = null,
    imageWidth: Float,
    imageHeight: Float,
    modifier: Modifier = Modifier,
    boxColor: Color = Color.Red,
    strokeWidth: Float = 2f,
    onCanvasSizeChanged: (Float, Float) -> Unit = { _, _ -> }
) {
    var detectionObjColor: Color
    var stroke: Float
    Canvas(modifier = modifier) {

        val viewW = size.width
        val viewH = size.height

        onCanvasSizeChanged(viewW, viewH)

        // IMPORTANT: FILL_CENTER uses max scale
        val scale = maxOf(viewW / imageWidth, viewH / imageHeight)

        val scaledW = imageWidth * scale
        val scaledH = imageHeight * scale

        val dx = (viewW - scaledW) / 2f
        val dy = (viewH - scaledH) / 2f

        boxes.forEach { b ->
            val rect = b.rect
           if(b.rect == focusedObj){
               detectionObjColor = Color.Yellow
               stroke = 2 * strokeWidth
            }else{
               stroke = strokeWidth
               detectionObjColor = boxColor
            }
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
                color = detectionObjColor,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
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

