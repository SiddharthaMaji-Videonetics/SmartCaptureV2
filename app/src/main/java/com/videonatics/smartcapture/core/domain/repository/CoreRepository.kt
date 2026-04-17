package com.videonatics.smartcapture.core.domain.repository

import com.videonatics.smartcapture.ui.theme.ThemeMode

interface CoreRepository {
    fun getThemeMode(): ThemeMode
    fun setThemeMode(themeMode: ThemeMode)
}