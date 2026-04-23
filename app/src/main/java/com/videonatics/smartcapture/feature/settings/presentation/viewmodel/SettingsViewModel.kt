package com.videonatics.smartcapture.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _logoutSuccess = MutableSharedFlow<Unit>()
    val logoutSuccess = _logoutSuccess.asSharedFlow()

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _logoutSuccess.emit(Unit)
        }
    }
}
