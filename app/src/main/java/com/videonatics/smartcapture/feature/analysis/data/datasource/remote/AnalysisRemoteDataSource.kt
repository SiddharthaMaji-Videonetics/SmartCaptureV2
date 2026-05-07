package com.videonatics.smartcapture.feature.analysis.data.datasource.remote

import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.core.data.safeGet
import com.videonatics.smartcapture.core.data.safePatch
import com.videonatics.smartcapture.core.data.safePost
import com.videonatics.smartcapture.feature.analysis.data.model.AnalysisCheckpointEditRequest
import com.videonatics.smartcapture.feature.analysis.data.model.AnalysisOverrideRequest
import com.videonatics.smartcapture.feature.analysis.data.model.AnalysisTaskDetailResponse
import com.videonatics.smartcapture.feature.analysis.data.model.AnalysisValidateCheckRequest
import com.videonatics.smartcapture.feature.analysis.data.model.AnalysisValidatedChecksResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header

interface AnalysisRemoteDataSource {
    suspend fun getTaskDetail(taskId: String): Result<AnalysisTaskDetailResponse>
    suspend fun getValidatedChecks(taskId: String): Result<AnalysisValidatedChecksResponse>
    suspend fun validateCheck(
        taskId: String,
        checkName: String,
        body: AnalysisValidateCheckRequest
    ): Result<Unit>
    suspend fun editCheckpoint(
        checkpointId: String,
        body: AnalysisCheckpointEditRequest
    ): Result<Unit>
    suspend fun overrideGate(
        taskId: String,
        body: AnalysisOverrideRequest
    ): Result<Unit>
}

class AnalysisRemoteDataSourceImpl(
    private val client: HttpClient,
    private val pref: Pref
) : AnalysisRemoteDataSource {

    override suspend fun getTaskDetail(taskId: String): Result<AnalysisTaskDetailResponse> =
        client.safeGet(
            endpoint = "tasks/$taskId",
            block = { header("Authorization", "Bearer ${pref.accessToken}") },
            map = { it.body() }
        )

    override suspend fun getValidatedChecks(taskId: String): Result<AnalysisValidatedChecksResponse> =
        client.safeGet(
            endpoint = "tasks/$taskId/validated-checks",
            block = { header("Authorization", "Bearer ${pref.accessToken}") },
            map = { it.body() }
        )

    override suspend fun validateCheck(
        taskId: String,
        checkName: String,
        body: AnalysisValidateCheckRequest
    ): Result<Unit> = client.safePost(
        endpoint = "tasks/$taskId/checks/$checkName/validate",
        body = body,
        map = {}
    )

    override suspend fun editCheckpoint(
        checkpointId: String,
        body: AnalysisCheckpointEditRequest
    ): Result<Unit> = client.safePatch(
        endpoint = "pipeline/checkpoints/$checkpointId/edit",
        body = body,
        map = {}
    )

    override suspend fun overrideGate(
        taskId: String,
        body: AnalysisOverrideRequest
    ): Result<Unit> = client.safePost(
        endpoint = "reviewer/tasks/$taskId/override-requests",
        body = body,
        map = {}
    )
}