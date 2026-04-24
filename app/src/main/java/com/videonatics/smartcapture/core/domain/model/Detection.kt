package com.videonatics.smartcapture.core.domain.model

import android.graphics.PointF
import android.graphics.RectF

data class Detection(
    val corners: List<PointF>,   // 4 corners (OBB)
    val score: Float,
    val classId: Int,
    val angle: Float = 0f,
    var color: Long = 0xFFFF3535
)