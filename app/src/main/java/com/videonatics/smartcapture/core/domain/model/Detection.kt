package com.videonatics.smartcapture.core.domain.model

import android.graphics.RectF

data class Detection(
    val rect: RectF,
    val score: Float,
    val classId: Int,
    val rectColor: Long = 0XFFff3535
)