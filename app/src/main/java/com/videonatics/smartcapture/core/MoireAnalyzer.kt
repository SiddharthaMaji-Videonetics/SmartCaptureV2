package com.videonatics.smartcapture.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.Interpreter

class MoireAnalyzer(
    private val interpreter: Interpreter
) {

    private val WIDTH = 500
    private val HEIGHT = 375

    fun analyze(bitmap: Bitmap): Float {

        // 1. Grayscale
        val gray = toGrayscale(bitmap)

        // 2. Proper preprocessing (IMPORTANT)
        val processed = preprocessImg(gray)

        // 3. Haar DWT → shape [4, 375, 500]
        val input = haarDWT(processed)

        // 4. Model output MUST match [4, 2]
        val output = Array(4) { FloatArray(2) }

        interpreter.run(input, output)

        // 5. Aggregate predictions
        var total = 0f
        for (i in 0 until 4) {
            val prob = softmax(output[i])[1] // moire prob
            total += prob
        }

        return total / 4f
    }
}

fun preprocessImg(src: Bitmap): Bitmap {
    val WIDTH = 500
    val HEIGHT = 375

    // Python: img.convert("L")
    var bitmap = toGrayscale(src)

    val w = bitmap.width
    val h = bitmap.height

    // Python:
    // img.resize((1000,750) if w>=h else (750,1000))
    bitmap = if (w >= h) {
        Bitmap.createScaledBitmap(bitmap, 1000, 750, true)
    } else {
        Bitmap.createScaledBitmap(bitmap, 750, 1000, true)
    }

    // Python:
    // if h > w: rotate(-90)
    if (bitmap.height > bitmap.width) {
        val matrix = Matrix().apply { postRotate(-90f) }
        bitmap = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    // Python:
    // img.resize((WIDTH*2, HEIGHT*2)) → (1000, 750)
    bitmap = Bitmap.createScaledBitmap(
        bitmap,
        WIDTH * 2,
        HEIGHT * 2,
        true
    )

    return bitmap
}

private fun softmax(logits: FloatArray): FloatArray {
    val max = logits.maxOrNull() ?: 0f

    val exps = FloatArray(logits.size)
    var sum = 0f

    for (i in logits.indices) {
        exps[i] = kotlin.math.exp((logits[i] - max).toDouble()).toFloat()
        sum += exps[i]
    }

    for (i in exps.indices) {
        exps[i] /= sum
    }

    return exps
}
