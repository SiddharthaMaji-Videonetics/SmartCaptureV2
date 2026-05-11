package com.videonatics.smartcapture.feature.analysis.presenation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisAttempt
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisCheck
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisValidatedCheck
import com.videonatics.smartcapture.feature.analysis.domain.repository.AnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalysisUiState(
    val isLoading: Boolean = true,
    val assetId: String = "",
    val taskStatus: String = "",
    val stageType: String = "",
    val stageStatus: String = "",
    val attempts: List<AnalysisAttempt> = emptyList(),
    val latestImageUrl: String? = null,
    val checks: List<AnalysisCheck> = emptyList(),
    val hasAnalysis: Boolean = false,
    val latestAnalysisPassed: Boolean = false,
    val validatedChecks: List<AnalysisValidatedCheck> = emptyList(),
    val showOverrideDialog: Boolean = false,
    val overrideReason: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null
) {
    val attemptCount: Int get() = attempts.size
}

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: AnalysisRepository
) : ViewModel() {

    val taskId: String = checkNotNull(savedState["taskId"])
    val stageId: String = checkNotNull(savedState["stageId"])

    private val _state = MutableStateFlow(AnalysisUiState())
    val state: StateFlow<AnalysisUiState> = _state.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        try {
            coroutineScope {
                val taskDetailDeferred = async { repository.getTaskDetail(taskId) }
                val validatedChecksDeferred = async { repository.getValidatedChecks(taskId) }

                val taskDetailResult = taskDetailDeferred.await()
                val validatedChecksResult = validatedChecksDeferred.await()

                taskDetailResult.fold(
                    onSuccess = { detail ->
                        val stage = detail.stages.find { it.id == stageId }
                            ?: detail.stages.lastOrNull()
                        val attempts = stage?.attempts ?: emptyList()
                        val latestImageUrl = attempts.maxByOrNull { it.attemptNo }?.imageUrl
                        _state.update { s ->
                            s.copy(
                                isLoading = false,
                                assetId = detail.assetId,
                                taskStatus = detail.status,
                                stageType = stage?.stageType.orEmpty(),
                                stageStatus = stage?.status.orEmpty(),
                                attempts = attempts,
                                latestImageUrl = latestImageUrl,
                                checks = stage?.latestAnalysis?.checks ?: emptyList(),
                                hasAnalysis = stage?.latestAnalysis != null,
                                latestAnalysisPassed = stage?.latestAnalysis?.passed ?: false,
                                validatedChecks = validatedChecksResult.getOrDefault(emptyList())
                            )
                        }
                    },
                    onFailure = { e ->
                        _state.update { it.copy(isLoading = false, error = e.message) }
                    }
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    fun validate(
        checkName: String,
        result: String,
        editedByOperator: Boolean = false
    ) = viewModelScope.launch {
        _state.update { it.copy(isSubmitting = true) }
        repository.validateCheck(
            taskId = taskId,
            checkName = checkName,
            result = result,
            confidence = 1.0,
            validatedAtStageId = stageId,
            editedByOperator = editedByOperator
        ).onSuccess {
            refresh()
            _events.send("Confirmed")
        }.onFailure {
            _events.send(it.message ?: "Confirm failed")
        }
        _state.update { it.copy(isSubmitting = false) }
    }

    fun validateAll() = viewModelScope.launch {
        val unvalidated = _state.value.checks.filter { !it.carryForward }
        if (unvalidated.isEmpty()) return@launch

        _state.update { it.copy(isSubmitting = true) }
        var allSuccess = true
        var lastError: String? = null

        unvalidated.forEach { check ->
            repository.validateCheck(
                taskId = taskId,
                checkName = check.checkName,
                result = check.effectiveResult,
                confidence = 1.0,
                validatedAtStageId = stageId,
                editedByOperator = check.isEdited
            ).onFailure {
                allSuccess = false
                lastError = it.message
            }
        }

        if (allSuccess) {
            refresh()
            _events.send("All checks confirmed")
        } else {
            refresh()
            _events.send(lastError ?: "Some checks failed to confirm")
        }
        _state.update { it.copy(isSubmitting = false) }
    }

    fun editCheckpoint(
        checkpointId: String,
        editedResult: String?,
        editNote: String? = null,
        editedData: Map<String, String>? = null
    ) = viewModelScope.launch {
        _state.update { it.copy(isSubmitting = true) }
        repository.editCheckpoint(checkpointId, editedResult, editNote, editedData)
            .onSuccess { refresh(); _events.send("Edit saved") }
            .onFailure { _events.send(it.message ?: "Edit failed") }
        _state.update { it.copy(isSubmitting = false) }
    }

    fun revertCheckpoint(checkpointId: String) = viewModelScope.launch {
        _state.update { it.copy(isSubmitting = true) }
        repository.revertCheckpoint(checkpointId)
            .onSuccess { refresh(); _events.send("Reverted") }
            .onFailure { _events.send(it.message ?: "Revert failed") }
        _state.update { it.copy(isSubmitting = false) }
    }

    fun showOverrideDialog() = _state.update { it.copy(showOverrideDialog = true, overrideReason = "") }

    fun dismissOverrideDialog() = _state.update { it.copy(showOverrideDialog = false) }

    fun setOverrideReason(text: String) = _state.update { it.copy(overrideReason = text) }

    fun confirmOverride() = viewModelScope.launch {
        val reason = _state.value.overrideReason.trim()
        if (reason.isEmpty()) {
            _events.send("Please enter a reason for the override")
            return@launch
        }
        _state.update { it.copy(isSubmitting = true) }
        repository.overrideGate(
            taskId = taskId,
            stageId = stageId,
            reason = reason,
            reasonCode = "operator_override"
        ).onSuccess {
            dismissOverrideDialog()
            refresh()
            _events.send("Override request submitted")
        }.onFailure { _events.send(it.message ?: "Override failed") }
        _state.update { it.copy(isSubmitting = false) }
    }
}