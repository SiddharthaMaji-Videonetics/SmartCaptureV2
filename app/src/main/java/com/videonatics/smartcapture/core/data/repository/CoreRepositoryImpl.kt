package com.videonatics.smartcapture.core.data.repository

import com.videonatics.smartcapture.core.data.datasource.local.LocalCoreDataSource
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.ui.theme.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoreRepositoryImpl @Inject constructor(
    private val localDataSource: LocalCoreDataSource
) : CoreRepository {
    private var themeMode = ThemeMode.LIGHT

    override fun getThemeMode(): ThemeMode = themeMode
    override fun setThemeMode(themeMode: ThemeMode) { this.themeMode = themeMode }
}