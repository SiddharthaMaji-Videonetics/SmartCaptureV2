package com.videonatics.smartcapture.core.domain.model

data class FrameData(
    val y: ByteArray,
    val u: ByteArray,
    val v: ByteArray,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val yRowStride: Int,
    val uvRowStride: Int,
    val uvPixelStride: Int
)