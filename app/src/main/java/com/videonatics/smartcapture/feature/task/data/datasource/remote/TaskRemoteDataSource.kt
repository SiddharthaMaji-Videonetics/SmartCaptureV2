package com.videonatics.smartcapture.feature.task.data.datasource.remote

import com.videonatics.smartcapture.core.Constants
import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.core.data.safeGet
import com.videonatics.smartcapture.feature.task.data.model.MyTaskResponse
import com.videonatics.smartcapture.feature.task.data.model.TaskResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.parameter

interface TaskRemoteDataSource {
    suspend fun getTasks(
        statusFilter: String? = null,
        skip: Int = 0,
        limit: Int = 10
    ): Result<List<TaskResponse>>

    suspend fun getMyTasks(): Result<List<MyTaskResponse>>
}

class TaskRemoteDataSourceImpl(
    private val client: HttpClient,
    private val pref: Pref
) : TaskRemoteDataSource {
    override suspend fun getTasks(
        statusFilter: String?,
        skip: Int,
        limit: Int
    ): Result<List<TaskResponse>> = client.safeGet(
        endpoint = Constants.Endpoints.TASKS,
        block = {
            header("Authorization", "Bearer ${pref.accessToken}")
            parameter("status_filter", statusFilter)
            parameter("skip", skip)
            parameter("limit", limit)
        },
        map = { it.body() }
    )

    override suspend fun getMyTasks(): Result<List<MyTaskResponse>> = client.safeGet(
        endpoint = Constants.Endpoints.MY_TASKS,
        block = {
            header("Authorization", "Bearer ${pref.accessToken}")
        },
        map = { it.body() }
    )
}
