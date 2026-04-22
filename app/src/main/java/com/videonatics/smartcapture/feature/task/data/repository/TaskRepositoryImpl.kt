package com.videonatics.smartcapture.feature.task.data.repository

import com.videonatics.smartcapture.feature.task.data.datasource.remote.TaskRemoteDataSource
import com.videonatics.smartcapture.feature.task.data.mapper.toDomain
import com.videonatics.smartcapture.feature.task.data.model.MyTaskResponse
import com.videonatics.smartcapture.feature.task.data.model.TaskResponse
import com.videonatics.smartcapture.feature.task.domain.model.MyTaskModel
import com.videonatics.smartcapture.feature.task.domain.model.TaskModel
import com.videonatics.smartcapture.feature.task.domain.repository.TaskRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val remote: TaskRemoteDataSource
) : TaskRepository {
    override suspend fun getTasks(
        statusFilter: String?,
        skip: Int,
        limit: Int
    ): Result<List<TaskModel>> {
        // Uncomment below to use real remote data
        // return remote.getTasks(statusFilter, skip, limit).map { it.map { response -> response.toDomain() } }

        delay(1000)
        
        val rawFakeTasks = listOf(
            TaskResponse(101, "Site Survey - Sector V", "Initial pre-installation check for the North wing.", "COMPLETED", "2023-12-01T10:00:00Z", "2023-12-01T10:00:00Z"),
            TaskResponse(102, "Camera Calibration - Zone A", "Calibrate existing smart capture units for better accuracy.", "IN_PROGRESS", "2023-12-02T11:30:00Z", "2023-12-05T14:20:00Z"),
            TaskResponse(103, "Hardware Maintenance", "Quarterly hardware health check and lens cleaning.", "OPEN", "2023-11-20T09:00:00Z", "2023-11-20T17:00:00Z"),
            TaskResponse(104, "Network Setup - Project X", "Configuring dedicated VLAN for camera traffic.", "OPEN", "2023-12-06T08:00:00Z", "2023-12-06T08:00:00Z")
        )

        val processedTasks = mutableListOf<TaskModel>()
        var previousTaskCompleted = true
        
        for (task in rawFakeTasks) {
            val isLocked = !previousTaskCompleted
            processedTasks.add(task.copy(is_locked = isLocked).toDomain())
            previousTaskCompleted = task.status.equals("COMPLETED", ignoreCase = true)
        }

        val filteredTasks = if (statusFilter != null) {
            processedTasks.filter { it.status.equals(statusFilter, ignoreCase = true) }
        } else {
            processedTasks
        }

        return Result.success(filteredTasks)
    }

    override suspend fun getMyTasks(): Result<List<MyTaskModel>> {
        // Uncomment below to use real remote data
        // return remote.getMyTasks().map { it.map { response -> response.toDomain() } }

        delay(1000)
        
        val fakeMyTasks = listOf(
            MyTaskResponse(
                id = 1,
                site_name = "Tech Park North",
                client_name = "Videonatics",
                city = "Kolkata",
                status = "IN_PROGRESS",
                description = "Smart Capture V2 Deployment",
                stages = emptyList() // Not needed for the list view
            ),
            MyTaskResponse(
                id = 2,
                site_name = "Sector V Substation",
                client_name = "WBSEDCL",
                city = "Kolkata",
                status = "OPEN",
                description = "Substation monitoring setup",
                stages = emptyList()
            )
        )
        
        return Result.success(fakeMyTasks.map { it.toDomain() })
    }
}
