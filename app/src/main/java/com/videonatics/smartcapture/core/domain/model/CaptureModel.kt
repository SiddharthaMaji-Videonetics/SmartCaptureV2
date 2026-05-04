package com.videonatics.smartcapture.core.domain.model

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF

data class CaptureModel(
    val id: String,
    val image: Bitmap,
    val timestamp: Long = System.currentTimeMillis(),
    val boundBox: RectF? = null,
    val obbCorners: List<PointF>? = null,
    val rotation: Float = 0f,
    val hashCode: String = id.split("-").last().uppercase()
)
