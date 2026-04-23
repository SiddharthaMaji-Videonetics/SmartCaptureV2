package com.videonatics.smartcapture.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.core.data.UnauthorizedException
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.feature.home.domain.model.DashboardModel
import com.videonatics.smartcapture.feature.home.domain.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val pref: Pref
) : ViewModel() {

    private val _dashboardData = MutableStateFlow<DashboardModel?>(null)
    val dashboardData = _dashboardData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _userName = MutableStateFlow(pref.userName)
    val userName = _userName.asStateFlow()

    private val _userType = MutableStateFlow(pref.userType)
    val userType = _userType.asStateFlow()

    private val _selectedWindow = MutableStateFlow("7d")
    val selectedWindow = _selectedWindow.asStateFlow()

    init {
        loadDashboardData()
        loadUserMe()
    }

    fun loadDashboardData(window: String = _selectedWindow.value, sharedViewModel: SharedCoreViewModel? = null) {
        viewModelScope.launch {
            _selectedWindow.value = window
            _isLoading.value = true
            _error.value = null
            repository.getDashboardData(window)
                .onSuccess {
                    _dashboardData.value = it
                }
                .onFailure {
                    if (it is UnauthorizedException) {
                        sharedViewModel?.triggerUnauthorizedLogout()
                    }
                    _error.value = it.message ?: "An unknown error occurred"
                }
            _isLoading.value = false
        }
    }

    fun loadUserMe(sharedViewModel: SharedCoreViewModel? = null) {
        viewModelScope.launch {
            repository.getUserMe()
                .onSuccess {
                    pref.userName = it.username
                    pref.userEmail = it.email
                    pref.userType = it.userType
                    
                    _userName.value = it.username
                    _userType.value = it.userType
                }
                .onFailure {
                    if (it is UnauthorizedException) {
                        sharedViewModel?.triggerUnauthorizedLogout()
                    }
                }
        }
    }
}
