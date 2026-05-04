package com.videonatics.smartcapture.feature.moresettings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MoreSettingsViewModel @Inject constructor(
    private val coreRepository: CoreRepository
) : ViewModel() {

    val themeMode = coreRepository.themeMode

    private val _isAutoFlashEnabled = MutableStateFlow(false)
    val isAutoFlashEnabled = _isAutoFlashEnabled.asStateFlow()

    fun setThemeMode(themeMode: ThemeMode) {
        coreRepository.setThemeMode(themeMode)
    }

    fun toggleAutoFlash(enabled: Boolean) {
        _isAutoFlashEnabled.value = enabled
    }
}