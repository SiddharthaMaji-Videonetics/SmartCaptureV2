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
