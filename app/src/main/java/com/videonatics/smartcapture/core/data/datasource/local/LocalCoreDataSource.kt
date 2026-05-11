package com.videonatics.smartcapture.core.data.datasource.local

import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.ui.theme.ThemeMode
import javax.inject.Inject

interface LocalCoreDataSource {
    fun getThemeMode(): ThemeMode
    fun setThemeMode(themeMode: ThemeMode)
    fun getAutoFlash(): Boolean
    fun setAutoFlash(enabled: Boolean)
}

class LocalCoreDataSourceImpl @Inject constructor(
    private val pref: Pref
) : LocalCoreDataSource {
    override fun getThemeMode(): ThemeMode {
        return try {
            ThemeMode.valueOf(pref.themeModePref ?: ThemeMode.LIGHT.name)
        } catch (e: Exception) {
            ThemeMode.LIGHT
        }
    }

    override fun setThemeMode(themeMode: ThemeMode) {
        pref.themeModePref = themeMode.name
    }

    override fun getAutoFlash(): Boolean = pref.isAutoFlash

    override fun setAutoFlash(enabled: Boolean) { pref.isAutoFlash = enabled }
}