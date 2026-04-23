package com.videonatics.smartcapture.feature.capture.presentation.view

import android.content.res.Configuration
import android.graphics.RectF
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.videonatics.smartcapture.core.domain.model.AlignmentVector
import com.videonatics.smartcapture.core.drawHandles
import com.videonatics.smartcapture.core.getDragMode
import com.videonatics.smartcapture.core.toImageRect
import kotlin.math.abs


enum class HorizontalSnap { LEFT, RIGHT, CENTER }
enum class VerticalSnap { TOP, BOTTOM, CENTER }
@Composable
fun FocusBoxOverlay(
    modifier: Modifier = Modifier,
    imageWidth: Float,
    imageHeight: Float,
    isEditable: Boolean = false,
    onImageRectChanged: (RectF) -> Unit = {},
    onScreenRectChanged: (RectF) -> Unit = {}
) {
    var rect by remember { mutableStateOf<Rect?>(null) }
    var dragMode by remember { mutableStateOf(DragMode.NONE) }

    var dragAccumX by remember { mutableStateOf(0f) }
    var dragAccumY by remember { mutableStateOf(0f) }

    var snapX by remember { mutableStateOf<HorizontalSnap?>(null) }
    var snapY by remember { mutableStateOf<VerticalSnap?>(null) }

    Box(modifier = modifier.fillMaxSize()) {

        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(isEditable) {

                    if (!isEditable) return@pointerInput

                    detectDragGestures(
                        onDragStart = { offset ->
                            rect?.let {
                                dragMode = getDragMode(it, offset, 24.dp.toPx())
                            }
                            dragAccumX = 0f
                            dragAccumY = 0f
                        },
                        onDragEnd = {
                            dragMode = DragMode.NONE
                            snapX = null
                            snapY = null
                        },
                        onDragCancel = {
                            dragMode = DragMode.NONE
                            snapX = null
                            snapY = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            dragAccumX += dragAmount.x
                            dragAccumY += dragAmount.y

                            rect = rect?.let { r ->

                                // -------------------------
                                // APPLY DRAG
                                // -------------------------
                                var updated = when (dragMode) {
                                    DragMode.MOVE -> r.translate(dragAmount.x, dragAmount.y)

                                    DragMode.LEFT -> r.copy(left = r.left + dragAmount.x)
                                    DragMode.RIGHT -> r.copy(right = r.right + dragAmount.x)
                                    DragMode.TOP -> r.copy(top = r.top + dragAmount.y)
                                    DragMode.BOTTOM -> r.copy(bottom = r.bottom + dragAmount.y)

                                    DragMode.TOP_LEFT -> r.copy(
                                        left = r.left + dragAmount.x,
                                        top = r.top + dragAmount.y
                                    )

                                    DragMode.TOP_RIGHT -> r.copy(
                                        right = r.right + dragAmount.x,
                                        top = r.top + dragAmount.y
                                    )

                                    DragMode.BOTTOM_LEFT -> r.copy(
                                        left = r.left + dragAmount.x,
                                        bottom = r.bottom + dragAmount.y
                                    )

                                    DragMode.BOTTOM_RIGHT -> r.copy(
                                        right = r.right + dragAmount.x,
                                        bottom = r.bottom + dragAmount.y
                                    )

                                    else -> r
                                }

                                // -------------------------
                                // 🔒 Bounds + Min Size
                                // -------------------------
                                val minSize = 80.dp.toPx()

                                var left = updated.left.coerceIn(0f, size.width.toFloat())
                                var right = updated.right.coerceIn(0f, size.width.toFloat())
                                var top = updated.top.coerceIn(0f, size.height.toFloat())
                                var bottom = updated.bottom.coerceIn(0f, size.height.toFloat())

                                if (right - left < minSize) {
                                    if (dragMode.name.contains("LEFT")) left = right - minSize
                                    else right = left + minSize
                                }

                                if (bottom - top < minSize) {
                                    if (dragMode.name.contains("TOP")) top = bottom - minSize
                                    else bottom = top + minSize
                                }

                                updated = Rect(left, top, right, bottom)

                                // -------------------------
                                // 🧲 SNAP (WITH DETACH)
                                // -------------------------
                                if (dragMode == DragMode.MOVE) {

                                    val snapThreshold = 5.dp.toPx()
                                    val releaseThreshold = 10.dp.toPx()

                                    val cx = size.width / 2f
                                    val cy = size.height / 2f

                                    val rectCx = updated.center.x
                                    val rectCy = updated.center.y

                                    // RELEASE SNAP
                                    if (snapX != null && abs(dragAccumX) > releaseThreshold) {
                                        snapX = null
                                        dragAccumX = 0f
                                    }

                                    if (snapY != null && abs(dragAccumY) > releaseThreshold) {
                                        snapY = null
                                        dragAccumY = 0f
                                    }

                                    // DETECT SNAP (only if not snapped)
                                    if (snapX == null) {
                                        snapX = when {
                                            abs(updated.left) < snapThreshold -> HorizontalSnap.LEFT
                                            abs(updated.right - size.width) < snapThreshold -> HorizontalSnap.RIGHT
                                            abs(rectCx - cx) < snapThreshold -> HorizontalSnap.CENTER
                                            else -> null
                                        }?.also { dragAccumX = 0f }
                                    }

                                    if (snapY == null) {
                                        snapY = when {
                                            abs(updated.top) < snapThreshold -> VerticalSnap.TOP
                                            abs(updated.bottom - size.height) < snapThreshold -> VerticalSnap.BOTTOM
                                            abs(rectCy - cy) < snapThreshold -> VerticalSnap.CENTER
                                            else -> null
                                        }?.also { dragAccumY = 0f }
                                    }

                                    // APPLY SNAP
                                    updated = when (snapX) {
                                        HorizontalSnap.LEFT -> updated.translate(-updated.left, 0f)
                                        HorizontalSnap.RIGHT -> updated.translate(size.width - updated.right, 0f)
                                        HorizontalSnap.CENTER -> updated.translate(cx - rectCx, 0f)
                                        null -> updated
                                    }

                                    updated = when (snapY) {
                                        VerticalSnap.TOP -> updated.translate(0f, -updated.top)
                                        VerticalSnap.BOTTOM -> updated.translate(0f, size.height - updated.bottom)
                                        VerticalSnap.CENTER -> updated.translate(0f, cy - rectCy)
                                        null -> updated
                                    }

                                    // SCREEN RECT (for overlay)
                                    onScreenRectChanged( RectF( updated.left, updated.top, updated.right, updated.bottom ) )
                                // IMAGE RECT (for ML)
                                    if (imageWidth > 0f && imageHeight > 0f) {
                                        val imageRect = updated.toImageRect( size.width.toFloat(), size.height.toFloat(), imageWidth, imageHeight )
                                        onImageRectChanged(imageRect)
                                    }
                                }

                                updated
                            }
                        }
                    )
                }
        ) {

            // INIT using Canvas size (correct)
            if (rect == null) {
                rect = Rect(
                    size.width * 0.2f,
                    size.height * 0.3f,
                    size.width * 0.8f,
                    size.height * 0.7f
                )
            }

            rect?.let { r ->

                //ALWAYS EMIT HERE (correct coordinate space)
                onScreenRectChanged(
                    RectF(r.left, r.top, r.right, r.bottom)
                )

                if (imageWidth > 0 && imageHeight > 0) {
                    onImageRectChanged(
                        r.toImageRect(
                            size.width,
                            size.height,
                            imageWidth,
                            imageHeight
                        )
                    )
                }

                // DRAW BOX
                drawRect(
                    color = Color.Green,
                    topLeft = Offset(r.left, r.top),
                    size = Size(r.width, r.height),
                    style = Stroke(4.dp.toPx())
                )

                if (isEditable) {
                    drawHandles(r, 20.dp.toPx())
                }

                // SNAP GUIDES
                val cx = size.width / 2
                val cy = size.height / 2

                when (snapX) {
                    HorizontalSnap.LEFT ->
                        drawLine(Color.Yellow, Offset(0f, 0f), Offset(0f, size.height), 2.dp.toPx())
                    HorizontalSnap.RIGHT ->
                        drawLine(Color.Yellow, Offset(size.width, 0f), Offset(size.width, size.height), 2.dp.toPx())
                    HorizontalSnap.CENTER ->
                        drawLine(Color.Yellow, Offset(cx, 0f), Offset(cx, size.height), 2.dp.toPx())
                    null -> {}
                }

                when (snapY) {
                    VerticalSnap.TOP ->
                        drawLine(Color.Yellow, Offset(0f, 0f), Offset(size.width, 0f), 2.dp.toPx())
                    VerticalSnap.BOTTOM ->
                        drawLine(Color.Yellow, Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx())
                    VerticalSnap.CENTER ->
                        drawLine(Color.Yellow, Offset(0f, cy), Offset(size.width, cy), 2.dp.toPx())
                    null -> {}
                }
            }
        }
    }
}

@Composable
fun InstructionGuideOverlay(
    modifier: Modifier = Modifier,
    focusRect: RectF,
    instruction: AlignmentVector?
) {

    if (instruction == null) return

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val blinkAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(500),
            RepeatMode.Reverse
        )
    )


    val zoomHint = when {
        instruction.z < -1.5 -> "Move Closer"
        instruction.z > 0-> "Move Away"
        else -> null
    }

    Log.e("zoom", instruction.z.toString())
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {

        Canvas(modifier = Modifier.matchParentSize()) {

            val rect = focusRect
            val stroke = 8f
            val paddingY = 15f
            val threshold = 0.02f

            val color = Color.Red.copy(alpha = blinkAlpha)

            val guideTop = rect.top
            val guideBottom = rect.bottom + 30

            // LEFT
            if (instruction.h < -threshold) {
                drawRect(
                    color = color,
                    topLeft = Offset(rect.left - 15f, guideTop + 10),
                    size = Size(stroke, rect.height() )
                )
            }

            // RIGHT
            if (instruction.h > threshold) {
                drawRect(
                    color = color,
                    topLeft = Offset(rect.right - stroke + 15f, guideTop + 10),
                    size = Size(stroke, rect.height() )
                )
            }

            // TOP
            if (instruction.v < -threshold) {
                drawRect(
                    color = color,
                    topLeft = Offset(rect.left, guideTop- paddingY),
                    size = Size(rect.width(), stroke)
                )
            }

            // BOTTOM
            if (instruction.v > threshold) {
                drawRect(
                    color = color,
                    topLeft = Offset(rect.left, guideBottom - paddingY),
                    size = Size(rect.width(), stroke)
                )
            }
        }

        zoomHint?.let {
            Text(
                text = it,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

enum class DragMode {
    LEFT, RIGHT, TOP, BOTTOM,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    MOVE, NONE
}