package com.videonatics.smartcapture.feature.task.data.repository

import com.videonatics.smartcapture.feature.task.data.datasource.remote.TaskRemoteDataSource
import com.videonatics.smartcapture.feature.task.data.mapper.toDomain
import com.videonatics.smartcapture.feature.task.domain.model.MyTaskModel
import com.videonatics.smartcapture.feature.task.domain.model.TaskModel
import com.videonatics.smartcapture.feature.task.domain.repository.TaskRepository
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val remote: TaskRemoteDataSource
) : TaskRepository {

    override suspend fun getMyTasks(): Result<List<MyTaskModel>> {
        return remote.getMyTasks().map { response ->
            response.data.map { it.toDomain() }
        }
    }

    override suspend fun getTaskDetails(taskId: String): Result<MyTaskModel> {
        return remote.getTaskDetails(taskId).map { response ->
            response.data.toDomain()
        }
    }

    override suspend fun getTasks(
        statusFilter: String?,
        page: Int,
        limit: Int
    ): Result<List<TaskModel>> {
        return remote.getTasks(statusFilter, page, limit).map { response ->
            response.data.map { it.toDomain() }
        }
    }
}
