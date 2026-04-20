package com.videonatics.smartcapture.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.RectF
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.videonatics.smartcapture.core.domain.model.AlignmentVector
import com.videonatics.smartcapture.core.domain.model.Detection
import com.videonatics.smartcapture.core.domain.model.PreprocessResult
import com.videonatics.smartcapture.feature.capture.presentation.view.DragMode
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs

fun ImageProxy.toBitmap(): Bitmap? {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(
        nv21,
        ImageFormat.NV21,
        width,
        height,
        null
    )

    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 80, out)

    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
    val fileDescriptor = context.assets.openFd(modelName)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel

    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength

    return fileChannel.map(
        FileChannel.MapMode.READ_ONLY,
        startOffset,
        declaredLength
    )
}



fun preprocess(
    image: ImageProxy,
    size: Int = 416
): PreprocessResult {

    val rotation = image.imageInfo.rotationDegrees

    //  Rotated source dimensions (what model sees)
    val srcW = if (rotation == 90 || rotation == 270) image.height else image.width
    val srcH = if (rotation == 90 || rotation == 270) image.width else image.height

    val ratio = minOf(size.toFloat() / srcW, size.toFloat() / srcH)

    val newW = (srcW * ratio).toInt()
    val newH = (srcH * ratio).toInt()

    val dx = ((size - newW) / 2f)
    val dy = ((size - newH) / 2f)

    val buffer = ByteBuffer.allocateDirect(1 * 3 * size * size * 4)
    buffer.order(ByteOrder.nativeOrder())

    val total = size * size
    val pad = 114f

    val r = FloatArray(total) { pad }
    val g = FloatArray(total) { pad }
    val b = FloatArray(total) { pad }

    val yPlane = image.planes[0].buffer
    val uPlane = image.planes[1].buffer
    val vPlane = image.planes[2].buffer

    val yRowStride = image.planes[0].rowStride
    val uvRowStride = image.planes[1].rowStride
    val uvPixelStride = image.planes[1].pixelStride

    for (y in 0 until newH) {
        for (x in 0 until newW) {

            // 🔹 Map to rotated space
            val srcXr = (x / ratio).toInt().coerceIn(0, srcW - 1)
            val srcYr = (y / ratio).toInt().coerceIn(0, srcH - 1)

            // 🔹 Map rotated → sensor space (FIXED)
            val (srcX, srcY) = when (rotation) {
                90 -> Pair(srcYr, image.height - srcXr - 1)
                180 -> Pair(image.width - srcXr - 1, image.height - srcYr - 1)
                270 -> Pair(image.width - srcYr - 1, srcXr)
                else -> Pair(srcXr, srcYr)
            }

            // 🔹 Safety bounds
            if (srcX !in 0 until image.width || srcY !in 0 until image.height) continue

            val yIndex = srcY * yRowStride + srcX
            val uvIndex = (srcY / 2) * uvRowStride + (srcX / 2) * uvPixelStride

            val Y = (yPlane.get(yIndex).toInt() and 0xFF)
            val U = (uPlane.get(uvIndex).toInt() and 0xFF)
            val V = (vPlane.get(uvIndex).toInt() and 0xFF)

            // 🔹 YUV → RGB
            var rVal = Y + 1.370705f * (V - 128)
            var gVal = Y - 0.337633f * (U - 128) - 0.698001f * (V - 128)
            var bVal = Y + 1.732446f * (U - 128)

            rVal = rVal.coerceIn(0f, 255f)
            gVal = gVal.coerceIn(0f, 255f)
            bVal = bVal.coerceIn(0f, 255f)

            val px = ((y + dy.toInt()) * size + (x + dx.toInt()))

            if (px !in 0 until total) continue

            r[px] = rVal
            g[px] = gVal
            b[px] = bVal
        }
    }

    // 🔹 CHW (BGR)
    b.forEach { buffer.putFloat(it) }
    g.forEach { buffer.putFloat(it) }
    r.forEach { buffer.putFloat(it) }

    buffer.rewind()

    return PreprocessResult(
        buffer = buffer,
        ratio = ratio,
        dx = dx,
        dy = dy,
        srcWidth = srcW,
        srcHeight = srcH
    )
}

fun parseOutput(
    output: Array<Array<FloatArray>>, // [1, 3549, 9]
    ratio: Float,
    dx: Float,
    dy: Float,
    confThreshold: Float = 0.3f
): List<Detection> {

    val detections = mutableListOf<Detection>()

    for (row in output[0]) {

        val cx = row[0]
        val cy = row[1]
        val w = row[2]
        val h = row[3]
        val obj = row[4]

        var bestClass = -1
        var bestScore = 0f

        for (i in 5 until row.size) {
            val score = obj * row[i]
            if (score > bestScore) {
                bestScore = score
                bestClass = i - 5
            }
        }

        if (bestScore < confThreshold) continue

        //Correct mapping back to original image
        val x1 = ((cx - w / 2f) - dx) / ratio
        val y1 = ((cy - h / 2f) - dy) / ratio
        val x2 = ((cx + w / 2f) - dx) / ratio
        val y2 = ((cy + h / 2f) - dy) / ratio

        detections.add(
            Detection(RectF(x1, y1, x2, y2), bestScore, bestClass)
        )
    }

    return detections
}

fun iou(a: RectF, b: RectF): Float {
    val left = maxOf(a.left, b.left)
    val top = maxOf(a.top, b.top)
    val right = minOf(a.right, b.right)
    val bottom = minOf(a.bottom, b.bottom)

    val inter = maxOf(0f, right - left) * maxOf(0f, bottom - top)
    if (inter <= 0f) return 0f

    val areaA = (a.right - a.left) * (a.bottom - a.top)
    val areaB = (b.right - b.left) * (b.bottom - b.top)

    return inter / (areaA + areaB - inter)
}

fun nmsPerClass(
    detections: List<Detection>,
    iouThreshold: Float = 0.45f
): List<Detection> {

    val result = mutableListOf<Detection>()
    val grouped = detections.groupBy { it.classId }

    for ((_, dets) in grouped) {
        val sorted = dets.sortedByDescending { it.score }.toMutableList()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            result.add(best)

            val it = sorted.iterator()
            while (it.hasNext()) {
                if (iou(best.rect, it.next().rect) > iouThreshold) {
                    it.remove()
                }
            }
        }
    }

    return result
}

fun loadLabels(context: Context): List<String> {
    return context.assets.open("labels.txt")
        .bufferedReader()
        .readLines()
}

fun rotateRect(
    rect: RectF,
    imageWidth: Float,
    imageHeight: Float,
    rotationDegrees: Int
): RectF {

    return when (rotationDegrees) {

        0 -> rect

        90 -> RectF(
            rect.top,
            imageWidth - rect.right,
            rect.bottom,
            imageWidth - rect.left
        )

        180 -> RectF(
            imageWidth - rect.right,
            imageHeight - rect.bottom,
            imageWidth - rect.left,
            imageHeight - rect.top
        )

        270 -> RectF(
            imageHeight - rect.bottom,
            rect.left,
            imageHeight - rect.top,
            rect.right
        )

        else -> rect
    }
}

fun getDragMode(rect: Rect, touch: Offset, threshold: Float): DragMode {

    val nearLeft = abs(touch.x - rect.left) <= threshold
    val nearRight = abs(touch.x - rect.right) <= threshold
    val nearTop = abs(touch.y - rect.top) <= threshold
    val nearBottom = abs(touch.y - rect.bottom) <= threshold

    val isInside = rect.contains(touch)
    val isNearEdge = nearLeft || nearRight || nearTop || nearBottom

    if (isInside && !isNearEdge) return DragMode.MOVE

    return when {
        nearLeft && nearTop -> DragMode.TOP_LEFT
        nearRight && nearTop -> DragMode.TOP_RIGHT
        nearLeft && nearBottom -> DragMode.BOTTOM_LEFT
        nearRight && nearBottom -> DragMode.BOTTOM_RIGHT

        nearLeft -> DragMode.LEFT
        nearRight -> DragMode.RIGHT
        nearTop -> DragMode.TOP
        nearBottom -> DragMode.BOTTOM

        isInside -> DragMode.MOVE
        else -> DragMode.NONE
    }
}

fun DrawScope.drawHandles(rect: Rect, size: Float) {
    val radius = size / 2

    listOf(
        Offset(rect.left, rect.top),
        Offset(rect.right, rect.top),
        Offset(rect.left, rect.bottom),
        Offset(rect.right, rect.bottom)
    ).forEach { center ->

        drawCircle(
            color = androidx.compose.ui.graphics.Color.Red,
            radius = radius,
            center = center
        )
    }
}

fun List<android.graphics.Rect>.filterInside(container: android.graphics.Rect): List<android.graphics.Rect> {
    return filter { r ->
        r.left >= container.left &&
                r.top >= container.top &&
                r.right <= container.right &&
                r.bottom <= container.bottom
    }
}
fun RectF.mapToCanvas(
    imageWidth: Float,
    imageHeight: Float,
    canvasWidth: Float,
    canvasHeight: Float
): Rect {

    val scale = maxOf(canvasWidth / imageWidth, canvasHeight / imageHeight)

    val scaledW = imageWidth * scale
    val scaledH = imageHeight * scale

    val dx = (canvasWidth - scaledW) / 2f
    val dy = (canvasHeight - scaledH) / 2f

    val isNormalized = right <= 1f

    val leftSrc = if (isNormalized) left * imageWidth else left
    val topSrc = if (isNormalized) top * imageHeight else top
    val rightSrc = if (isNormalized) right * imageWidth else right
    val bottomSrc = if (isNormalized) bottom * imageHeight else bottom

    return Rect(
        leftSrc * scale + dx,
        topSrc * scale + dy,
        rightSrc * scale + dx,
        bottomSrc * scale + dy
    )
}

fun Rect.toImageRect(
    canvasWidth: Float,
    canvasHeight: Float,
    imageWidth: Float,
    imageHeight: Float
): RectF {

    val scale = maxOf(canvasWidth / imageWidth, canvasHeight / imageHeight)

    val scaledW = imageWidth * scale
    val scaledH = imageHeight * scale

    val dx = (canvasWidth - scaledW) / 2f
    val dy = (canvasHeight - scaledH) / 2f

    val leftImg = (left - dx) / scale
    val topImg = (top - dy) / scale
    val rightImg = (right - dx) / scale
    val bottomImg = (bottom - dy) / scale

    // Clamp to image bounds
    return RectF(
        leftImg.coerceIn(0f, imageWidth),
        topImg.coerceIn(0f, imageHeight),
        rightImg.coerceIn(0f, imageWidth),
        bottomImg.coerceIn(0f, imageHeight)
    )
}

fun mergeToFittedBoxes(
    detections: List<Detection>,
    margin: Float = 20f
): List<Detection> {


    val remaining = detections.toMutableList()
    val result = mutableListOf<Detection>()

    fun isClose(a: RectF, b: RectF): Boolean {
        val expanded = RectF(
            a.left - margin,
            a.top - margin,
            a.right + margin,
            a.bottom + margin
        )
        return RectF.intersects(expanded, b)
    }

    while (remaining.isNotEmpty()) {

        val seed = remaining.removeAt(0)

        var minLeft = seed.rect.left
        var minTop = seed.rect.top
        var maxRight = seed.rect.right
        var maxBottom = seed.rect.bottom

        var bestScore = seed.score
        var bestColor = seed.rectColor

        var changed: Boolean

        do {
            changed = false

            val iterator = remaining.iterator()

            while (iterator.hasNext()) {
                val other = iterator.next()

                // Remove this if you want cross-class merging
                if (other.classId != seed.classId) continue

                val currentRect = RectF(minLeft, minTop, maxRight, maxBottom)

                if (isClose(currentRect, other.rect)) {

                    // Expand cluster bounds
                    minLeft = minOf(minLeft, other.rect.left)
                    minTop = minOf(minTop, other.rect.top)
                    maxRight = maxOf(maxRight, other.rect.right)
                    maxBottom = maxOf(maxBottom, other.rect.bottom)

                    if (other.score > bestScore) {
                        bestScore = other.score
                        bestColor = other.rectColor
                    }

                    iterator.remove()
                    changed = true
                }
            }

        } while (changed)

        result.add(
            Detection(
                rect = RectF(minLeft, minTop, maxRight, maxBottom),
                score = bestScore,
                classId = seed.classId,
                rectColor = bestColor
            )
        )
    }

    return result
}

fun findBestBox(
    detections: List<Detection>,
    focusBox: RectF
): Detection? {
    return detections.maxByOrNull { det ->
        val intersection = RectF().apply {
            setIntersect(det.rect, focusBox)
        }
        val overlapArea = intersection.width() * intersection.height()
        val detArea = det.rect.width() * det.rect.height()

        // prioritize overlap, fallback to closeness
        val overlapRatio = if (detArea > 0) overlapArea / detArea else 0f

        val centerDist = distance(det.rect.centerX(), det.rect.centerY(),
            focusBox.centerX(), focusBox.centerY())

        overlapRatio * 2 - centerDist * 0.001f
    }
}

fun getDirection(dx: Float, dy: Float, threshold: Float): String {
    val horizontal = when {
        dx > threshold -> "Move Right"
        dx < -threshold -> "Move Left"
        else -> ""
    }

    val vertical = when {
        dy > threshold -> "Move Down"
        dy < -threshold -> "Move Up"
        else -> ""
    }

    return listOf(horizontal, vertical)
        .filter { it.isNotEmpty() }
        .joinToString(" & ")
        .ifEmpty { "Centered" }
}

fun getDistanceHint(areaRatio: Float): String {
    return when {
        areaRatio < 0.5 -> "Move Closer"
        areaRatio > 1.5 -> "Move Back"
        else -> "Good Distance"
    }
}

fun generateInstruction(best: Detection, focusBox: RectF): String {
    val dx = best.rect.centerX() - focusBox.centerX()
    val dy = best.rect.centerY() - focusBox.centerY()

    val direction = getDirection(dx, dy, threshold = 20f)

    val areaRatio = (best.rect.width() * best.rect.height()) /
            (focusBox.width() * focusBox.height())

    val distance = getDistanceHint(areaRatio)

    return when {
        direction == "Centered" && distance == "Good Distance" -> "Perfect Alignment ✅"
        direction == "Centered" -> distance
        else -> "$direction + $distance"
    }
}
fun generateAlignmentVector(
    best: Detection,
    focusBox: RectF
): AlignmentVector {

    val dx = best.rect.centerX() - focusBox.centerX()
    val dy = best.rect.centerY() - focusBox.centerY()

    // normalized horizontal / vertical
    val h = dx / focusBox.width()
    val v = dy / focusBox.height()

    // depth using area ratio (log makes it stable)
    val areaRatio =
        (best.rect.width() * best.rect.height()) /
                (focusBox.width() * focusBox.height())

    val z =Math.log(areaRatio.toDouble()).toFloat()

    return AlignmentVector(
        h = h,
        v = v,
        z = z
    )
}

fun distance(
    x1: Float, y1: Float,
    x2: Float, y2: Float
): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

fun imageToScreenRect(
    rect: RectF,
    screenW: Float,
    screenH: Float,
    imageW: Float,
    imageH: Float
): RectF {

    val scaleX = screenW / imageW
    val scaleY = screenH / imageH

    return RectF(
        rect.left * scaleX,
        rect.top * scaleY,
        rect.right * scaleX,
        rect.bottom * scaleY
    )
}