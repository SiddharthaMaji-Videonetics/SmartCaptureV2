package com.videonatics.smartcapture.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.utils.MatrixExt.postRotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.videonatics.smartcapture.core.domain.model.AlignmentVector
import com.videonatics.smartcapture.core.domain.model.Detection
import com.videonatics.smartcapture.core.domain.model.PreprocessResult
import com.videonatics.smartcapture.feature.capture.presentation.view.DragMode
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.abs

fun ImageProxy.toBitmap(): Bitmap? {
    return try {
        imageProxyToBitmap(this)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
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

    val srcW = if (rotation == 90 || rotation == 270) image.height else image.width
    val srcH = if (rotation == 90 || rotation == 270) image.width else image.height

    val ratio = minOf(size.toFloat() / srcW, size.toFloat() / srcH)

    val newW = (srcW * ratio).toInt()
    val newH = (srcH * ratio).toInt()

    val dx = ((size - newW) / 2f)
    val dy = ((size - newH) / 2f)

    val buffer = ByteBuffer.allocateDirect(1 * size * size * 3 * 4)
    buffer.order(ByteOrder.nativeOrder())

    val total = size * size
    val pad = 114f

    val r = FloatArray(total) { pad }
    val g = FloatArray(total) { pad }
    val b = FloatArray(total) { pad }

    val planes = image.planes
    val yPlane = planes[0].buffer
    val uPlane = if (planes.size > 1) planes[1].buffer else null
    val vPlane = if (planes.size > 2) planes[2].buffer else null

    val yRowStride = planes[0].rowStride
    val uvRowStride = if (planes.size > 1) planes[1].rowStride else 0
    val uvPixelStride = if (planes.size > 1) planes[1].pixelStride else 0

    for (y in 0 until newH) {
        for (x in 0 until newW) {

            val srcXr = (x / ratio).toInt().coerceIn(0, srcW - 1)
            val srcYr = (y / ratio).toInt().coerceIn(0, srcH - 1)

            val (srcX, srcY) = when (rotation) {
                90 -> Pair(srcYr, image.height - srcXr - 1)
                180 -> Pair(image.width - srcXr - 1, image.height - srcYr - 1)
                270 -> Pair(image.width - srcYr - 1, srcXr)
                else -> Pair(srcXr, srcYr)
            }

            if (srcX !in 0 until image.width || srcY !in 0 until image.height) continue

            val yIndex = srcY * yRowStride + srcX
            val Y = (yPlane.get(yIndex).toInt() and 0xFF)
            
            var rVal: Float
            var gVal: Float
            var bVal: Float

            if (uPlane != null && vPlane != null) {
                val uvIndex = (srcY / 2) * uvRowStride + (srcX / 2) * uvPixelStride
                val U = (uPlane.get(uvIndex).toInt() and 0xFF)
                val V = (vPlane.get(uvIndex).toInt() and 0xFF)

                rVal = Y + 1.370705f * (V - 128)
                gVal = Y - 0.337633f * (U - 128) - 0.698001f * (V - 128)
                bVal = Y + 1.732446f * (U - 128)
            } else {
                rVal = Y.toFloat()
                gVal = Y.toFloat()
                bVal = Y.toFloat()
            }

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

    // NHWC for YOLO11 OBB as per script
    for (i in 0 until total) {
        buffer.putFloat(r[i] / 255f)
        buffer.putFloat(g[i] / 255f)
        buffer.putFloat(b[i] / 255f)
    }

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
    output: Array<Array<FloatArray>>,
    ratio: Float,
    dx: Float,
    dy: Float,
    confThreshold: Float = 0.25f,
    imgSize: Int = 416
): List<Detection> {
    val detections = mutableListOf<Detection>()
    // Output is (1, 6, 3549). We received it as Array(1) { Array(6) { FloatArray(3549) } }
    // but the input in the call was Array(1) { Array(3549) { FloatArray(9) } } - which was wrong.
    // Let's assume output is [6][3549] for simplicity in implementation.
    
    val numAnchors = output[0][0].size // 3549
    
    for (i in 0 until numAnchors) {
        val xc = output[0][0][i] * imgSize
        val yc = output[0][1][i] * imgSize
        val w = output[0][2][i] * imgSize
        val h = output[0][3][i] * imgSize
        val conf = output[0][4][i]
        val angle = output[0][5][i]

        if (conf < confThreshold) continue

        val x1 = ((xc - w / 2f) - dx) / ratio
        val y1 = ((yc - h / 2f) - dy) / ratio
        val x2 = ((xc + w / 2f) - dx) / ratio
        val y2 = ((yc + h / 2f) - dy) / ratio

        detections.add(Detection(RectF(x1, y1, x2, y2), conf, 0, angle = angle))
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
        90 -> RectF(rect.top, imageWidth - rect.right, rect.bottom, imageWidth - rect.left)
        180 -> RectF(imageWidth - rect.right, imageHeight - rect.bottom, imageWidth - rect.left, imageHeight - rect.top)
        270 -> RectF(imageHeight - rect.bottom, rect.left, imageHeight - rect.top, rect.right)
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

    return RectF(
        leftImg.coerceIn(0f, imageWidth),
        topImg.coerceIn(0f, imageHeight),
        rightImg.coerceIn(0f, imageWidth),
        bottomImg.coerceIn(0f, imageHeight)
    )
}

fun mergeToFittedBoxes(
    detections: List<Detection>,
    margin: Float = 5f
): List<Detection> {
    val remaining = detections.toMutableList()
    val result = mutableListOf<Detection>()

    fun isClose(a: RectF, b: RectF): Boolean {
        val expanded = RectF(a.left - margin, a.top - margin, a.right + margin, a.bottom + margin)
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
        var bestAngle = seed.angle
        var changed: Boolean

        do {
            changed = false
            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val other = iterator.next()
                if (other.classId != seed.classId) continue
                val currentRect = RectF(minLeft, minTop, maxRight, maxBottom)
                if (isClose(currentRect, other.rect)) {
                    minLeft = minOf(minLeft, other.rect.left)
                    minTop = minOf(minTop, other.rect.top)
                    maxRight = maxOf(maxRight, other.rect.right)
                    maxBottom = maxOf(maxBottom, other.rect.bottom)
                    if (other.score > bestScore) {
                        bestScore = other.score
                        bestColor = other.rectColor
                        bestAngle = other.angle
                    }
                    iterator.remove()
                    changed = true
                }
            }
        } while (changed)

        result.add(Detection(RectF(minLeft, minTop, maxRight, maxBottom), bestScore, seed.classId, bestColor, bestAngle))
    }
    return result
}

fun findBestBox(detections: List<Detection>, focusBox: RectF): Detection? {
    return detections.maxByOrNull { det ->
        val intersection = RectF().apply { setIntersect(det.rect, focusBox) }
        val overlapArea = intersection.width() * intersection.height()
        val detArea = det.rect.width() * det.rect.height()
        val overlapRatio = if (detArea > 0) overlapArea / detArea else 0f
        val centerDist = distance(det.rect.centerX(), det.rect.centerY(), focusBox.centerX(), focusBox.centerY())
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
    return listOf(horizontal, vertical).filter { it.isNotEmpty() }.joinToString(" & ").ifEmpty { "Centered" }
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
    val areaRatio = (best.rect.width() * best.rect.height()) / (focusBox.width() * focusBox.height())
    val distance = getDistanceHint(areaRatio)

    return when {
        direction == "Centered" && distance == "Good Distance" -> "Perfect Alignment ✅"
        direction == "Centered" -> distance
        else -> "$direction + $distance"
    }
}

fun generateAlignmentVector(best: Detection, focusBox: RectF): AlignmentVector {
    val dx = best.rect.centerX() - focusBox.centerX()
    val dy = best.rect.centerY() - focusBox.centerY()
    val h = dx / focusBox.width()
    val v = dy / focusBox.height()
    val areaRatio = (best.rect.width() * best.rect.height()) / (focusBox.width() * focusBox.height())
    val z = Math.log(areaRatio.toDouble()).toFloat()
    return AlignmentVector(h = h, v = v, z = z)
}

fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val planes = image.planes
    if (planes.isEmpty()) throw IllegalArgumentException("ImageProxy has no planes")

    val bitmap = if (image.format == ImageFormat.JPEG) {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } else if (planes.size >= 3) {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val nv21 = ByteArray(yBuffer.remaining() + uBuffer.remaining() + vBuffer.remaining())
        yBuffer.get(nv21, 0, yBuffer.remaining())
        vBuffer.get(nv21, yBuffer.remaining(), vBuffer.remaining())
        uBuffer.get(nv21, yBuffer.remaining() + vBuffer.remaining(), uBuffer.remaining())

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
        BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
    } else {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } ?: throw RuntimeException("Failed to decode bitmap")

    val rotationDegrees = image.imageInfo.rotationDegrees
    return if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}

fun ByteBuffer.toByteArray(): ByteArray {
    rewind()
    val arr = ByteArray(remaining())
    get(arr)
    return arr
}

fun loadFatBoxModel(context: Context): Interpreter {
    val file = context.assets.openFd("box_classifier.tflite")
    val input = FileInputStream(file.fileDescriptor)
    val channel = input.channel

    val mapped = channel.map(
        FileChannel.MapMode.READ_ONLY,
        file.startOffset,
        file.declaredLength
    )

    return Interpreter(mapped, Interpreter.Options().apply {
        setNumThreads(4)
    })
}

fun loadFatBoxLabels(context: Context): List<String> {
    return context.assets.open("fatbox_classes.txt")
        .bufferedReader()
        .readLines()
}
