package com.videonatics.smartcapture.feature.analysis.domain.repository

import com.videonatics.smartcapture.feature.analysis.data.datasource.remote.AnalysisRemoteDataSource
import com.videonatics.smartcapture.feature.analysis.data.model.AnalysisCheckpointEditRequest
import com.videonatics.smartcapture.feature.analysis.data.model.AnalysisOverrideRequest
import com.videonatics.smartcapture.feature.analysis.data.model.AnalysisValidateCheckRequest
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisAttempt
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisCheck
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisResult
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisStage
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisTaskDetail
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisValidatedCheck
import com.videonatics.smartcapture.feature.analysis.domain.model.OcrCandidate
import com.videonatics.smartcapture.feature.analysis.domain.repository.AnalysisRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject

interface AnalysisRepository {

     suspend fun getTaskDetail(taskId: String): Result<AnalysisTaskDetail>
     suspend fun getValidatedChecks(taskId: String): Result<List<AnalysisValidatedCheck>>
     suspend fun validateCheck(
        taskId: String,
        checkName: String,
        result: String,
        confidence: Double,
        validatedAtStageId: String,
        editedByOperator: Boolean
    ): Result<Unit>

     suspend fun editCheckpoint(
        checkpointId: String,
        editedResult: String?,
        editNote: String?,
        editedData: Map<String, String>? = null
    ): Result<Unit>

     suspend fun revertCheckpoint(checkpointId: String): Result<Unit>

     suspend fun overrideGate(
        taskId: String,
        stageId: String,
        reason: String,
        reasonCode: String?
    ): Result<Unit>
}