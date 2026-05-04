package com.videonatics.smartcapture.core.domain.repository

import com.videonatics.smartcapture.ui.theme.ThemeMode
import kotlinx.coroutines.flow.StateFlow

interface CoreRepository {
    val themeMode: StateFlow<ThemeMode>
    fun setThemeMode(themeMode: ThemeMode)
}