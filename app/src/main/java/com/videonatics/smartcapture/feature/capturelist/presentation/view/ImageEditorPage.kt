package com.videonatics.smartcapture.feature.capturelist.presentation.view


import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.videonatics.smartcapture.core.loadModelFile
import com.videonatics.smartcapture.core.toNormalized
import com.videonatics.smartcapture.core.toPixel
import com.videonatics.smartcapture.core.uriToBitmap
import com.videonatics.smartcapture.feature.capturelist.presentation.viewmodel.ImageEditorViewModel
import com.videonatics.smartcapture.feature.details.presentation.view.ImageWithBoundingBox
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import kotlin.math.min
import kotlin.math.sqrt

data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

enum class DragHandle {
    NONE,
    MOVE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}


@Composable
fun ImageEditorPage(
    modifier: Modifier = Modifier,
    title: String = "Image Editor",
    imgUri: Uri?,
    onBack: () -> Unit = {},
    onCrop: (Bitmap, Bitmap, RectF) -> Unit = {_, _, _->}
) {
    val vm = hiltViewModel<ImageEditorViewModel>()
    var interpreter by remember { mutableStateOf<Interpreter?>(null) }
    var gpuDelegate by remember { mutableStateOf<GpuDelegate?>(null) }
    val context = LocalContext.current

    val detectedBoxFlow = vm.detectedBox.collectAsState(null)
    val detectedBoxAngleFlow = vm.detectedBoxAngle.collectAsState(null)
    val detectedCornersFlow = vm.detectedCorners.collectAsState(null)

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val model = try {
                loadModelFile(context, "box_detector.tflite")
            } catch (e: Exception) {
                Log.e("CapturePage", "Failed to load model file", e)
                return@withContext
            }

            var interp: Interpreter? = null
            var delegate: GpuDelegate? = null

            try {
                val options = Interpreter.Options()
                try {
                    delegate = GpuDelegate()
                    options.addDelegate(delegate)
                } catch (e: Exception) {
                    Log.e("CapturePage", "GpuDelegate creation failed: ${e.message}")
                }

                interp = Interpreter(model, options)
                Log.d("CapturePage", "Interpreter initialized (GPU: ${delegate != null})")
            } catch (e: Exception) {
                Log.e("CapturePage", "Interpreter initialization with delegate failed: ${e.message}. Falling back to CPU.")
                delegate?.close()
                delegate = null

                try {
                    val cpuOptions = Interpreter.Options().apply {
                        setNumThreads(4)
                    }
                    interp = Interpreter(model, cpuOptions)
                    Log.d("CapturePage", "Interpreter initialized with CPU fallback")
                } catch (e2: Exception) {
                    Log.e("CapturePage", "CPU fallback also failed", e2)
                }
            }

            withContext(Dispatchers.Main) {
                interpreter = interp
                gpuDelegate = delegate
            }
        }
    }

    val bitmap = remember(imgUri) {
        imgUri?.let {
            uriToBitmap(context, it)
        }
    }

    LaunchedEffect(bitmap, interpreter) {
        if (bitmap != null && interpreter != null) {
            vm.precessImage(interpreter, bitmap)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            interpreter?.close()
            gpuDelegate?.close()
        }
    }

    if (bitmap == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    ImageEditorPageComponent(
        title = title,
        bitmap = bitmap,
        detectedBox = detectedBoxFlow.value,
        detectedBoxAngle = detectedBoxAngleFlow.value,
        detectedCorners = detectedCornersFlow.value,
        onBack = onBack,
        onCrop = onCrop,
        onCropSizeChange = { _, _ -> }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorPageComponent(
    title: String,
    bitmap: Bitmap,
    onBack: () -> Unit = {},
    onCropSizeChange: (Bitmap, RectF) -> Unit = { _, _ -> },
    onCrop: (Bitmap, Bitmap, RectF) -> Unit = { _, _, _ -> },
    detectedBox: RectF?,
    detectedBoxAngle: Float? = null,
    detectedCorners: List<PointF>? = null
) {
    var cropRect by remember {
        mutableStateOf<CropRect?>(null)
    }
    var canvasSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    var activeHandle by remember {
        mutableStateOf(DragHandle.NONE)
    }

    val handleRadius = 32f
    val minCropSize = 200f

    val effBox = remember(detectedBox, bitmap) {
        detectedBox?.toPixel(bitmap.width.toFloat(), bitmap.height.toFloat())
    }
    val properCorners = remember(detectedCorners, bitmap) {
        detectedCorners?.map { PointF(it.x * bitmap.width, it.y * bitmap.height) }
    }

    // Fallback: default centered square when canvas size is first known
    LaunchedEffect(canvasSize) {
        if (cropRect == null && canvasSize.width > 0 && canvasSize.height > 0) {
            val w = canvasSize.width.toFloat()
            val h = canvasSize.height.toFloat()
            val s = min(w, h) * 0.7f
            val l = (w - s) / 2f
            val t = (h - s) / 2f
            cropRect = CropRect(l, t, l + s, t + s)
        }
    }

    // Auto-fit crop rect to the detected bounding box on first detection
    var fittedToBox by remember { mutableStateOf(false) }
    LaunchedEffect(detectedBox, canvasSize) {
        if (!fittedToBox && detectedBox != null && canvasSize.width > 0 && canvasSize.height > 0) {
            fittedToBox = true
            val iw = bitmap.width.toFloat()
            val ih = bitmap.height.toFloat()
            val cw = canvasSize.width.toFloat()
            val ch = canvasSize.height.toFloat()
            val sc = min(cw / iw, ch / ih)
            val ox = (cw - iw * sc) / 2f
            val oy = (ch - ih * sc) / 2f
            val eb = detectedBox.toPixel(iw, ih)
            val pad = 28f
            cropRect = CropRect(
                left   = (ox + eb.left  * sc - pad).coerceAtLeast(0f),
                top    = (oy + eb.top   * sc - pad).coerceAtLeast(0f),
                right  = (ox + eb.right * sc + pad).coerceAtMost(cw),
                bottom = (oy + eb.bottom* sc + pad).coerceAtMost(ch)
            )
        }
    }

    // Crop is enabled only when the detected bounding box (effBox) is fully inside the crop rect.
    val isCropEnabled = run {
        val cr = cropRect ?: return@run false
        val eb = effBox ?: return@run false
        if (canvasSize.width == 0 || canvasSize.height == 0) return@run false
        val iw = bitmap.width.toFloat()
        val ih = bitmap.height.toFloat()
        val sc = min(canvasSize.width.toFloat() / iw, canvasSize.height.toFloat() / ih)
        val ox = (canvasSize.width.toFloat() - iw * sc) / 2f
        val oy = (canvasSize.height.toFloat() - ih * sc) / 2f
        // Convert effBox (bitmap pixel coords) to canvas coords and check containment
        val bl = ox + eb.left   * sc
        val bt = oy + eb.top    * sc
        val br = ox + eb.right  * sc
        val bb = oy + eb.bottom * sc
        cr.left <= bl && cr.top <= bt && cr.right >= br && cr.bottom >= bb
    }

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text(title)
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },

                actions = {

                    TextButton(
                        enabled = isCropEnabled,
                        onClick = {
                            cropRect?.let {
                                val (cropped, boundBox) = cropBitmapFromRect(
                                    originalBitmap = bitmap,
                                    cropRect = it,
                                    containerWidth = canvasSize.width.toFloat(),
                                    containerHeight = canvasSize.height.toFloat(),
                                    detectedBoxInBitmapPixels = effBox
                                )
                                onCrop(bitmap, cropped, boundBox)
                            }
                        }
                    ) {
                        Text("Crop")
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .onSizeChanged {
                    canvasSize = it
                }
                .pointerInput(Unit) {

                    detectDragGestures(

                        onDragStart = { offset ->

                            cropRect?.let {

                                activeHandle =
                                    detectHandleTouch(
                                        offset = offset,
                                        cropRect = it,
                                        radius = handleRadius * 2f
                                    )
                            }
                        },

                        onDragEnd = {
                            activeHandle = DragHandle.NONE
                            cropRect?.let {

                                val (cropped,boundBox) =
                                    cropBitmapFromRect(
                                        originalBitmap = bitmap,
                                        cropRect = it,
                                        containerWidth = canvasSize.width.toFloat(),
                                        containerHeight = canvasSize.height.toFloat()
                                    )
                                onCropSizeChange.invoke(cropped, boundBox)
                            }
                        },

                        onDragCancel = {
                            activeHandle = DragHandle.NONE
                        }

                    ) { change, dragAmount ->

                        change.consume()

                        cropRect?.let {

                            cropRect =
                                updateCropRect(
                                    cropRect = it,
                                    dragHandle = activeHandle,
                                    dx = dragAmount.x,
                                    dy = dragAmount.y,
                                    canvasWidth = canvasSize.width.toFloat(),
                                    canvasHeight = canvasSize.height.toFloat(),
                                    minSize = minCropSize
                                )



                        }
                    }
                }
        ) {

            ImageWithBoundingBox(
                bitmap = bitmap,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                rect = if (properCorners == null) effBox else null,
                corners = properCorners,
                rotationAngle = if (properCorners == null) (detectedBoxAngle ?: 0f) else 0f,
                boxColor = Color(0xFF00D77B)
            )

            if (detectedBox == null) {
                Text(
                    text = "No box detected",
                    color = Color.Yellow,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            }


            cropRect?.let { rect ->

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy =
                                CompositingStrategy.Offscreen
                        }
                ) {

                    // Dark overlay
                    drawRect(
                        color = Color.Black.copy(alpha = 0.65f)
                    )


                    // Transparent crop area
                    drawRect(
                        color = Color.Transparent,
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(
                            rect.right - rect.left,
                            rect.bottom - rect.top
                        ),
                        blendMode = BlendMode.Clear
                    )

                    // Border
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(
                            rect.right - rect.left,
                            rect.bottom - rect.top
                        ),
                        style = Stroke(width = 4f)
                    )

                    // Grid lines
                    val thirdWidth =
                        (rect.right - rect.left) / 3f

                    val thirdHeight =
                        (rect.bottom - rect.top) / 3f

                    repeat(2) { i ->

                        val x =
                            rect.left + thirdWidth * (i + 1)

                        drawLine(
                            color = Color.White.copy(alpha = 0.7f),
                            start = Offset(x, rect.top),
                            end = Offset(x, rect.bottom),
                            strokeWidth = 2f
                        )
                    }

                    repeat(2) { i ->

                        val y =
                            rect.top + thirdHeight * (i + 1)

                        drawLine(
                            color = Color.White.copy(alpha = 0.7f),
                            start = Offset(rect.left, y),
                            end = Offset(rect.right, y),
                            strokeWidth = 2f
                        )
                    }

                    drawHandle(rect.left, rect.top, handleRadius)
                    drawHandle(rect.right, rect.top, handleRadius)
                    drawHandle(rect.left, rect.bottom, handleRadius)
                    drawHandle(rect.right, rect.bottom, handleRadius)
                }
            }
        }
    }
}

private fun detectHandleTouch(
    offset: Offset,
    cropRect: CropRect,
    radius: Float
): DragHandle {

    fun distance(
        x: Float,
        y: Float
    ): Float {

        return sqrt(
            (offset.x - x) * (offset.x - x) +
                    (offset.y - y) * (offset.y - y)
        )
    }

    return when {

        distance(cropRect.left, cropRect.top) <= radius ->
            DragHandle.TOP_LEFT

        distance(cropRect.right, cropRect.top) <= radius ->
            DragHandle.TOP_RIGHT

        distance(cropRect.left, cropRect.bottom) <= radius ->
            DragHandle.BOTTOM_LEFT

        distance(cropRect.right, cropRect.bottom) <= radius ->
            DragHandle.BOTTOM_RIGHT

        offset.x in cropRect.left..cropRect.right &&
                offset.y in cropRect.top..cropRect.bottom ->
            DragHandle.MOVE

        else ->
            DragHandle.NONE
    }
}

private fun updateCropRect(
    cropRect: CropRect,
    dragHandle: DragHandle,
    dx: Float,
    dy: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    minSize: Float
): CropRect {

    var left = cropRect.left
    var top = cropRect.top
    var right = cropRect.right
    var bottom = cropRect.bottom

    when (dragHandle) {

        DragHandle.TOP_LEFT -> {

            left += dx
            top += dy

            if (right - left < minSize) {
                left = right - minSize
            }

            if (bottom - top < minSize) {
                top = bottom - minSize
            }
        }

        DragHandle.TOP_RIGHT -> {

            right += dx
            top += dy

            if (right - left < minSize) {
                right = left + minSize
            }

            if (bottom - top < minSize) {
                top = bottom - minSize
            }
        }

        DragHandle.BOTTOM_LEFT -> {

            left += dx
            bottom += dy

            if (right - left < minSize) {
                left = right - minSize
            }

            if (bottom - top < minSize) {
                bottom = top + minSize
            }
        }

        DragHandle.BOTTOM_RIGHT -> {

            right += dx
            bottom += dy

            if (right - left < minSize) {
                right = left + minSize
            }

            if (bottom - top < minSize) {
                bottom = top + minSize
            }
        }

        DragHandle.MOVE -> {

            val width =
                right - left

            val height =
                bottom - top

            left += dx
            right += dx

            top += dy
            bottom += dy

            if (left < 0f) {

                left = 0f
                right = width
            }

            if (top < 0f) {

                top = 0f
                bottom = height
            }

            if (right > canvasWidth) {

                right = canvasWidth
                left = right - width
            }

            if (bottom > canvasHeight) {

                bottom = canvasHeight
                top = bottom - height
            }
        }

        else -> Unit
    }

    left = left.coerceIn(0f, canvasWidth)
    top = top.coerceIn(0f, canvasHeight)

    right = right.coerceIn(0f, canvasWidth)
    bottom = bottom.coerceIn(0f, canvasHeight)

    return CropRect(
        left = left,
        top = top,
        right = right,
        bottom = bottom
    )
}

private fun cropBitmapFromRect(
    originalBitmap: Bitmap,
    cropRect: CropRect,
    containerWidth: Float,
    containerHeight: Float,
    detectedBoxInBitmapPixels: RectF? = null
): Pair<Bitmap, RectF> {

    val bitmapWidth =
        originalBitmap.width.toFloat()

    val bitmapHeight =
        originalBitmap.height.toFloat()

    val scale = min(
        containerWidth / bitmapWidth,
        containerHeight / bitmapHeight
    )

    val displayedImageWidth =
        bitmapWidth * scale

    val displayedImageHeight =
        bitmapHeight * scale

    val imageOffsetX =
        (containerWidth - displayedImageWidth) / 2f

    val imageOffsetY =
        (containerHeight - displayedImageHeight) / 2f

    val left = ((cropRect.left - imageOffsetX) / scale)
        .toInt()
        .coerceIn(0, originalBitmap.width - 1)

    val top = ((cropRect.top - imageOffsetY) / scale)
        .toInt()
        .coerceIn(0, originalBitmap.height - 1)

    // right/bottom must be strictly greater than left/top so the crop region is always valid
    val right = ((cropRect.right - imageOffsetX) / scale)
        .toInt()
        .coerceIn(left + 1, originalBitmap.width)

    val bottom = ((cropRect.bottom - imageOffsetY) / scale)
        .toInt()
        .coerceIn(top + 1, originalBitmap.height)

    val width = right - left
    val height = bottom - top

    val croppedBitmap = Bitmap.createBitmap(originalBitmap, left, top, width, height)

    // If a detected box is provided, compute its position inside the cropped region
    // and normalize it to the cropped bitmap dimensions so CaptureDetailsPage can
    // display it correctly on top of the cropped image.
    val boundBox = if (detectedBoxInBitmapPixels != null) {
        val boxLeft   = (detectedBoxInBitmapPixels.left   - left).coerceIn(0f, width.toFloat())
        val boxTop    = (detectedBoxInBitmapPixels.top    - top ).coerceIn(0f, height.toFloat())
        val boxRight  = (detectedBoxInBitmapPixels.right  - left).coerceIn(0f, width.toFloat())
        val boxBottom = (detectedBoxInBitmapPixels.bottom - top ).coerceIn(0f, height.toFloat())
        RectF(boxLeft, boxTop, boxRight, boxBottom).toNormalized(width.toFloat(), height.toFloat())
    } else {
        RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
            .toNormalized(bitmapWidth, bitmapHeight)
    }

    return Pair(croppedBitmap, boundBox)
}
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandle(
    x: Float,
    y: Float,
    radius: Float
) {

    drawCircle(
        color = Color.White,
        radius = radius,
        center = Offset(x, y)
    )
}

@Preview
@Composable
private fun ImageEditorPagePreview() {
    SmartCaptureTheme(
        darkTheme = true
    ) {
        ImageEditorPageComponent(
            title = "Image Editor",
            bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888),
            onBack = {},
            onCrop = { _, _, _ -> },
            detectedBox = null
        )
    }
}
