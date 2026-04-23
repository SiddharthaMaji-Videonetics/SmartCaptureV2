package com.videonatics.smartcapture.feature.task.domain.repository

import com.videonatics.smartcapture.feature.task.domain.model.MyTaskModel
import com.videonatics.smartcapture.feature.task.domain.model.TaskModel

interface TaskRepository {
    suspend fun getTasks(
        statusFilter: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<List<TaskModel>>

    suspend fun getMyTasks(): Result<List<MyTaskModel>>

    suspend fun getTaskDetails(taskId: String): Result<MyTaskModel>
}
