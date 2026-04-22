package com.videonatics.smartcapture.core.presentation.viewmodel


import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.feature.language.domain.model.LanguageModel
import com.videonatics.smartcapture.feature.language.domain.repository.LanguageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedCoreViewModel @Inject constructor(
    private val coreRepository: CoreRepository,
    private val languageRepository: LanguageRepository
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

    fun selectLanguage(language: LanguageModel) {
        viewModelScope.launch {
            languageRepository.setSelectedLanguage(language)
        }
    }
}
