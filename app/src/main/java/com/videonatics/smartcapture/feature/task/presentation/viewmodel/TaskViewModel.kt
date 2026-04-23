package com.videonatics.smartcapture.feature.task.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.feature.task.domain.model.MyTaskModel
import com.videonatics.smartcapture.feature.task.domain.model.TaskModel
import com.videonatics.smartcapture.feature.task.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository
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

    init {
        loadMyTasks()
    }

    fun loadTasks(statusFilter: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getTasks(statusFilter = statusFilter)
                .onSuccess {
                    _tasks.value = it
                }
                .onFailure {
                    _error.value = it.message ?: "An unknown error occurred"
                }
            _isLoading.value = false
        }
    }

    fun loadMyTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getMyTasks()
                .onSuccess {
                    _myTasks.value = it
                }
                .onFailure {
                    _error.value = it.message ?: "An unknown error occurred"
                }
            _isLoading.value = false
        }
    }

    fun loadTaskDetails(taskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getTaskDetails(taskId)
                .onSuccess {
                    _taskDetails.value = it
                }
                .onFailure {
                    _error.value = it.message ?: "An unknown error occurred"
                }
            _isLoading.value = false
        }
    }
}
