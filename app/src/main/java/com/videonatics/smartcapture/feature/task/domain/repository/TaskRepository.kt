package com.videonatics.smartcapture.feature.task.domain.repository

import com.videonatics.smartcapture.feature.task.data.model.MyTaskResponse
import com.videonatics.smartcapture.feature.task.data.model.TaskResponse

interface TaskRepository {
    suspend fun getTasks(
        statusFilter: String? = null,
        skip: Int = 0,
        limit: Int = 10
    ): Result<List<TaskResponse>>

    suspend fun getMyTasks(): Result<List<MyTaskResponse>>
}
