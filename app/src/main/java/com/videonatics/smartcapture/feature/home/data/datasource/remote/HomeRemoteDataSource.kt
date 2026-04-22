package com.videonatics.smartcapture.feature.home.data.datasource.remote

import com.videonatics.smartcapture.core.Constants
import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.core.data.safeGet
import com.videonatics.smartcapture.feature.home.data.model.DashboardResponse
import com.videonatics.smartcapture.feature.home.data.model.FailureReasonResponse
import com.videonatics.smartcapture.feature.home.data.model.StagePassRateResponse
import com.videonatics.smartcapture.feature.home.data.model.UserMeResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import kotlinx.coroutines.delay

interface HomeRemoteDataSource {
    suspend fun getDashboardData(): Result<DashboardResponse>
    suspend fun getUserMe(): Result<UserMeResponse>
}

class HomeRemoteDataSourceImpl(
    private val client: HttpClient,
    private val pref: Pref
) : HomeRemoteDataSource {
    override suspend fun getDashboardData(): Result<DashboardResponse> {
        // We are returning fake data directly to ensure the UI renders correctly 
        // while the backend is being integrated.
        
        delay(500) // Small delay to simulate network
        return Result.success(
            DashboardResponse(
                active_tasks = 2,
                pass_rate = 45,
                completed_tasks = 10,
                overrides_pending = 1,
                warning_message = "Your pass rate is 45% — below 50%. Review actions.",
                info_message = "You have 1 override request awaiting review.",
                stage_pass_rates = listOf(
                    StagePassRateResponse("CL-PRE", 40),
                    StagePassRateResponse("OP-PRE", 50),
                    StagePassRateResponse("OP-POST", 30),
                    StagePassRateResponse("CL-POST", 60)
                ),
                failure_reasons = listOf(
                    FailureReasonResponse("Screenshot detected", 2),
                    FailureReasonResponse("Asset needs cleaning", 1),
                    FailureReasonResponse("ID not readable", 3)
                )
            )
        )
    }

    override suspend fun getUserMe(): Result<UserMeResponse> = client.safeGet(
        endpoint = Constants.Endpoints.ME,
        block = {
            header("Authorization", "Bearer ${pref.accessToken}")
        },
        map = { it.body() }
    )
}
