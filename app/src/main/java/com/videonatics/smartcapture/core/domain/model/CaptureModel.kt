package com.videonatics.smartcapture.core.domain.model

import android.graphics.Bitmap
import android.graphics.RectF

data class CaptureModel(
    val id: String,
    val image: Bitmap,
    val timestamp: Long = System.currentTimeMillis(),
    val boundBox: RectF? = null,
    val hashCode: String = id.split("-").last().uppercase()
)
