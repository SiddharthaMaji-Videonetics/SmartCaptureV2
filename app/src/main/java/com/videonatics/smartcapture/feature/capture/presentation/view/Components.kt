package com.videonatics.smartcapture.feature.capture.presentation.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.media.AudioManager
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
    torchEnabled: Boolean = false,
    soundEnabled: Boolean = true,
    onFrame: (ImageProxy) -> Unit,
    onFullImageCapture:(Float, Bitmap) -> Unit,
    onResolution: (width: Int, height: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    val executor = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var activeCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val preview = remember { Preview.Builder().build() }

    LaunchedEffect(torchEnabled) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    LaunchedEffect(captureTrigger) {
        if (captureTrigger) {
            val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
            if (!soundEnabled) {
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
            }
            val mainExecutor = ContextCompat.getMainExecutor(context)
            imageCapture?.takePicture(
                mainExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        if (!soundEnabled) {
                            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, originalVolume, 0)
                        }
                        try {
                            val rotation = image.imageInfo.rotationDegrees.toFloat()
                            val bitmap = imageProxyToBitmap(image)
                            onFullImageCapture(rotation, bitmap)
                        } catch (e: Exception) {
                            Log.e("CameraPreview", "Error processing captured image", e)
                        } finally {
                            image.close()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        if (!soundEnabled) {
                            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, originalVolume, 0)
                        }
                        Log.e("CameraPreview", "Error capturing image: ${exception.message}", exception)
                    }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                preview.setSurfaceProvider(null)
                activeCameraProvider?.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraPreview", "Error during unbinding", e)
            }
            executor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        val cameraProvider = ContextCompat.getMainExecutor(context).let { mainExecutor ->
            cameraProviderFuture.get()
        }
        activeCameraProvider = cameraProvider

        preview.surfaceProvider = previewView.surfaceProvider

        var lastFrameTime = 0L
        var resolutionSent = false

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
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
                    Log.e("CameraPreview", "Error in onFrame analyzer", e)
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
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer,
                capture
            )
        } catch (e: Exception) {
            Log.e("CameraPreview", "Use case binding failed", e)
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
    isPerfectState: Boolean = false,
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

        if (imageWidth <= 0f || imageHeight <= 0f) return@Canvas

        val scale = maxOf(viewW / imageWidth, viewH / imageHeight)
        val scaledW = imageWidth * scale
        val scaledH = imageHeight * scale
        val dx = (viewW - scaledW) / 2f
        val dy = (viewH - scaledH) / 2f

        boxes.forEach { b ->
            val color = if (b == focusedObj) {if(isPerfectState) boxColor else Color.Yellow} else Color.Red
            val stroke = if (b == focusedObj) 2 * strokeWidth else strokeWidth

            val path = Path()
            b.corners.forEachIndexed { index, pt ->
                val x = pt.x * scale + dx
                val y = pt.y * scale + dy
                if (index == 0) path.moveTo(x, y)
                else path.lineTo(x, y)
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
fun ToolbarComponent(
    isLowLight: Boolean,
    flashOn: Boolean = false,
    onFlashToggle: (Boolean) -> Unit,
    isBoundBoxEdit: Boolean,
    onBoundBoxEdit: (Boolean) -> Unit = {},
    soundEnabled: Boolean = true,
    onSoundToggle: (Boolean) -> Unit = {},
    onToolbarSize: (w: Int, h: Int) -> Unit = { _, _ -> }
) {
    var boundBoxEdit by remember { mutableStateOf(isBoundBoxEdit) }
    var soundOn by remember { mutableStateOf(soundEnabled) }

    val infiniteTransition = rememberInfiniteTransition(label = "flashBlink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    val flashTint = when {
        flashOn -> Color(0xFFFF9800)
        isLowLight -> Color(0xFFFFFFFF).copy(alpha = blinkAlpha)
        else -> Color(0xFFFFFFFF)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .onSizeChanged { onToolbarSize.invoke(it.width, it.height) },
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (soundOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            modifier = Modifier
                .size(30.dp)
                .padding(5.dp)
                .clickable {
                    soundOn = !soundOn
                    onSoundToggle(soundOn)
                },
            contentDescription = "Sound toggle",
            tint = if (soundOn) Color(0xFFFFFFFF) else Color(0xFFFF9800),
        )

        Icon(
            painterResource(R.drawable.ic_bounding_box),
            modifier = Modifier
                .size(30.dp)
                .padding(5.dp)
                .clickable {
                    boundBoxEdit = !boundBoxEdit
                    onBoundBoxEdit(boundBoxEdit)
                },
            contentDescription = "Edit focus box",
            tint = if (boundBoxEdit) Color(0xFFFF9800) else Color(0xFFFFFFFF),
        )

        Icon(
            painterResource(R.drawable.ic_flash_on),
            modifier = Modifier
                .size(30.dp)
                .padding(5.dp)
                .clickable { onFlashToggle(!flashOn) },
            contentDescription = "Flash",
            tint = flashTint,
        )
    }
}
