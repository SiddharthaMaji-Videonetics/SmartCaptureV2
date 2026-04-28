package com.videonatics.smartcapture.core

import android.content.Context
import android.graphics.Bitmap
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

        // 2. Resize (match Python exactly)
        val resized = Bitmap.createScaledBitmap(
            gray,
            WIDTH * 2,
            HEIGHT * 2,
            true
        )

        // 3. Haar DWT
        val input = haarDWT(resized)

        // 4. Run model
        val output = Array(1) { FloatArray(2) }
        interpreter.run(input, output)

        // 5. Softmax
        val prob = softmax(output[0])[1]

        return prob // moiré probability
    }
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





