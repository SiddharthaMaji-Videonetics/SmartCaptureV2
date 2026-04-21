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

@Composable
fun FocusBoxOverlay(
    modifier: Modifier = Modifier,
    imageWidth: Float,
    imageHeight: Float,
    isEditable: Boolean = false,
    isCenterGuideVisible: Boolean = true,
    onImageRectChanged: (RectF) -> Unit = {},
    onScreenRectChanged: (RectF) -> Unit = {}
) {
    var rect by remember { mutableStateOf<Rect?>(null) }
    var dragMode by remember { mutableStateOf(DragMode.NONE) }

    var isSnappedX by remember { mutableStateOf(false) }
    var isSnappedY by remember { mutableStateOf(false) }

    var dragAccumX by remember { mutableStateOf(0f) }
    var dragAccumY by remember { mutableStateOf(0f) }

    val configuration = LocalConfiguration.current

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
                            isSnappedX = false
                            isSnappedY = false
                        },
                        onDragCancel = {
                            dragMode = DragMode.NONE
                            isSnappedX = false
                            isSnappedY = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            dragAccumX += dragAmount.x
                            dragAccumY += dragAmount.y

                            rect = rect?.let { r ->

                                var updated = when (dragMode) {
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

                                    DragMode.MOVE -> r.translate(dragAmount.x, dragAmount.y)
                                    else -> r
                                }

                                // -------------------------
                                // SNAP (same as before)
                                // -------------------------
                                if (dragMode == DragMode.MOVE) {

                                    val snapThreshold = 30.dp.toPx()
                                    val releaseThreshold = 60.dp.toPx()


                                    val cx = size.width / 2f
                                    val cy = size.height / 2f

                                    val rectCx = updated.center.x
                                    val rectCy = updated.center.y

                                    if (isSnappedX && abs(dragAccumX) > releaseThreshold) {
                                        isSnappedX = false
                                        dragAccumX = 0f
                                    }
                                    if (isSnappedY && abs(dragAccumY) > releaseThreshold) {
                                        isSnappedY = false
                                        dragAccumY = 0f
                                    }

                                    if (isSnappedX) {
                                        updated = updated.translate(cx - rectCx, 0f)
                                    } else if (abs(rectCx - cx) < snapThreshold) {
                                        isSnappedX = true
                                        dragAccumX = 0f
                                        updated = updated.translate(cx - rectCx, 0f)
                                    }

                                    if (isSnappedY) {
                                        updated = updated.translate(0f, cy - rectCy)
                                    } else if (abs(rectCy - cy) < snapThreshold) {
                                        isSnappedY = true
                                        dragAccumY = 0f
                                        updated = updated.translate(0f, cy - rectCy)
                                    }
                                }


                                // SCREEN RECT (for overlay)
                                onScreenRectChanged(
                                    RectF(
                                        updated.left,
                                        updated.top,
                                        updated.right,
                                        updated.bottom
                                    )
                                )

                                // IMAGE RECT (for ML)
                                if (imageWidth > 0f && imageHeight > 0f) {
                                    val imageRect = updated.toImageRect(
                                        size.width.toFloat(),
                                        size.height.toFloat(),
                                        imageWidth,
                                        imageHeight
                                    )
                                    onImageRectChanged(imageRect)
                                }

                                updated
                            }
                        }
                    )
                }
        ) {

            // INIT
            if (rect == null) {
                rect = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    Rect(
                        size.width * 0.15f,
                        size.height * 0.15f,
                        size.width * 0.85f,
                        size.height * 0.85f
                    )
                } else {
                    Rect(
                        size.width * 0.2f,
                        size.height * 0.3f,
                        size.width * 0.8f,
                        size.height * 0.7f
                    )
                }
            }

            rect?.let { r ->

                // ALSO EMIT on first draw
                onScreenRectChanged(
                    RectF(r.left, r.top, r.right, r.bottom)
                )

                if (imageWidth > 0f && imageHeight > 0f) {
                    val imageRect = r.toImageRect(
                        size.width,
                        size.height,
                        imageWidth,
                        imageHeight
                    )
                    onImageRectChanged(imageRect)
                }

                // DRAW BOX
                drawRect(
                    color = Color.Green,
                    topLeft = Offset(r.left, r.top),
                    size = Size(r.width, r.height),
                    style = Stroke(width = 4.dp.toPx())
                )

                if (isCenterGuideVisible) {
                    val cx = size.width / 2
                    val cy = size.height / 2

                    drawLine(Color.Gray, Offset(cx, 0f), Offset(cx, size.height), 1.dp.toPx())
                    drawLine(Color.Gray, Offset(0f, cy), Offset(size.width, cy), 1.dp.toPx())
                }

                if (isEditable) {
                    drawHandles(r, 20.dp.toPx())
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

    // -------------------------
    // 🔥 ZOOM LOGIC (outside Canvas)
    // -------------------------

    val zoomHint = when {
        instruction.z < -1.5 -> "Move Closer"
        instruction.z > 0-> "Move Away"
        else -> null
    }

    Log.e("zoom", instruction.z.toString())
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }   // ✅ get size here
    ) {

        Canvas(modifier = Modifier.matchParentSize()) {

            val rect = focusRect
            val stroke = 8f
            val paddingY = 15f
            val threshold = 0.02f

            val color = Color.White.copy(alpha = blinkAlpha)

            val guideTop = rect.top - paddingY
            val guideBottom = rect.bottom + paddingY

            // LEFT
            if (instruction.h < -threshold) {
                drawRect(
                    color = color,
                    topLeft = Offset(rect.left - 15f, rect.top),
                    size = Size(stroke, rect.height())
                )
            }

            // RIGHT
            if (instruction.h > threshold) {
                drawRect(
                    color = color,
                    topLeft = Offset(rect.right - stroke + 15f, rect.top),
                    size = Size(stroke, rect.height())
                )
            }

            // TOP
            if (instruction.v < -threshold) {
                drawRect(
                    color = color,
                    topLeft = Offset(rect.left, guideTop),
                    size = Size(rect.width(), stroke)
                )
            }

            // BOTTOM
            if (instruction.v > threshold) {
                drawRect(
                    color = color,
                    topLeft = Offset(rect.left, guideBottom - stroke),
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