package com.videonatics.smartcapture.feature.capture.presentation.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.videonatics.smartcapture.core.drawHandles
import com.videonatics.smartcapture.core.getDragMode
import kotlin.math.abs

@Composable
fun FocusBoxOverlay(
    modifier: Modifier = Modifier,
    isEditable: Boolean = false,
    onRectChanged: (Rect) -> Unit = {}
) {
    var rect by remember { mutableStateOf<Rect?>(null) }
    var dragMode by remember { mutableStateOf(DragMode.NONE) }

    // snap state
    var isSnappedX by remember { mutableStateOf(false) }
    var isSnappedY by remember { mutableStateOf(false) }

    // drag intent (for release)
    var dragAccumX by remember { mutableStateOf(0f) }
    var dragAccumY by remember { mutableStateOf(0f) }

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

                                    // resize
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

                                    // move
                                    DragMode.MOVE -> r.translate(dragAmount.x, dragAmount.y)

                                    else -> r
                                }

                                //SNAP ONLY WHEN MOVING
                                if (dragMode == DragMode.MOVE) {

                                    val snapThreshold = 30.dp.toPx()
                                    val releaseThreshold = 60.dp.toPx()

                                    val cx = size.width / 2f
                                    val cy = size.height / 2f

                                    val rectCx = updated.center.x
                                    val rectCy = updated.center.y

                                    // 🔹 RELEASE FIRST (prevents jitter)
                                    if (isSnappedX && abs(dragAccumX) > releaseThreshold) {
                                        isSnappedX = false
                                        dragAccumX = 0f
                                    }
                                    if (isSnappedY && abs(dragAccumY) > releaseThreshold) {
                                        isSnappedY = false
                                        dragAccumY = 0f
                                    }

                                    // 🔹 APPLY SNAP
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

                                onRectChanged(updated)

                                updated
                            }
                        }
                    )
                }
        ) {

            // init rect once
            if (rect == null) {
                rect = Rect(
                    left = size.width * 0.2f,
                    top = size.height * 0.3f,
                    right = size.width * 0.8f,
                    bottom = size.height * 0.7f
                )
                rect?.let { onRectChanged(it) } //initial emit
            }

            rect?.let { r ->

                // box
                drawRect(
                    color = Color.Green,
                    topLeft = Offset(r.left, r.top),
                    size = Size(r.width, r.height),
                    style = Stroke(width = 4.dp.toPx())
                )

                // center guides
                val cx = size.width / 2
                val cy = size.height / 2

                drawLine(Color.Gray, Offset(cx, 0f), Offset(cx, size.height), 1.dp.toPx())
                drawLine(Color.Gray, Offset(0f, cy), Offset(size.width, cy), 1.dp.toPx())

                if(isEditable) {
                    drawHandles(r, 20.dp.toPx())
                }

            }
        }
    }
}

enum class DragMode {
    LEFT, RIGHT, TOP, BOTTOM,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    MOVE, NONE
}