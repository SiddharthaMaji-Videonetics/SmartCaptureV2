package com.videonatics.smartcapture.feature.analysis.domain.model

data class OcrCandidate(val text: String, val confidence: Double)

data class AnalysisCheck(
    val id: String,
    val checkName: String,
    val result: String,
    val editedResult: String? = null,
    val editNote: String? = null,
    val confidence: Double = 0.0,
    val reason: String = "",
    val correctiveAction: String? = null,
    val fixable: Boolean = true,
    // Structured per-check payload (varies per checkName — box_status,
    // port_count, fat_ne_id_ocr, etc.). Keys are always strings; values
    // are pre-stringified for display.
    val data: Map<String, String> = emptyMap(),
    val editedData: Map<String, String> = emptyMap(),
    val ocrCandidates: List<OcrCandidate> = emptyList(),
    val gateOverridden: Boolean = false,
    val carryForward: Boolean = false
) {
    val effectiveResult: String get() = editedResult ?: result
    val isEdited: Boolean get() = editedResult != null || editedData.isNotEmpty()
    val effectiveData: Map<String, String> get() = editedData.ifEmpty { data }
}

data class AnalysisResult(
    val id: String,
    val passed: Boolean,
    val attemptId: String?,
    val checks: List<AnalysisCheck>
)

data class AnalysisAttempt(
    val id: String,
    val attemptNo: Int,
    val createdAt: String,
    val guardrailPassed: Boolean?,
    val imageUrl: String? = null,
    val imageCaptureId: String? = null
)

data class AnalysisStage(
    val id: String,
    val stageType: String,
    val status: String,
    val order: Int = 0,
    val attempts: List<AnalysisAttempt> = emptyList(),
    val latestAnalysis: AnalysisResult? = null,
    val allAnalysisResults: List<AnalysisResult> = emptyList()
) {
    val attemptCount: Int get() = attempts.size
    fun analysisForAttempt(attemptId: String): AnalysisResult? =
        allAnalysisResults.firstOrNull { it.attemptId == attemptId }
}

data class AnalysisTaskDetail(
    val id: String,
    val assetId: String = "",
    val status: String,
    val stages: List<AnalysisStage> = emptyList()
)

data class AnalysisValidatedCheck(
    val id: String?,
    val checkName: String,
    val result: String,
    val confidence: Double?,
    val source: String?
)