package com.videonatics.smartcapture.feature.details.data.model

data class ImageQualityResult(
    val fileName: String? = null,

    val laplacianVar: Double = 0.0,
    val labLMean: Double = 0.0,
    val darkPixelPct: Double = 0.0,

    val isBlurry: Boolean = false,
    val isUnderlit: Boolean = false,

    val error: String? = null
) {
    val hasIssue: Boolean
        get() = isBlurry || isUnderlit || error != null

    val issues: List<String>
        get() = buildList {
            if (isBlurry) add("BLURRY")
            if (isUnderlit) add("UNDERLIT")
            if (error != null) add("ERROR")
        }
}