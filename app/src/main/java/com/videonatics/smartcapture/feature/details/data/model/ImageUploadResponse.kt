package com.videonatics.smartcapture.feature.details.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ImageUploadResponse(
    val success: Boolean,
    val data: ImageUploadData
)

@Serializable
data class ImageUploadData(
    val attemptId: String,
    val attemptNo: Int,
    val imageCaptureId: String,
    val queued: Boolean
)
