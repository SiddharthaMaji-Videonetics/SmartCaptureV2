package com.videonatics.smartcapture.core.presentation.viewmodel

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.core.domain.model.CaptureModel
import com.videonatics.smartcapture.feature.language.domain.model.LanguageModel
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import com.videonatics.smartcapture.feature.capturelist.domain.repository.CaptureRepository
import com.videonatics.smartcapture.feature.language.domain.repository.LanguageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SharedCoreViewModel @Inject constructor(
    private val coreRepository: CoreRepository,
    private val languageRepository: LanguageRepository,
    private val authRepository: AuthRepository,
    private val captureRepository: CaptureRepository
) : ViewModel() {
    private var capturedImage: Bitmap? = null
    private var capturedImageBoundBox: RectF? = null
    private var capturedImageObbCorners: List<PointF>? = null
    private var currentStageId: String? = null

    private var sourceImageWidth: Float = 0f
    private var sourceImageHeight: Float = 0f

    private var imageQuality: Float = 0f
    private var rotationAngle: Float = 0f
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    val capturedImagesList: StateFlow<List<CaptureModel>> = captureRepository.getAllCaptures()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCapture(image: Bitmap, boundBox: RectF? = null, obbCorners: List<PointF>? = null) {
        viewModelScope.launch {
            val newCapture = CaptureModel(
                id = UUID.randomUUID().toString(),
                image = image,
                boundBox = boundBox,
                obbCorners = obbCorners
            )
            captureRepository.saveCapture(newCapture)
        }
    }

    fun setSelectedCapture(capture: CaptureModel) {
        capturedImage = capture.image
        capturedImageBoundBox = capture.boundBox
        capturedImageObbCorners = capture.obbCorners
        // When loading from a saved capture, we use the image's own dimensions as source for normalization
        sourceImageWidth = capture.image.width.toFloat()
        sourceImageHeight = capture.image.height.toFloat()
    }

    private val _recentCaptureImagePreCaptures = mutableListOf<Bitmap>()
    val recentCaptureImagePreCaptures: List<Bitmap> = _recentCaptureImagePreCaptures
    
    fun addRecentCaptureImagePreCapture(image: Bitmap) {
        _recentCaptureImagePreCaptures.add(image)
    }
    
    fun saveCapturedImageBoundBox(bBox: RectF) {
        capturedImageBoundBox = bBox
    }

    fun saveCapturedImageObbCorners(corners: List<PointF>) {
        capturedImageObbCorners = corners
    }
    
    fun saveCapturedImage(img: Bitmap, boundBox: RectF? = null, obbCorners: List<PointF>? = null) {
        capturedImage = img
        capturedImageBoundBox = boundBox
        capturedImageObbCorners = obbCorners
        // Also add to the list for MyCapturesPage
        addCapture(img, boundBox, obbCorners)
    }

    fun getCapturedImage(): Bitmap? {
        return capturedImage
    }

    fun getCapturedImageBoundBox(): RectF? {
        return capturedImageBoundBox
    }

    fun getCapturedImageObbCorners(): List<PointF>? {
        return capturedImageObbCorners
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

    private val _showSessionExpiredDialog = MutableStateFlow(false)
    val showSessionExpiredDialog = _showSessionExpiredDialog.asStateFlow()

    fun selectLanguage(language: LanguageModel) {
        viewModelScope.launch {
            languageRepository.setSelectedLanguage(language)
        }
    }

    fun triggerUnauthorizedLogout() {
        if (authRepository.isTestUser()) return

        viewModelScope.launch {
            authRepository.logout()
            _showSessionExpiredDialog.value = true
        }
    }

    fun dismissSessionExpiredDialog() {
        _showSessionExpiredDialog.value = false
        viewModelScope.launch {
            _unauthorizedEvent.emit(Unit)
        }
    }
}
