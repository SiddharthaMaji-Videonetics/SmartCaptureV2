package com.videonatics.smartcapture.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.PointF
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
    output: Array<Array<FloatArray>>, // (1, 6, N)
    ratio: Float,
    dx: Float,
    dy: Float,
    confThreshold: Float = 0.18f,
    iouThreshold: Float = 0.35f,
    imgSize: Int = 416
): List<Detection> {

    val detections = mutableListOf<Detection>()
    val numAnchors = output[0][0].size

    val minSize = 20f
    val edgeMargin = 5f

    for (i in 0 until numAnchors) {

        val conf = output[0][4][i]
        if (conf < confThreshold) continue

        // De-normalize
        val xc = output[0][0][i] * imgSize
        val yc = output[0][1][i] * imgSize
        val w = output[0][2][i] * imgSize
        val h = output[0][3][i] * imgSize
        val angle = output[0][5][i]

        // --- 🔥 Noise Filters ---

        // 1. Size filter
        if (w < minSize || h < minSize) continue

        // 2. Aspect ratio filter
        val aspect = w / h
        if (aspect < 0.3f || aspect > 3.0f) continue

        // 3. Edge filter
        if (xc < edgeMargin || yc < edgeMargin ||
            xc > imgSize - edgeMargin || yc > imgSize - edgeMargin
        ) continue

        // --- Convert to original image space ---
        val xcO = (xc - dx) / ratio
        val ycO = (yc - dy) / ratio
        val wO = w / ratio
        val hO = h / ratio

        // --- OBB corners ---
        val corners = obbToCorners(xcO, ycO, wO, hO, angle)

        detections.add(
            Detection(
                corners = corners,
                score = conf,
                classId = 0,
                angle = angle
            )
        )
    }

    // --- NMS ---
    val finalDetections = nms(detections, iouThreshold)

    // --- Keep top-K (extra noise control) ---
    return finalDetections
        .sortedByDescending { it.score }
        .take(10)
}




fun cornersToRect(corners: List<PointF>): RectF {
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE

    for (p in corners) {
        if (p.x < minX) minX = p.x
        if (p.y < minY) minY = p.y
        if (p.x > maxX) maxX = p.x
        if (p.y > maxY) maxY = p.y
    }

    return RectF(minX, minY, maxX, maxY)
}

fun nms(
    detections: List<Detection>,
    iouThreshold: Float,
    maxDet: Int = 300
): List<Detection> {

    if (detections.isEmpty()) return emptyList()

    val sorted = detections.sortedByDescending { it.score }.toMutableList()
    val result = mutableListOf<Detection>()

    val rectCache = sorted.map { cornersToRect(it.corners) }

    while (sorted.isNotEmpty() && result.size < maxDet) {

        val best = sorted.removeAt(0)
        val bestRect = cornersToRect(best.corners)

        result.add(best)

        val iterator = sorted.listIterator()
        var index = 0

        while (iterator.hasNext()) {
            val other = iterator.next()

            val overlap = iou(bestRect, rectCache[index + 1])
            if (overlap > iouThreshold) {
                iterator.remove()
            }
            index++
        }
    }

    return result
}

fun iou(a: RectF, b: RectF): Float {
    val left = maxOf(a.left, b.left)
    val top = maxOf(a.top, b.top)
    val right = minOf(a.right, b.right)
    val bottom = minOf(a.bottom, b.bottom)

    val inter = maxOf(0f, right - left) * maxOf(0f, bottom - top)
    val union = a.width() * a.height() + b.width() * b.height() - inter

    return if (union > 0) inter / union else 0f
}
fun obbToCorners(
    xc: Float,
    yc: Float,
    w: Float,
    h: Float,
    angle: Float
): List<PointF> {

    val cosA = kotlin.math.cos(angle)
    val sinA = kotlin.math.sin(angle)

    val hw = w / 2f
    val hh = h / 2f

    val pts = arrayOf(
        floatArrayOf(-hw, -hh),
        floatArrayOf(hw, -hh),
        floatArrayOf(hw, hh),
        floatArrayOf(-hw, hh)
    )

    val result = ArrayList<PointF>(4)

    for (p in pts) {
        val x = p[0] * cosA - p[1] * sinA + xc
        val y = p[0] * sinA + p[1] * cosA + yc
        result.add(PointF(x, y))
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

fun Detection.toRect(): RectF {
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE

    for (p in corners) {
        if (p.x < minX) minX = p.x
        if (p.y < minY) minY = p.y
        if (p.x > maxX) maxX = p.x
        if (p.y > maxY) maxY = p.y
    }

    return RectF(minX, minY, maxX, maxY)
}

fun mergeToFittedBoxes(
    detections: List<Detection>,
    margin: Float = 5f
): List<Detection> {

    val remaining = detections.toMutableList()
    val result = mutableListOf<Detection>()

    fun toRect(corners: List<PointF>): RectF {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (p in corners) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
        return RectF(minX, minY, maxX, maxY)
    }

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

        var mergedRect = toRect(seed.corners)

        var bestScore = seed.score
        var bestColor = seed.color
        var bestAngle = seed.angle
        var bestCorners = seed.corners

        var changed: Boolean

        do {
            changed = false
            val iterator = remaining.iterator()

            while (iterator.hasNext()) {
                val other = iterator.next()
                if (other.classId != seed.classId) continue

                val otherRect = toRect(other.corners)

                if (isClose(mergedRect, otherRect)) {

                    // Expand merged rect
                    mergedRect = RectF(
                        minOf(mergedRect.left, otherRect.left),
                        minOf(mergedRect.top, otherRect.top),
                        maxOf(mergedRect.right, otherRect.right),
                        maxOf(mergedRect.bottom, otherRect.bottom)
                    )

                    // Keep best scoring detection (for angle + corners)
                    if (other.score > bestScore) {
                        bestScore = other.score
                        bestColor = other.color
                        bestAngle = other.angle
                        bestCorners = other.corners
                    }

                    iterator.remove()
                    changed = true
                }
            }

        } while (changed)

        // Final detection: keep best OBB but merged score/color
        result.add(
            Detection(
                corners = bestCorners,
                score = bestScore,
                classId = seed.classId,
                angle = bestAngle,
                color = bestColor
            )
        )
    }

    return result
}

fun findBestBox(
    detections: List<Detection>,
    focusBox: RectF
): Detection? {

    fun toRect(corners: List<PointF>): RectF {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (p in corners) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }

        return RectF(minX, minY, maxX, maxY)
    }

    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return kotlin.math.hypot(x1 - x2, y1 - y2)
    }

    return detections.maxByOrNull { det ->

        val rect = toRect(det.corners)

        val intersection = RectF()
        val hasIntersection = intersection.setIntersect(rect, focusBox)

        val overlapArea = if (hasIntersection) {
            intersection.width() * intersection.height()
        } else 0f

        val detArea = rect.width() * rect.height()

        val overlapRatio = if (detArea > 0) overlapArea / detArea else 0f

        val centerDist = distance(
            rect.centerX(),
            rect.centerY(),
            focusBox.centerX(),
            focusBox.centerY()
        )

        // Same scoring logic as yours
        overlapRatio * 2f - centerDist * 0.001f
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

fun generateInstruction(
    best: Detection,
    focusBox: RectF
): String {

    fun toRect(corners: List<PointF>): RectF {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (p in corners) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }

        return RectF(minX, minY, maxX, maxY)
    }

    val rect = toRect(best.corners)

    val dx = rect.centerX() - focusBox.centerX()
    val dy = rect.centerY() - focusBox.centerY()

    val direction = getDirection(dx, dy, threshold = 20f)

    val areaRatio =
        (rect.width() * rect.height()) /
                (focusBox.width() * focusBox.height())

    val distance = getDistanceHint(areaRatio)

    return when {
        direction == "Centered" && distance == "Good Distance" -> "Perfect Alignment"
        direction == "Centered" -> distance
        else -> "$direction + $distance"
    }
}

fun generateAlignmentVector(
    best: Detection,
    focusBox: RectF
): AlignmentVector {

    fun toRect(corners: List<PointF>): RectF {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (p in corners) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }

        return RectF(minX, minY, maxX, maxY)
    }

    val rect = toRect(best.corners)

    val dx = rect.centerX() - focusBox.centerX()
    val dy = rect.centerY() - focusBox.centerY()

    val h = dx / focusBox.width()
    val v = dy / focusBox.height()

    val areaRatio =
        (rect.width() * rect.height()) /
                (focusBox.width() * focusBox.height())

    val z = kotlin.math.ln(areaRatio)   // same as log()

    return AlignmentVector(
        h = h,
        v = v,
        z = z
    )
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
    return if (rotationDegrees != 0 && bitmap.width > 0 && bitmap.height > 0) {
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

fun List<RectF>.filterInside(focus: RectF): List<RectF> {
    return filter { RectF.intersects(it, focus) }
}

fun mapRectToBitmapSpaceWithLetterbox(
    rect: RectF,              // model output (input size space)
    inputSize: Int,           // e.g. 416 or 640
    bitmap: Bitmap
): RectF {

    val bw = bitmap.width.toFloat()
    val bh = bitmap.height.toFloat()

    // 1. Compute scale used during resize (FIT)
    val scale = minOf(inputSize / bw, inputSize / bh)

    val scaledW = bw * scale
    val scaledH = bh * scale

    // 2. Compute padding (letterbox)
    val dx = (inputSize - scaledW) / 2f
    val dy = (inputSize - scaledH) / 2f

    // 3. Remove padding from rect
    val left = (rect.left - dx) / scale
    val top = (rect.top - dy) / scale
    val right = (rect.right - dx) / scale
    val bottom = (rect.bottom - dy) / scale

    return RectF(left, top, right, bottom)
}

fun cropBitmap(bitmap: Bitmap, rect: RectF): Bitmap {
    if (bitmap.width <= 0 || bitmap.height <= 0) return bitmap

    val x = rect.left.toInt().coerceIn(0, bitmap.width - 1)
    val y = rect.top.toInt().coerceIn(0, bitmap.height - 1)
    val width = rect.width().toInt().coerceIn(1, bitmap.width - x)
    val height = rect.height().toInt().coerceIn(1, bitmap.height - y)

    return Bitmap.createBitmap(bitmap, x, y, width, height)
}

fun cropBitmapFromNormalizedRect(
    bitmap: Bitmap,
    normalizedRect: RectF
): Bitmap? {

    // 1. Fix inverted rect (very important)
    val leftN = minOf(normalizedRect.left, normalizedRect.right)
    val rightN = maxOf(normalizedRect.left, normalizedRect.right)
    val topN = minOf(normalizedRect.top, normalizedRect.bottom)
    val bottomN = maxOf(normalizedRect.top, normalizedRect.bottom)

    // 2. Clamp to [0,1]
    val left = leftN.coerceIn(0f, 1f)
    val top = topN.coerceIn(0f, 1f)
    val right = rightN.coerceIn(0f, 1f)
    val bottom = bottomN.coerceIn(0f, 1f)

    // 3. Convert to pixel space
    val x = (left * bitmap.width).toInt()
    val y = (top * bitmap.height).toInt()
    val w = ((right - left) * bitmap.width).toInt()
    val h = ((bottom - top) * bitmap.height).toInt()

    // 4. Reject invalid crops
    if (w <= 1 || h <= 1) {
        return null // <-- THIS is likely happening in your case
    }

    // 5. Clamp safely
    val safeW = (x + w).coerceAtMost(bitmap.width) - x
    val safeH = (y + h).coerceAtMost(bitmap.height) - y

    if (safeW <= 0 || safeH <= 0) return null

    return Bitmap.createBitmap(bitmap, x, y, safeW, safeH)
}