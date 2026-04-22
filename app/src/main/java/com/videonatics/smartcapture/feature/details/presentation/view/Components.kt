package com.videonatics.smartcapture.feature.details.presentation.view

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.videonatics.smartcapture.core.domain.model.Detection

@Composable
fun ZoomableImageWithBoundingBox(
    bitmap: Bitmap,
    rect: RectF,
    imageWidth: Float,
    imageHeight: Float,
    modifier: Modifier = Modifier,
    boxColor: Color = Color.Red,
    strokeWidth: Dp = 3.dp,
    onTransformedImage: (Bitmap) -> Unit = {}
) {

    var userScale by remember { mutableStateOf(1f) }
    var userOffset by remember { mutableStateOf(Offset.Zero) }

    val minScale = 1f
    val maxScale = 5f

    // 👇 Track gesture end
    var isTransforming by remember { mutableStateOf(false) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        isTransforming = true

        val newScale = (userScale * zoomChange).coerceIn(minScale, maxScale)
        val scaleFactor = newScale / userScale

        userScale = newScale
        userOffset += panChange
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        userScale = 1f
                        userOffset = Offset.Zero
                    }
                )
            }
            .transformable(state = transformState)
            .pointerInput(Unit) {
                // Detect gesture end
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        if (isTransforming) {
                            isTransforming = false
                        }
                    }
                }
            }
    ) {

        var latestOutput by remember { mutableStateOf<Bitmap?>(null) }

        Canvas(modifier = Modifier.fillMaxSize()) {

            if (imageWidth == 0f || imageHeight == 0f) return@Canvas

            val canvasW = size.width
            val canvasH = size.height

            val baseScale = minOf(
                canvasW / imageWidth,
                canvasH / imageHeight
            )

            val displayedW = imageWidth * baseScale
            val displayedH = imageHeight * baseScale

            val centerOffset = Offset(
                (canvasW - displayedW) / 2f,
                (canvasH - displayedH) / 2f
            )

            val finalScale = baseScale * userScale
            val finalOffset = centerOffset + userOffset

            // =============================
            // 🔥 Compute visible image rect
            // =============================
            val visibleLeft = ((0f - finalOffset.x) / finalScale)
                .coerceIn(0f, imageWidth)

            val visibleTop = ((0f - finalOffset.y) / finalScale)
                .coerceIn(0f, imageHeight)

            val visibleRight = ((canvasW - finalOffset.x) / finalScale)
                .coerceIn(0f, imageWidth)

            val visibleBottom = ((canvasH - finalOffset.y) / finalScale)
                .coerceIn(0f, imageHeight)

            val visibleWidth = (visibleRight - visibleLeft).coerceAtLeast(1f)
            val visibleHeight = (visibleBottom - visibleTop).coerceAtLeast(1f)

            // =============================
            // 🎯 Create cropped bitmap
            // =============================
            val safeLeft = visibleLeft.toInt().coerceIn(0, bitmap.width - 1)
            val safeTop = visibleTop.toInt().coerceIn(0, bitmap.height - 1)

            val safeWidth = visibleWidth.toInt()
                .coerceAtMost(bitmap.width - safeLeft)
                .coerceAtLeast(1)

            val safeHeight = visibleHeight.toInt()
                .coerceAtMost(bitmap.height - safeTop)
                .coerceAtLeast(1)

            val cropped = try {
                Bitmap.createBitmap(
                    bitmap,
                    safeLeft,
                    safeTop,
                    safeWidth,
                    safeHeight
                )
            } catch (e: Exception) {
                null
            }

            val output = cropped?.let {
                Bitmap.createScaledBitmap(
                    it,
                    canvasW.toInt(),
                    canvasH.toInt(),
                    true
                )
            }

            latestOutput = output

            // =============================
            // 🎨 DRAW
            // =============================
            withTransform({

                translate(finalOffset.x, finalOffset.y)
                scale(finalScale, finalScale)

            }) {

                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstSize = IntSize(
                        imageWidth.toInt(),
                        imageHeight.toInt()
                    )
                )

                drawRect(
                    color = boxColor,
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width(), rect.height()),
                    style = Stroke(width = strokeWidth.toPx() / finalScale)
                )
            }
        }

        // =============================
        // 🚀 Emit ONLY when gesture ends
        // =============================
        LaunchedEffect(isTransforming) {
            if (!isTransforming) {
                latestOutput?.let { onTransformedImage(it) }
            }
        }
    }
}