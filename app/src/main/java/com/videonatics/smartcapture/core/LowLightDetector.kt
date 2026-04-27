package com.videonatics.smartcapture.core

class LowLightDetector {

    private val window = ArrayDeque<Float>()
    private val windowSize = 8

    private var lastResult = false

    fun update(luma: Float): Boolean {

        // smoothing
        window.addLast(luma)
        if (window.size > windowSize) window.removeFirst()

        val avg = window.average().toFloat()

        val isDark = avg < 50f

        // hysteresis (VERY IMPORTANT)
        lastResult = when {
            lastResult && avg > 65f -> false
            !lastResult && avg < 50f -> true
            else -> lastResult
        }

        return lastResult
    }
}