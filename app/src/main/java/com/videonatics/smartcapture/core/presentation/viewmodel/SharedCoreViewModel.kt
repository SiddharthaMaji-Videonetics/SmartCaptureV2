package com.videonatics.smartcapture.core.presentation.viewmodel

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.core.domain.model.CaptureModel
import com.videonatics.smartcapture.feature.language.domain.model.LanguageModel
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.core.data.remote.websocket.SocketService
import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import com.videonatics.smartcapture.feature.capturelist.domain.repository.CaptureRepository
import com.videonatics.smartcapture.feature.language.domain.repository.LanguageRepository
import com.videonatics.smartcapture.ui.theme.ThemeMode
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
    private val captureRepository: CaptureRepository,
    private val socketService: SocketService
) : ViewModel() {

    private val _capturedImage = MutableStateFlow<Bitmap?>(null)
    val capturedImage = _capturedImage.asStateFlow()

    private val _capturedImageBoundBox = MutableStateFlow<RectF?>(null)
    val capturedImageBoundBox = _capturedImageBoundBox.asStateFlow()

    private val _capturedImageObbCorners = MutableStateFlow<List<PointF>?>(null)
    val capturedImageObbCorners = _capturedImageObbCorners.asStateFlow()

    private val _rotationAngle = MutableStateFlow(0f)
    val rotationAngle = _rotationAngle.asStateFlow()

    private val _currentStageId = MutableStateFlow<String?>(null)
    val currentStageId = _currentStageId.asStateFlow()

    private val _currentTaskId = MutableStateFlow<String?>(null)
    val currentTaskId = _currentTaskId.asStateFlow()

    private val _isAssignmentLoading = MutableStateFlow(false)
    val isAssignmentLoading = _isAssignmentLoading.asStateFlow()

    private val _selectedCaptureId = MutableStateFlow<String?>(null)
    val selectedCaptureId = _selectedCaptureId.asStateFlow()

    private val _recaptureTargetId = MutableStateFlow<String?>(null)
    private val _recaptureTargetStageId = MutableStateFlow<String?>(null)

    private var sourceImageWidth: Float = 0f
    private var sourceImageHeight: Float = 0f

    private var imageQuality: Float = 0f
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    val socketEvents = socketService.events
    val taskStreamEvents = socketService.taskEvents

    init {
        viewModelScope.launch {
            if (authRepository.getLoggedIn()) {
                socketService.connect()
            }
        }
    }

    fun startSocket() {
        socketService.connect()
    }

    fun stopSocket() {
        socketService.disconnect()
    }

    val capturedImagesList: StateFlow<List<CaptureModel>> = captureRepository.getAllCaptures()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCapture(image: Bitmap, boundBox: RectF? = null, obbCorners: List<PointF>? = null, rotation: Float = 0f, stageId: String? = null) {
        viewModelScope.launch {
            val newCapture = CaptureModel(
                id = UUID.randomUUID().toString(),
                image = image,
                boundBox = boundBox,
                obbCorners = obbCorners,
                rotation = rotation,
                stageId = stageId
            )
            captureRepository.saveCapture(newCapture)
        }
    }

    fun setSelectedCapture(capture: CaptureModel) {
        _capturedImage.value = capture.image
        _capturedImageBoundBox.value = capture.boundBox
        _capturedImageObbCorners.value = capture.obbCorners
        _rotationAngle.value = capture.rotation
        _currentStageId.value = capture.stageId
        _selectedCaptureId.value = capture.id
        sourceImageWidth = capture.image.width.toFloat()
        sourceImageHeight = capture.image.height.toFloat()
    }

    fun setRecaptureTarget() {
        _recaptureTargetId.value = _selectedCaptureId.value
        _recaptureTargetStageId.value = _currentStageId.value
    }

    fun clearRecaptureTarget() {
        _recaptureTargetId.value = null
        _recaptureTargetStageId.value = null
    }

    fun updateCaptureWithStage(capture: CaptureModel, stageId: String) {
        viewModelScope.launch {
            _isAssignmentLoading.value = true
            try {
                captureRepository.saveCapture(capture.copy(stageId = stageId))
            } finally {
                _isAssignmentLoading.value = false
            }
        }
    }

    fun clearCaptureStage(capture: CaptureModel) {
        viewModelScope.launch {
            _isAssignmentLoading.value = true
            try {
                captureRepository.saveCapture(capture.copy(stageId = null))
            } finally {
                _isAssignmentLoading.value = false
            }
        }
    }

    private val _recentCaptureImagePreCaptures = mutableListOf<Bitmap>()
    val recentCaptureImagePreCaptures: List<Bitmap> = _recentCaptureImagePreCaptures
    
    fun addRecentCaptureImagePreCapture(image: Bitmap) {
        _recentCaptureImagePreCaptures.add(image)
    }
    
    fun saveCapturedImageBoundBox(bBox: RectF) {
        _capturedImageBoundBox.value = bBox
    }

    fun saveCapturedImageObbCorners(corners: List<PointF>) {
        _capturedImageObbCorners.value = corners
    }
    
    fun saveCapturedImage(img: Bitmap, boundBox: RectF? = null, obbCorners: List<PointF>? = null, rotation: Float = 0f) {
        _capturedImage.value = img
        _capturedImageBoundBox.value = boundBox
        _capturedImageObbCorners.value = obbCorners
        _rotationAngle.value = rotation

        val recaptureId = _recaptureTargetId.value
        val recaptureStageId = _recaptureTargetStageId.value

        val id = recaptureId ?: UUID.randomUUID().toString()
        val stageId = if (recaptureId != null) recaptureStageId else _currentStageId.value

        _selectedCaptureId.value = id
        if (recaptureId != null) {
            _currentStageId.value = stageId
            _recaptureTargetId.value = null
            _recaptureTargetStageId.value = null
        }

        viewModelScope.launch {
            captureRepository.saveCapture(
                CaptureModel(
                    id = id,
                    image = img,
                    boundBox = boundBox,
                    obbCorners = obbCorners,
                    rotation = rotation,
                    stageId = stageId
                )
            )
        }
    }

    fun getCapturedImage(): Bitmap? = _capturedImage.value
    fun getCapturedImageBoundBox(): RectF? = _capturedImageBoundBox.value
    fun getCapturedImageObbCorners(): List<PointF>? = _capturedImageObbCorners.value

    fun saveCurrentStageId(stageId: String?) {
        _currentStageId.value = stageId
    }

    fun getCurrentStageId(): String? = _currentStageId.value

    fun saveCurrentTaskId(taskId: String?) {
        _currentTaskId.value = taskId
    }

    fun deassignCurrentCapture() {
        val captureId = _selectedCaptureId.value ?: return
        viewModelScope.launch {
            capturedImagesList.value.find { it.id == captureId }?.let {
                captureRepository.saveCapture(it.copy(stageId = null))
            }
        }
    }

    fun saveSourceImageDimensions(width: Float, height: Float) {
        sourceImageWidth = width
        sourceImageHeight = height
    }

    fun getSourceImageWidth(): Float = sourceImageWidth
    fun getSourceImageHeight(): Float = sourceImageHeight

    fun saveMetadata(quality: Float, rotation: Float, lat: Double, lon: Double) {
        imageQuality = quality
        _rotationAngle.value = rotation
        latitude = lat
        longitude = lon
    }

    fun getImageQuality(): Float = imageQuality
    fun getRotationAngle(): Float = _rotationAngle.value
    fun getLatitude(): Double = latitude
    fun getLongitude(): Double = longitude


    val themeMode = coreRepository.themeMode

    fun setThemeMode(mode: ThemeMode) {
        coreRepository.setThemeMode(mode)
    }

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
            stopSocket()
            _showSessionExpiredDialog.value = true
        }
    }

    fun dismissSessionExpiredDialog() {
        _showSessionExpiredDialog.value = false
        viewModelScope.launch {
            _unauthorizedEvent.emit(Unit)
        }
    }

    fun deleteCapturedImage(capture: CaptureModel) {
        viewModelScope.launch {
            captureRepository.deleteCapture(capture.id)
        }

    }
}
