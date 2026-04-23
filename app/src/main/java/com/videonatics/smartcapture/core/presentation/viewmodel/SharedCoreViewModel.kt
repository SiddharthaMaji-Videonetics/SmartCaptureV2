package com.videonatics.smartcapture.core.presentation.viewmodel


import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import com.videonatics.smartcapture.feature.language.domain.model.LanguageModel
import com.videonatics.smartcapture.feature.language.domain.repository.LanguageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedCoreViewModel @Inject constructor(
    private val coreRepository: CoreRepository,
    private val languageRepository: LanguageRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private var capturedImage: Bitmap? = null
    private var capturedImageBoundBox: RectF? = null
    private var currentStageId: String? = null

    private var sourceImageWidth: Float = 0f
    private var sourceImageHeight: Float = 0f

    private var imageQuality: Float = 0f
    private var rotationAngle: Float = 0f
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    fun saveCapturedImageBoundBox(bBox: RectF) {
        capturedImageBoundBox = bBox
    }
    fun saveCapturedImage(img: Bitmap) {
        capturedImage = img
    }
    fun getCapturedImage(): Bitmap? {
        return capturedImage
    }

    fun getCapturedImageBoundBox(): RectF? {
        return capturedImageBoundBox
    }

    fun saveCurrentStageId(stageId: String) {
        currentStageId = stageId
    }

    fun getCurrentStageId(): String? {
        return currentStageId
    }

    fun saveSourceImageDimensions(width: Float, height: Float) {
        sourceImageWidth = width
        sourceImageHeight = height
    }

    fun getSourceImageWidth(): Float = sourceImageWidth
    fun getSourceImageHeight(): Float = sourceImageHeight

    fun saveMetadata(quality: Float, rotation: Float, lat: Double, lon: Double) {
        imageQuality = quality
        rotationAngle = rotation
        latitude = lat
        longitude = lon
    }

    fun getImageQuality(): Float = imageQuality
    fun getRotationAngle(): Float = rotationAngle
    fun getLatitude(): Double = latitude
    fun getLongitude(): Double = longitude


    var themeMode = coreRepository.getThemeMode()

    val selectedLanguage = languageRepository.getSelectedLanguage()

    val languages = languageRepository.getLanguages()

    private val _unauthorizedEvent = MutableSharedFlow<Unit>()
    val unauthorizedEvent = _unauthorizedEvent.asSharedFlow()

    fun selectLanguage(language: LanguageModel) {
        viewModelScope.launch {
            languageRepository.setSelectedLanguage(language)
        }
    }

    fun triggerUnauthorizedLogout() {
        viewModelScope.launch {
            authRepository.logout()
            _unauthorizedEvent.emit(Unit)
        }
    }
}
