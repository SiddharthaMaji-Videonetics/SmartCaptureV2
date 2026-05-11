package com.videonatics.smartcapture.feature.task.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.core.data.remote.websocket.SocketService
import com.videonatics.smartcapture.feature.task.domain.model.MyTaskModel
import com.videonatics.smartcapture.feature.task.domain.model.TaskModel
import com.videonatics.smartcapture.feature.task.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val socketService: SocketService,
    private val pref: Pref
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<TaskModel>>(emptyList())
    val tasks = _tasks.asStateFlow()

    private val _myTasks = MutableStateFlow<List<MyTaskModel>>(emptyList())
    val myTasks = _myTasks.asStateFlow()

    private val _taskDetails = MutableStateFlow<MyTaskModel?>(null)
    val taskDetails = _taskDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var taskDetailEventJob: Job? = null
    private var myTasksEventJob: Job? = null

    // Tracks which task IDs are currently subscribed so we only call
    // subscribeToTasks when the set genuinely changes — preventing the
    // subscribe→event→reload→subscribe loop that caused 429s.
    private var subscribedTaskIds: Set<String> = emptySet()

    // Hard cooldown between socket-triggered reloads (separate from debounce).
    private var lastSocketReloadAt = 0L
    private val SOCKET_RELOAD_COOLDOWN_MS = 5_000L

    init {
        startMyTasksSocketListener()
        loadMyTasks()
    }

    fun loadTasks(statusFilter: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getTasks(statusFilter = statusFilter)
                .onSuccess { _tasks.value = it }
                .onFailure { _error.value = it.message ?: "An unknown error occurred" }
            _isLoading.value = false
        }
    }

    // Public load — also updates the subscribe_many subscription when task IDs change.
    // This is the ONLY place subscribeToTasks is called; reload paths never call it,
    // which prevents the subscribe→event→reload→subscribe loop.
    fun loadMyTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getMyTasks()
                .onSuccess { tasks ->
                    _myTasks.value = tasks
                    updateTaskSubscription(tasks.map { it.id })
                }
                .onFailure { _error.value = it.message ?: "An unknown error occurred" }
            _isLoading.value = false
        }
    }

    fun loadTaskDetails(taskId: String) {
        fetchTaskDetails(taskId)
        subscribeToTaskDetailEvents(taskId)
    }

    private fun fetchTaskDetails(taskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getTaskDetails(taskId)
                .onSuccess { _taskDetails.value = it }
                .onFailure { _error.value = it.message ?: "An unknown error occurred" }
            _isLoading.value = false
        }
    }

    private fun subscribeToTaskDetailEvents(taskId: String) {
        taskDetailEventJob?.cancel()
        taskDetailEventJob = viewModelScope.launch {
            socketService.subscribeToTask(taskId)
            socketService.taskEvents
                .filter { it.event != "subscribed" }
                .debounce(2_000L)
                .collect { event ->
                    val eventTaskId = event.data["taskId"]?.toString()
                    if (eventTaskId == null || eventTaskId == taskId) {
                        fetchTaskDetails(taskId)
                    }
                }
        }
    }

    // Subscribes to user-tasks:<userId>. Any non-ack event triggers a list
    // re-fetch via reloadMyTasksOnly (which does NOT call subscribeToTasks).
    private fun startMyTasksSocketListener() {
        myTasksEventJob?.cancel()
        myTasksEventJob = viewModelScope.launch {
            val userId = pref.userId
            if (userId.isNotBlank()) {
                socketService.subscribeToUserTasks(userId)
            }
            socketService.taskEvents
                .filter { it.event != "subscribed" }
                .debounce(2_000L)
                .collect {
                    val now = System.currentTimeMillis()
                    if (now - lastSocketReloadAt >= SOCKET_RELOAD_COOLDOWN_MS) {
                        lastSocketReloadAt = now
                        reloadMyTasksOnly()
                    }
                }
        }
    }

    // Reloads the task list without touching subscriptions — safe to call
    // from the socket event path because it breaks the reload→subscribe loop.
    private fun reloadMyTasksOnly() {
        viewModelScope.launch {
            repository.getMyTasks()
                .onSuccess { tasks -> _myTasks.value = tasks }
                .onFailure { _error.value = it.message ?: "An unknown error occurred" }
        }
    }

    // Only calls subscribeToTasks when the task ID set has actually changed,
    // preventing redundant subscribe messages that would cause the server to
    // send acks/events and re-trigger the reload cycle.
    private suspend fun updateTaskSubscription(taskIds: List<String>) {
        val newIds = taskIds.toSet()
        if (newIds != subscribedTaskIds) {
            subscribedTaskIds = newIds
            if (newIds.isNotEmpty()) {
                socketService.subscribeToTasks(newIds.toList())
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        taskDetailEventJob?.cancel()
        myTasksEventJob?.cancel()
    }
}
