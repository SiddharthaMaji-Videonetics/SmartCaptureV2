package com.videonatics.smartcapture.feature.details.data.model

data class ImageQualityConfig(
    val blurThreshold: Double = 400.0,
    val lowLightThreshold: Double = 50.0,
    val darkPixelValue: Int = 50,
    val darkPctThreshold: Double = 35.0
)