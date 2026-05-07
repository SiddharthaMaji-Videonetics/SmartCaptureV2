package com.videonatics.smartcapture.feature.analysis.data.repository

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
import javax.inject.Inject

class AnalysisRepositoryImpl @Inject constructor(
    private val remoteDataSource: AnalysisRemoteDataSource
) : AnalysisRepository {

    override suspend fun getTaskDetail(taskId: String): Result<AnalysisTaskDetail> =
        remoteDataSource.getTaskDetail(taskId).map { response ->
            val data = response.data ?: error("Empty task detail")
            AnalysisTaskDetail(
                id = data.id,
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
                                guardrailPassed = attempt.guardrailResult?.passed
                            )
                        },
                        latestAnalysis = stage.analysisResults.firstOrNull()?.let { ar ->
                            AnalysisResult(
                                id = ar.id,
                                passed = ar.passed,
                                attemptId = ar.attemptId,
                                checks = ar.checks.map { check ->
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
                        }
                    )
                }
            )
        }

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
        editNote: String?
    ): Result<Unit> = remoteDataSource.editCheckpoint(
        checkpointId = checkpointId,
        body = AnalysisCheckpointEditRequest(
            editedResult = editedResult,
            editNote = editNote
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

private fun JsonElement?.toStringMap(): Map<String, String> {
    if (this == null || this !is JsonObject) return emptyMap()
    return buildMap {
        for ((k, v) in entries) {
            when (v) {
                is JsonPrimitive -> put(k, v.content)
                is JsonArray -> {
                    val joined = v.joinToString(", ") { el ->
                        when (el) {
                            is JsonPrimitive -> el.content
                            is JsonObject -> (el["text"] as? JsonPrimitive)?.content ?: el.toString()
                            else -> el.toString()
                        }
                    }
                    put(k, joined)
                }
                is JsonObject -> put(k, v.toString())
                else -> Unit
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