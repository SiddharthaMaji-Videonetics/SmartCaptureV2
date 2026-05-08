package com.videonatics.smartcapture.feature.analysis.data.repository

import com.videonatics.smartcapture.feature.analysis.data.datasource.remote.AnalysisRemoteDataSource
import com.videonatics.smartcapture.feature.analysis.data.model.AnalysisCheckpointEditRequest
import com.videonatics.smartcapture.feature.analysis.data.model.AnalysisOverrideRequest
import com.videonatics.smartcapture.feature.analysis.data.model.AnalysisResultData
import com.videonatics.smartcapture.feature.analysis.data.model.AnalysisValidateCheckRequest
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisAttempt
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisCheck
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisResult
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisStage
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisTaskDetail
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisValidatedCheck
import com.videonatics.smartcapture.feature.analysis.domain.model.OcrCandidate
import com.videonatics.smartcapture.feature.analysis.domain.repository.AnalysisRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject

class AnalysisRepositoryImpl @Inject constructor(
    private val remoteDataSource: AnalysisRemoteDataSource
) : AnalysisRepository {

    override suspend fun getTaskDetail(taskId: String): Result<AnalysisTaskDetail> =
        remoteDataSource.getTaskDetail(taskId).map { response ->
            val data = response.data ?: error("Empty task detail")
            AnalysisTaskDetail(
                id = data.id,
                assetId = data.assetId,
                status = data.status,
                stages = data.stages.map { stage ->
                    AnalysisStage(
                        id = stage.id,
                        stageType = stage.stageType ?: stage.name ?: "",
                        status = stage.status,
                        order = stage.order,
                        attempts = stage.attempts.map { attempt ->
                            AnalysisAttempt(
                                id = attempt.id,
                                attemptNo = attempt.attemptNo,
                                createdAt = attempt.createdAt,
                                guardrailPassed = attempt.guardrailResult?.passed,
                                imageUrl = attempt.imageCapture?.imageUrl?.takeIf { it.isNotEmpty() },
                                imageCaptureId = attempt.imageCapture?.id?.takeIf { it.isNotEmpty() }
                            )
                        },
                        latestAnalysis = stage.analysisResults.firstOrNull()?.toAnalysisResult(),
                        allAnalysisResults = stage.analysisResults.map { it.toAnalysisResult() },

                    )
                }
            )
        }

    private fun AnalysisResultData.toAnalysisResult(): AnalysisResult =
        AnalysisResult(
            id = id,
            passed = passed,
            attemptId = attemptId,
            checks = checks.map { check ->
                val dataMap = check.data.toStringMap()
                val editedDataMap = check.editedData.toStringMap()
                AnalysisCheck(
                    id = check.id,
                    checkName = check.checkName,
                    result = check.result,
                    editedResult = check.editedResult,
                    editNote = check.editNote,
                    confidence = check.confidence,
                    reason = check.reason,
                    correctiveAction = check.correctiveAction,
                    fixable = check.fixable,
                    data = dataMap,
                    editedData = editedDataMap,
                    ocrCandidates = check.data.toOcrCandidates(),
                    gateOverridden = dataMap["gateOverridden"] == "true",
                    carryForward = dataMap["validatedCarryForward"] == "true"
                )
            }
        )

    override suspend fun getValidatedChecks(taskId: String): Result<List<AnalysisValidatedCheck>> =
        remoteDataSource.getValidatedChecks(taskId).map { response ->
            response.data.orEmpty().map { vc ->
                AnalysisValidatedCheck(
                    id = vc.id,
                    checkName = vc.checkName,
                    result = vc.result,
                    confidence = vc.confidence,
                    source = vc.source
                )
            }
        }

    override suspend fun validateCheck(
        taskId: String,
        checkName: String,
        result: String,
        confidence: Double,
        validatedAtStageId: String,
        editedByOperator: Boolean
    ): Result<Unit> = remoteDataSource.validateCheck(
        taskId = taskId,
        checkName = checkName,
        body = AnalysisValidateCheckRequest(
            result = result,
            confidence = confidence,
            validatedAtStageId = validatedAtStageId,
            source = if (editedByOperator) "operator_edited" else "operator_confirmed_system"
        )
    )

    override suspend fun editCheckpoint(
        checkpointId: String,
        editedResult: String?,
        editNote: String?,
        editedData: Map<String, String>?
    ): Result<Unit> = remoteDataSource.editCheckpoint(
        checkpointId = checkpointId,
        body = AnalysisCheckpointEditRequest(
            editedResult = editedResult,
            editNote = editNote,
            editedData = editedData?.toJsonObject()
        )
    )

    override suspend fun revertCheckpoint(checkpointId: String): Result<Unit> =
        remoteDataSource.editCheckpoint(
            checkpointId = checkpointId,
            body = AnalysisCheckpointEditRequest(revert = true)
        )

    override suspend fun overrideGate(
        taskId: String,
        stageId: String,
        reason: String,
        reasonCode: String?
    ): Result<Unit> = remoteDataSource.overrideGate(
        taskId = taskId,
        body = AnalysisOverrideRequest(
            stageId = stageId,
            reason = reason,
            reasonCode = reasonCode
        )
    )
}

// ── JSON helpers ──────────────────────────────────────────────────────────────

private fun Map<String, String>.toJsonObject(): JsonObject = buildJsonObject {
    forEach { (k, v) -> put(k, v) }
}

private fun JsonElement?.toStringMap(): Map<String, String> {

    val jsonObject: JsonObject = when (this) {

        null -> return emptyMap()

        is JsonObject -> this

        // Handles JSON stored as string:
        // "{\"name\":\"test\",\"age\":23}"
        is JsonPrimitive -> {
            runCatching {
                Json.parseToJsonElement(content) as? JsonObject
            }.getOrNull() ?: return emptyMap()
        }

        else -> return emptyMap()
    }

    return buildMap {

        for ((k, v) in jsonObject.entries) {

            when (v) {

                is JsonPrimitive -> {
                    put(k, v.contentOrNull ?: v.toString())
                }

                is JsonArray -> {

                    val joined = v.joinToString(", ") { el ->

                        when (el) {

                            is JsonPrimitive ->
                                el.contentOrNull ?: el.toString()

                            is JsonObject ->
                                el.toString()

                            else ->
                                el.toString()
                        }
                    }

                    put(k, joined)
                }

                is JsonObject -> {
                    put(k, v.toString())
                }

                else -> {
                    put(k, v.toString())
                }
            }
        }
    }
}

private fun JsonElement?.toOcrCandidates(): List<OcrCandidate> {
    if (this == null || this !is JsonObject) return emptyList()
    val extracted = this["extractedNumbers"]
    if (extracted !is JsonArray || extracted.size < 2) return emptyList()
    return extracted.mapNotNull { item ->
        if (item !is JsonObject) return@mapNotNull null
        val text = (item["text"] as? JsonPrimitive)?.content?.takeIf { it.isNotEmpty() }
            ?: return@mapNotNull null
        val conf = (item["confidence"] as? JsonPrimitive)?.doubleOrNull ?: 0.0
        OcrCandidate(text, conf)
    }
}
