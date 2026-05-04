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

        // 1. Resize FIRST (CRITICAL)
        val resized = Bitmap.createScaledBitmap(bitmap, 1000, 750, true)

        // 2. Grayscale
        val gray = toGrayscale(resized)

        // 3. Preprocess (if needed)
        val processed = preprocessImg(gray)

        // 4. Haar DWT → [4, 375, 500]
        val dwt = haarDWT(processed)

        // 5. Add batch → [1, 4, 375, 500]
        val input = arrayOf(dwt)

        //Correct output shape
        val output = Array(1) { FloatArray(2) }

        interpreter.run(input, output)

        // 6. Softmax (since backend applies it AFTER)
        val probs = softmax(output[0])

        // 7. Return moiré probability
        return probs[1]
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
