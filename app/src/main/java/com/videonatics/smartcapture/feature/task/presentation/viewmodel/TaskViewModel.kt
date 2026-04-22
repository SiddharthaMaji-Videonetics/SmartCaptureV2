package com.videonatics.smartcapture.feature.task.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.feature.task.data.model.MyTaskResponse
import com.videonatics.smartcapture.feature.task.data.model.TaskResponse
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

    private val _tasks = MutableStateFlow<List<TaskResponse>>(emptyList())
    val tasks = _tasks.asStateFlow()

    private val _myTasks = MutableStateFlow<List<MyTaskResponse>>(emptyList())
    val myTasks = _myTasks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadTasks()
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
}
