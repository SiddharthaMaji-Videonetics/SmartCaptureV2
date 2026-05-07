package com.videonatics.smartcapture.feature.details.presentation.view

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun ZoomableImageWithBoundingBox(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    rect: RectF? = null,
    corners: List<PointF>? = null,
    rotationAngle: Float = 0f,
    boxColor: Color = Color.Red,
    strokeWidth: Dp = 3.dp
) {
    val imageWidth = bitmap.width.toFloat()
    val imageHeight = bitmap.height.toFloat()

    var userScale by remember { mutableStateOf(1f) }
    var userOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val minScale = 1f
    val maxScale = 5f

    var isTransforming by remember { mutableStateOf(false) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        isTransforming = true
        userScale = (userScale * zoomChange).coerceIn(minScale, maxScale)
        userOffset += panChange
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { canvasSize = it }
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
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.all { !it.pressed }) {
                            isTransforming = false
                        }
                    }
                }
            }
    ) {

        val canvasW = canvasSize.width.toFloat()
        val canvasH = canvasSize.height.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (canvasW <= 0f || canvasH <= 0f) return@Canvas

            val baseScale = minOf(canvasW / imageWidth, canvasH / imageHeight)
            val finalScale = baseScale * userScale

            withTransform({
                translate(canvasW / 2f + userOffset.x, canvasH / 2f + userOffset.y)
                scale(userScale, userScale, pivot = Offset.Zero)
                scale(baseScale, baseScale, pivot = Offset.Zero)
                translate(-imageWidth / 2f, -imageHeight / 2f)
            }) {

                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstSize = IntSize(bitmap.width, bitmap.height)
                )

                if (corners != null && corners.size >= 4) {
                    val path = Path()
                    corners.forEachIndexed { index, pt ->
                        if (index == 0) path.moveTo(pt.x, pt.y)
                        else path.lineTo(pt.x, pt.y)
                    }
                    path.close()
                    drawPath(
                        path = path,
                        color = boxColor,
                        style = Stroke(width = strokeWidth.toPx() / finalScale)
                    )
                } else if (rect != null) {
                    withTransform({
                        rotate(rotationAngle, pivot = Offset(rect.centerX(), rect.centerY()))
                    }) {
                        drawRect(
                            color = boxColor,
                            topLeft = Offset(rect.left, rect.top),
                            size = androidx.compose.ui.geometry.Size(rect.width(), rect.height()),
                            style = Stroke(width = strokeWidth.toPx() / finalScale)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageWithBoundingBox(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    rect: RectF? = null,
    corners: List<PointF>? = null,
    rotationAngle: Float = 0f,
    boxColor: Color = Color.Red,
    strokeWidth: Dp = 3.dp,
    contentScale: ContentScale = ContentScale.Fit
) {

    val imageBitmap = remember(bitmap) {
        bitmap.asImageBitmap()
    }

    var canvasSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .onSizeChanged {
                canvasSize = it
            }
    ) {

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            if (canvasSize.width == 0 || canvasSize.height == 0) {
                return@Canvas
            }

            val canvasWidth = canvasSize.width.toFloat()
            val canvasHeight = canvasSize.height.toFloat()

            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()

            val scale = when (contentScale) {

                ContentScale.Crop -> {
                    maxOf(
                        canvasWidth / imageWidth,
                        canvasHeight / imageHeight
                    )
                }

                else -> {
                    minOf(
                        canvasWidth / imageWidth,
                        canvasHeight / imageHeight
                    )
                }
            }

            val displayedWidth = imageWidth * scale
            val displayedHeight = imageHeight * scale

            val offsetX = (canvasWidth - displayedWidth) / 2f
            val offsetY = (canvasHeight - displayedHeight) / 2f

            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(
                    offsetX.toInt(),
                    offsetY.toInt()
                ),
                dstSize = IntSize(
                    displayedWidth.toInt(),
                    displayedHeight.toInt()
                )
            )

            withTransform({

                translate(offsetX, offsetY)

                scale(scale, scale)

            }) {

                if (corners != null && corners.size >= 4) {

                    val path = Path()

                    corners.forEachIndexed { index, pt ->

                        if (index == 0) {
                            path.moveTo(pt.x, pt.y)
                        } else {
                            path.lineTo(pt.x, pt.y)
                        }
                    }

                    path.close()

                    drawPath(
                        path = path,
                        color = boxColor,
                        style = Stroke(
                            width = strokeWidth.toPx() / scale
                        )
                    )

                } else if (rect != null) {

                    withTransform({

                        rotate(
                            rotationAngle,
                            pivot = Offset(
                                rect.centerX(),
                                rect.centerY()
                            )
                        )

                    }) {

                        drawRect(
                            color = boxColor,
                            topLeft = Offset(
                                rect.left,
                                rect.top
                            ),
                            size = Size(
                                rect.width(),
                                rect.height()
                            ),
                            style = Stroke(
                                width = strokeWidth.toPx() / scale
                            )
                        )
                    }
                }
            }
        }
    }
}
