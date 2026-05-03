package com.videonatics.smartcapture.feature.capturelist.domain.repository

import com.videonatics.smartcapture.core.domain.model.CaptureModel
import kotlinx.coroutines.flow.Flow

interface CaptureRepository {
    fun getAllCaptures(): Flow<List<CaptureModel>>
    suspend fun saveCapture(capture: CaptureModel)
    suspend fun deleteCapture(id: String)
}
