package com.videonatics.smartcapture.feature.language.domain.repository

import com.videonatics.smartcapture.feature.language.domain.model.LanguageModel
import kotlinx.coroutines.flow.StateFlow

interface LanguageRepository {
    fun getLanguages(): List<LanguageModel>
    fun getSelectedLanguage(): StateFlow<LanguageModel>
    suspend fun setSelectedLanguage(language: LanguageModel)
}
