package com.videonatics.smartcapture.feature.task.domain.repository

import com.videonatics.smartcapture.feature.task.domain.model.MyTaskModel
import com.videonatics.smartcapture.feature.task.domain.model.TaskModel

interface TaskRepository {
    suspend fun getTasks(
        statusFilter: String? = null,
        skip: Int = 0,
        limit: Int = 10
    ): Result<List<TaskModel>>

    suspend fun getMyTasks(): Result<List<MyTaskModel>>
}
