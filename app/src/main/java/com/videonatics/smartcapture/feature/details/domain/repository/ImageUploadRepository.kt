package com.videonatics.smartcapture.feature.details.domain.repository

import android.graphics.Bitmap
import com.videonatics.smartcapture.feature.details.FatBoxClassifier
import com.videonatics.smartcapture.feature.details.data.model.ImageQualityResult
import com.videonatics.smartcapture.feature.details.data.model.ImageUploadResponse
import kotlinx.coroutines.flow.Flow

interface ImageUploadRepository {
    suspend fun uploadImage(
        stageId: String,
        imageBytes: ByteArray,
        onProgress: (Float) -> Unit = {}
    ): Result<ImageUploadResponse>

    suspend fun analyzeImageQuality(bitmap: Bitmap): Result<ImageQualityResult>
    suspend fun classifyImage(bitmap: Bitmap): Result<FatBoxClassifier.Result>
}
