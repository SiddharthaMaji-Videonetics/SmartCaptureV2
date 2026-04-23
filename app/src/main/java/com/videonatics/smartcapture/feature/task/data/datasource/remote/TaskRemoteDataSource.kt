package com.videonatics.smartcapture.feature.task.data.datasource.remote

import com.videonatics.smartcapture.core.Constants
import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.core.data.safeGet
import com.videonatics.smartcapture.feature.task.data.model.MyTaskResponse
import com.videonatics.smartcapture.feature.task.data.model.SingleTaskResponse
import com.videonatics.smartcapture.feature.task.data.model.TasksResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.parameter

interface TaskRemoteDataSource {
    suspend fun getTasks(
        statusFilter: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<TasksResponse>

    suspend fun getMyTasks(page: Int = 1, limit: Int = 20): Result<MyTaskResponse>
    suspend fun getTaskDetails(taskId: String): Result<SingleTaskResponse>
}

class TaskRemoteDataSourceImpl(
    private val client: HttpClient,
    private val pref: Pref
) : TaskRemoteDataSource {

    override suspend fun getTasks(
        statusFilter: String?,
        page: Int,
        limit: Int
    ): Result<TasksResponse> = client.safeGet(
        endpoint = Constants.Endpoints.TASKS,
        block = {
            header("Authorization", "Bearer ${pref.accessToken}")
            parameter("page", page)
            parameter("limit", limit)
            statusFilter?.let { parameter("status", it) }
        },
        map = { it.body() }
    )

    override suspend fun getMyTasks(page: Int, limit: Int): Result<MyTaskResponse> = client.safeGet(
        endpoint = Constants.Endpoints.MY_TASKS,
        block = {
            header("Authorization", "Bearer ${pref.accessToken}")
            parameter("page", page)
            parameter("limit", limit)
        },
        map = { it.body() }
    )

    override suspend fun getTaskDetails(taskId: String): Result<SingleTaskResponse> = client.safeGet(
        endpoint = "${Constants.Endpoints.TASK_DETAILS}$taskId",
        block = {
            header("Authorization", "Bearer ${pref.accessToken}")
        },
        map = { it.body() }
    )
}
