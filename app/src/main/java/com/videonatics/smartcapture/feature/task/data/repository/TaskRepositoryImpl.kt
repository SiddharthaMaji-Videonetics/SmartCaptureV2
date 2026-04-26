package com.videonatics.smartcapture.feature.task.data.repository

import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import com.videonatics.smartcapture.feature.task.data.datasource.remote.TaskRemoteDataSource
import com.videonatics.smartcapture.feature.task.data.mapper.toDomain
import com.videonatics.smartcapture.feature.task.domain.model.MyTaskModel
import com.videonatics.smartcapture.feature.task.domain.model.TaskModel
import com.videonatics.smartcapture.feature.task.domain.model.TaskStageModel
import com.videonatics.smartcapture.feature.task.domain.repository.TaskRepository

class TaskRepositoryImpl(
    private val remoteDataSource: TaskRemoteDataSource,
    private val authRepository: AuthRepository
) : TaskRepository {
    override suspend fun getTasks(
        statusFilter: String?,
        page: Int,
        limit: Int
    ): Result<List<TaskModel>> {
        if (authRepository.isTestUser()) {
            return Result.success(
                listOf(
                    TaskModel(
                        id = "1",
                        title = "Test Task 1",
                        description = "Description for test task 1",
                        status = "PENDING",
                        createdAt = "2023-10-27T10:00:00Z",
                        updatedAt = "2023-10-27T10:00:00Z"
                    ),
                    TaskModel(
                        id = "2",
                        title = "Test Task 2",
                        description = "Description for test task 2",
                        status = "IN_PROGRESS",
                        createdAt = "2023-10-27T11:00:00Z",
                        updatedAt = "2023-10-27T11:00:00Z"
                    )
                )
            )
        }
        return remoteDataSource.getTasks(statusFilter, page, limit).map { response ->
            response.data.map { it.toDomain() }
        }
    }

    override suspend fun getMyTasks(): Result<List<MyTaskModel>> {
        if (authRepository.isTestUser()) {
            return Result.success(
                listOf(
                    MyTaskModel(
                        id = "1",
                        title = "My Test Task 1",
                        description = "Description for my test task 1",
                        assetId = "ASSET-001",
                        location = "Warehouse A",
                        status = "PENDING",
                        createdAt = "2023-10-27T10:00:00Z",
                        updatedAt = "2023-10-27T10:00:00Z",
                        assignedToName = "Admin User",
                        stages = listOf(
                            TaskStageModel("s1", "CAPTURE", "PENDING", 1),
                            TaskStageModel("s2", "REVIEW", "PENDING", 2)
                        )
                    )
                )
            )
        }
        return remoteDataSource.getMyTasks().map { response ->
            response.data.map { it.toDomain() }
        }
    }

    override suspend fun getTaskDetails(taskId: String): Result<MyTaskModel> {
        if (authRepository.isTestUser()) {
            return getMyTasks().map { it.first() }
        }
        return remoteDataSource.getTaskDetails(taskId).map { response ->
            response.data.toDomain()
        }
    }
}
