package com.videonatics.smartcapture.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.core.data.UnauthorizedException
import com.videonatics.smartcapture.core.data.remote.websocket.SocketService
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCoreViewModel
import com.videonatics.smartcapture.feature.home.domain.model.DashboardModel
import com.videonatics.smartcapture.feature.home.domain.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val pref: Pref,
    private val socketService: SocketService
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

    private var socketEventJob: Job? = null

    // Hard cooldown: socket events can trigger at most 1 dashboard reload
    // every SOCKET_RELOAD_COOLDOWN_MS, regardless of how many events arrive.
    // This is the primary guard against 429 — debounce handles burst coalescing,
    // cooldown handles the reload→subscribe→event→reload loop.
    private var lastSocketReloadAt = 0L
    private val SOCKET_RELOAD_COOLDOWN_MS = 5_000L

    init {
        startSocketEventListener(pref.userId)
        loadDashboardData()
        loadUserMe()
    }

    fun loadDashboardData(window: String = _selectedWindow.value, sharedViewModel: SharedCoreViewModel? = null) {
        viewModelScope.launch {
            _selectedWindow.value = window
            _isLoading.value = true
            _error.value = null
            repository.getDashboardData(window)
                .onSuccess { _dashboardData.value = it }
                // ↑ No subscribeToTasks here — that path caused a reload→subscribe→
                //   event→reload loop that exhausted the server rate limit.
                .onFailure {
                    if (it is UnauthorizedException) sharedViewModel?.triggerUnauthorizedLogout()
                    _error.value = it.message ?: "An unknown error occurred"
                }
            _isLoading.value = false
        }
    }

    fun loadUserMe(sharedViewModel: SharedCoreViewModel? = null) {
        viewModelScope.launch {
            repository.getUserMe()
                .onSuccess { user ->
                    pref.userId = user.id
                    pref.userName = user.username
                    pref.userEmail = user.email
                    pref.userType = user.userType
                    _userName.value = user.username
                    _userType.value = user.userType
                    startSocketEventListener(user.id)
                }
                .onFailure {
                    if (it is UnauthorizedException) sharedViewModel?.triggerUnauthorizedLogout()
                }
        }
    }

    // Subscribes to user-tasks:<userId> only.
    // Per-task (subscribe_many) subscriptions are owned by TaskViewModel;
    // those events also flow through the shared taskEvents flow and reach
    // this collector, so no separate per-task subscription is needed here.
    private fun startSocketEventListener(userId: String) {
        socketEventJob?.cancel()
        socketEventJob = viewModelScope.launch {
            if (userId.isNotBlank()) {
                socketService.subscribeToUserTasks(userId)
            }
            socketService.taskEvents
                .filter { it.event != "subscribed" }
                .debounce(2_000L)          // coalesce event bursts
                .collect {
                    val now = System.currentTimeMillis()
                    if (now - lastSocketReloadAt >= SOCKET_RELOAD_COOLDOWN_MS) {
                        lastSocketReloadAt = now
                        loadDashboardData(_selectedWindow.value)
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketEventJob?.cancel()
    }
}
