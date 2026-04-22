package com.videonatics.smartcapture.feature.language.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.videonatics.smartcapture.feature.language.domain.repository.LanguageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val repository: LanguageRepository
) : ViewModel() {
    // Functionality moved to SharedCoreViewModel
}
