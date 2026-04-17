package com.videonatics.smartcapture.core.domain.model

import android.graphics.Bitmap
import java.nio.ByteBuffer

data class PreprocessResult(
    val buffer: ByteBuffer,
    val ratio: Float,
    val dx: Float,
    val dy: Float,
    val paddedBitmap: Bitmap
)