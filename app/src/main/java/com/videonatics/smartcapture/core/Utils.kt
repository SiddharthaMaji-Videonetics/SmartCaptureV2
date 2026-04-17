package com.videonatics.smartcapture.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import com.videonatics.smartcapture.core.domain.model.Detection
import com.videonatics.smartcapture.core.domain.model.PreprocessResult
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

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
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 80, out)

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

fun preprocess(bitmap: Bitmap, size: Int = 416): PreprocessResult {

    val buffer = ByteBuffer.allocateDirect(1 * 3 * size * size * 4)
    buffer.order(ByteOrder.nativeOrder())

    val origW = bitmap.width
    val origH = bitmap.height

    val ratio = minOf(size.toFloat() / origW, size.toFloat() / origH)

    val newW = (origW * ratio).toInt()
    val newH = (origH * ratio).toInt()

    val resized = Bitmap.createScaledBitmap(bitmap, newW, newH, true)

    val dx = (size - newW) / 2f
    val dy = (size - newH) / 2f

    val padded = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(padded)
    canvas.drawColor(Color.rgb(114, 114, 114))
    canvas.drawBitmap(resized, dx, dy, null)

    val pixels = IntArray(size * size)
    padded.getPixels(pixels, 0, size, 0, 0, size, size)

    val b = FloatArray(size * size)
    val g = FloatArray(size * size)
    val r = FloatArray(size * size)

    for (i in pixels.indices) {
        val px = pixels[i]

        // LEGACY = FALSE → BGR, NO normalization
        b[i] = Color.blue(px).toFloat()
        g[i] = Color.green(px).toFloat()
        r[i] = Color.red(px).toFloat()
    }

    // CHW
    b.forEach { buffer.putFloat(it) }
    g.forEach { buffer.putFloat(it) }
    r.forEach { buffer.putFloat(it) }

    buffer.rewind()

    return PreprocessResult(buffer, ratio, dx, dy, padded)
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

fun RectF.toNormalized(
    imageWidth: Float,
    imageHeight: Float
): RectF {
    return RectF(
        this.left / imageWidth,
        this.top / imageHeight,
        this.right / imageWidth,
        this.bottom / imageHeight
    )
}