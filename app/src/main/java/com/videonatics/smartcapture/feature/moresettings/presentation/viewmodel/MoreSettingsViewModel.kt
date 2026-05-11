package com.videonatics.smartcapture.feature.moresettings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MoreSettingsViewModel @Inject constructor(
    private val coreRepository: CoreRepository
) : ViewModel() {

    val themeMode = coreRepository.themeMode
    val isAutoFlashEnabled = coreRepository.isAutoFlash

    fun setThemeMode(themeMode: ThemeMode) {
        coreRepository.setThemeMode(themeMode)
    }

    fun toggleAutoFlash(enabled: Boolean) {
        coreRepository.setAutoFlash(enabled)
    }
}