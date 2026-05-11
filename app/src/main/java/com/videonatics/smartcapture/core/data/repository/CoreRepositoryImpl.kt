package com.videonatics.smartcapture.core.data.repository

import com.videonatics.smartcapture.core.data.datasource.local.LocalCoreDataSource
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoreRepositoryImpl @Inject constructor(
    private val localDataSource: LocalCoreDataSource
) : CoreRepository {
    private val _themeMode = MutableStateFlow(localDataSource.getThemeMode())
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override fun setThemeMode(themeMode: ThemeMode) {
        localDataSource.setThemeMode(themeMode)
        _themeMode.value = themeMode
    }

    private val _isAutoFlash = MutableStateFlow(localDataSource.getAutoFlash())
    override val isAutoFlash: StateFlow<Boolean> = _isAutoFlash.asStateFlow()

    override fun setAutoFlash(enabled: Boolean) {
        localDataSource.setAutoFlash(enabled)
        _isAutoFlash.value = enabled
    }
}