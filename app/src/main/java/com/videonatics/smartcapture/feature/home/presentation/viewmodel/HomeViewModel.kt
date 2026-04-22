package com.videonatics.smartcapture.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.feature.home.data.model.DashboardResponse
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

    private val _dashboardData = MutableStateFlow<DashboardResponse?>(null)
    val dashboardData = _dashboardData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _userName = MutableStateFlow(pref.userName)
    val userName = _userName.asStateFlow()

    private val _userType = MutableStateFlow(pref.userType)
    val userType = _userType.asStateFlow()

    init {
        loadDashboardData()
        loadUserMe()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getDashboardData()
                .onSuccess {
                    _dashboardData.value = it
                }
                .onFailure {
                    _error.value = it.message ?: "An unknown error occurred"
                }
            _isLoading.value = false
        }
    }

    fun loadUserMe() {
        viewModelScope.launch {
            repository.getUserMe()
                .onSuccess {
                    pref.userName = it.username
                    pref.userEmail = it.email
                    pref.userType = it.user_type
                    
                    _userName.value = it.username
                    _userType.value = it.user_type
                }
        }
    }
}
