package com.videonatics.smartcapture.feature.details.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.feature.details.FatBoxClassifier
import com.videonatics.smartcapture.feature.details.data.model.ImageQualityResult
import com.videonatics.smartcapture.feature.details.domain.repository.ImageUploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class CaptureDetailsViewModel @Inject constructor(
    private val repository: ImageUploadRepository
) : ViewModel() {

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    private val _qualityState = MutableStateFlow<QualityState>(QualityState.Idle)
    val qualityState: StateFlow<QualityState> = _qualityState

    private val _classificationState = MutableStateFlow<ClassificationState>(ClassificationState.Idle)
    val classificationState: StateFlow<ClassificationState> = _classificationState

    fun analyzeQuality(bitmap: Bitmap) {
        viewModelScope.launch {
            _qualityState.value = QualityState.Loading
            repository.analyzeImageQuality(bitmap)
                .onSuccess { _qualityState.value = QualityState.Success(it) }
                .onFailure { _qualityState.value = QualityState.Error(it.message ?: "Analysis failed") }
        }
    }

    fun classifyImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _classificationState.value = ClassificationState.Loading
            repository.classifyImage(bitmap)
                .onSuccess { _classificationState.value = ClassificationState.Success(it) }
                .onFailure { _classificationState.value = ClassificationState.Error(it.message ?: "Classification failed") }
        }
    }

    fun uploadImage(stageId: String, bitmap: Bitmap) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            
            val imageBytes = bitmapToByteArray(bitmap)
            val result = repository.uploadImage(stageId, imageBytes)
            
            result.onSuccess {
                _uploadState.value = UploadState.Success
            }.onFailure {
                _uploadState.value = UploadState.Error(it.message ?: "Unknown error")
            }
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    sealed class UploadState {
        object Idle : UploadState()
        object Loading : UploadState()
        object Success : UploadState()
        data class Error(val message: String) : UploadState()
    }

    sealed class QualityState {
        object Idle : QualityState()
        object Loading : QualityState()
        data class Success(val result: ImageQualityResult) : QualityState()
        data class Error(val message: String) : QualityState()
    }

    sealed class ClassificationState {
        object Idle : ClassificationState()
        object Loading : ClassificationState()
        data class Success(val result: FatBoxClassifier.Result) : ClassificationState()
        data class Error(val message: String) : ClassificationState()
    }
}
