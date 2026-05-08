package com.videonatics.smartcapture.feature.analysis.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class AnalysisTaskDetailResponse(
    val success: Boolean = false,
    val data: AnalysisTaskDetailData? = null
)

@Serializable
data class AnalysisTaskDetailData(
    val id: String,
    val assetId: String = "",
    val status: String = "",
    val stages: List<AnalysisStageData> = emptyList()
)

@Serializable
data class AnalysisStageData(
    val id: String,
    val stageType: String? = null,
    val name: String? = null,
    val status: String = "",
    val order: Int = 0,
    val attempts: List<AnalysisAttemptData> = emptyList(),
    val analysisResults: List<AnalysisResultData> = emptyList()
)

@Serializable
data class AnalysisAttemptData(
    val id: String = "",
    val attemptNo: Int = 0,
    val createdAt: String = "",
    val guardrailResult: AnalysisGuardrailData? = null,
    val imageCapture: AnalysisAttemptImageData? = null
)

@Serializable
data class AnalysisAttemptImageData(
    val id: String = "",
    val imageUrl: String = ""
)

@Serializable
data class AnalysisGuardrailData(
    val passed: Boolean = false
)

// Backend returns analysisResults ordered by createdAt: desc
// so first element is the latest analysis.
@Serializable
data class AnalysisResultData(
    val id: String = "",
    val passed: Boolean = false,
    val attemptId: String? = null,
    val checks: List<AnalysisCheckData> = emptyList()
)

@Serializable
data class AnalysisCheckData(
    val id: String = "",
    val checkName: String = "",
    val result: String = "",
    val editedResult: String? = null,
    val editNote: String? = null,
    val confidence: Double = 0.0,
    val reason: String = "",
    val correctiveAction: String? = null,
    val fixable: Boolean = true,
    val data: JsonElement? = null,
    val editedData: JsonElement? = null
)

// Validated checks response
@Serializable
data class AnalysisValidatedChecksResponse(
    val success: Boolean = false,
    val data: List<AnalysisValidatedCheckData>? = null
)

@Serializable
data class AnalysisValidatedCheckData(
    val id: String? = null,
    val checkName: String = "",
    val result: String = "",
    val confidence: Double? = null,
    val source: String? = null,
    val createdAt: String? = null
)

// Request bodies
@Serializable
data class AnalysisValidateCheckRequest(
    val result: String,
    val confidence: Double,
    val validatedAtStageId: String? = null,
    val source: String
)

@Serializable
data class AnalysisCheckpointEditRequest(
    val editedResult: String? = null,
    val editNote: String? = null,
    val editedData: JsonObject? = null,
    val revert: Boolean? = null
)

// POST /api/v1/reviewer/tasks/{taskId}/override-requests
@Serializable
data class AnalysisOverrideRequest(
    val stageId: String,
    val reason: String,
    val reasonCode: String? = null,
    val otherText: String? = null
)