package com.videonatics.smartcapture.feature.capturelist.presentation.view


import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import com.videonatics.smartcapture.core.toNormalized
import com.videonatics.smartcapture.core.uriToBitmap
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import kotlin.math.max
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorPage(
    modifier: Modifier = Modifier,
    title: String = "Image Editor",
    imgUri: Uri?,
    onBack: () -> Unit = {},
    onCrop: (Bitmap, Bitmap, RectF) -> Unit = {_, _, _->}
) {

    val context = LocalContext.current

    val bitmap = remember(imgUri) {
        imgUri?.let {
            uriToBitmap(context, it)
        }
    }

    if (bitmap == null) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No Image Loaded")
        }

        return
    }

    val imageBitmap = remember(bitmap) {
        bitmap.asImageBitmap()
    }

    var canvasSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    var cropRect by remember {
        mutableStateOf<CropRect?>(null)
    }

    var activeHandle by remember {
        mutableStateOf(DragHandle.NONE)
    }

    val handleRadius = 32f
    val minCropSize = 200f

    LaunchedEffect(canvasSize) {

        if (
            cropRect == null &&
            canvasSize.width > 0 &&
            canvasSize.height > 0
        ) {

            val width = canvasSize.width.toFloat()
            val height = canvasSize.height.toFloat()

            val cropSize =
                min(width, height) * 0.7f

            val left =
                (width - cropSize) / 2f

            val top =
                (height - cropSize) / 2f

            cropRect = CropRect(
                left = left,
                top = top,
                right = left + cropSize,
                bottom = top + cropSize
            )
        }
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
                        onClick = {

                            cropRect?.let {

                                val (cropped,boundBox) =
                                    cropBitmapFromRect(
                                        originalBitmap = bitmap,
                                        cropRect = it,
                                        containerWidth = canvasSize.width.toFloat(),
                                        containerHeight = canvasSize.height.toFloat()
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
            modifier = modifier
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

            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

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
    containerHeight: Float
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

    val left =
        ((cropRect.left - imageOffsetX) / scale)
            .toInt()
            .coerceIn(0, originalBitmap.width - 1)

    val top =
        ((cropRect.top - imageOffsetY) / scale)
            .toInt()
            .coerceIn(0, originalBitmap.height - 1)

    val right =
        ((cropRect.right - imageOffsetX) / scale)
            .toInt()
            .coerceIn(1, originalBitmap.width)

    val bottom =
        ((cropRect.bottom - imageOffsetY) / scale)
            .toInt()
            .coerceIn(1, originalBitmap.height)

    val width =
        max(1, right - left)

    val height =
        max(1, bottom - top)

    val croppedBitmap =
        Bitmap.createBitmap(
            originalBitmap,
            left,
            top,
            width,
            height
        )

    val rectF = RectF(
        left.toFloat(),
        top.toFloat(),
        right.toFloat(),
        bottom.toFloat()
    )

    return Pair(
        croppedBitmap,
        rectF.toNormalized(bitmapWidth, bitmapHeight)
    )
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
        ImageEditorPage(
            title = "Image Editor",
            imgUri = null
        )
    }
}