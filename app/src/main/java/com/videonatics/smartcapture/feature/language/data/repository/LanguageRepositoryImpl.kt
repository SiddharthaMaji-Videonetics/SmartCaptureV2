package com.videonatics.smartcapture.feature.language.data.repository

import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.feature.language.domain.model.LanguageModel
import com.videonatics.smartcapture.feature.language.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageRepositoryImpl @Inject constructor(
    private val pref: Pref
) : LanguageRepository {

    private val languages = listOf(
        LanguageModel("English", "en", "🇺🇸"),
        LanguageModel("Hindi", "hi", "🇮🇳"),
//        LanguageModel("Bengali", "bn", "🇮🇳"),
//        LanguageModel("Spanish", "es", "🇪🇸"),
//        LanguageModel("French", "fr", "🇫🇷")
    )

    private val _selectedLanguage = MutableStateFlow(
        languages.find { it.code == pref.selectedLanguageCode } ?: languages[0]
    )

    override fun getLanguages(): List<LanguageModel> = languages

    override fun getSelectedLanguage(): StateFlow<LanguageModel> = _selectedLanguage.asStateFlow()

    override suspend fun setSelectedLanguage(language: LanguageModel) {
        pref.selectedLanguageCode = language.code
        _selectedLanguage.value = language
    }
}
