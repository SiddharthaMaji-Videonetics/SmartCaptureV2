package com.videonatics.smartcapture.feature.details

import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FatBoxClassifier(
    private val interpreter: Interpreter,
    private val labels: List<String>
) {

    companion object {
        private const val INPUT_SIZE = 224
        private const val RESIZE_SIZE = 257

        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD  = floatArrayOf(0.229f, 0.224f, 0.225f)
    }

    fun classify(bitmap: Bitmap): Result {
        val processed = preprocess(bitmap)

        val output = Array(1) { FloatArray(labels.size) }

        interpreter.run(processed, output)

        val probs = softmax(output[0])

        val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: -1

        // Mock quality logic for now - this should ideally come from another model or image processing
        val quality = estimateQuality(bitmap)

        return Result(
            label = labels.getOrElse(maxIdx) { "Unknown" },
            confidence = probs.getOrElse(maxIdx) { 0f },
            probabilities = probs,
            quality = quality
        )
    }

    private fun estimateQuality(bitmap: Bitmap): String {
        // Simple mock logic for quality flags based on image properties or random for UI testing
        // In real app, this would use a Blur detector and Brightness detector
        return "good" 
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {

        val cropped = resizeAndCrop(bitmap)

        val buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        cropped.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF)
            val g = (pixel shr 8 and 0xFF)
            val b = (pixel and 0xFF)

            // Normalize EXACTLY like Python
            buffer.putFloat((r / 255f - MEAN[0]) / STD[0])
            buffer.putFloat((g / 255f - MEAN[1]) / STD[1])
            buffer.putFloat((b / 255f - MEAN[2]) / STD[2])
        }

        buffer.rewind()
        return buffer
    }

    // ─────────────────────────────────────────────
    // RESIZE + CENTER CROP (257 → 224)
    // ─────────────────────────────────────────────
    private fun resizeAndCrop(bitmap: Bitmap): Bitmap {
        val resized = Bitmap.createScaledBitmap(
            bitmap,
            RESIZE_SIZE,
            RESIZE_SIZE,
            true
        )

        val left = (RESIZE_SIZE - INPUT_SIZE) / 2
        val top = (RESIZE_SIZE - INPUT_SIZE) / 2

        return Bitmap.createBitmap(
            resized,
            left,
            top,
            INPUT_SIZE,
            INPUT_SIZE
        )
    }

    // ─────────────────────────────────────────────
    // SOFTMAX (same as backend)
    // ─────────────────────────────────────────────
    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f

        val exp = logits.map { kotlin.math.exp((it - max).toDouble()) }
        val sum = exp.sum()

        return exp.map { (it / sum).toFloat() }.toFloatArray()
    }

    data class Result(
        val label: String,
        val confidence: Float,
        val probabilities: FloatArray,
        val quality: String = "good"
    )
}
