package com.videonatics.smartcapture.feature.home.data.datasource.remote

import com.videonatics.smartcapture.core.Constants
import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.core.data.safeGet
import com.videonatics.smartcapture.feature.home.data.model.DashboardWrapperResponse
import com.videonatics.smartcapture.feature.home.data.model.DashboardResponse
import com.videonatics.smartcapture.feature.home.data.model.UserMeResponse
import com.videonatics.smartcapture.feature.home.data.model.UserMeWrapperResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.parameter

interface HomeRemoteDataSource {
    suspend fun getDashboardData(window: String = "7d"): Result<DashboardResponse>
    suspend fun getUserMe(): Result<UserMeResponse>
}

class HomeRemoteDataSourceImpl(
    private val client: HttpClient,
    private val pref: Pref
) : HomeRemoteDataSource {
    
    override suspend fun getDashboardData(window: String): Result<DashboardResponse> = client.safeGet(
        endpoint = Constants.Endpoints.DASHBOARD,
        block = {
            header("Authorization", "Bearer ${pref.accessToken}")
            parameter("window", window)
        },
        map = { response -> 
            val wrapper: DashboardWrapperResponse = response.body()
            wrapper.data
        }
    )

    override suspend fun getUserMe(): Result<UserMeResponse> = client.safeGet(
        endpoint = Constants.Endpoints.ME,
        block = {
            header("Authorization", "Bearer ${pref.accessToken}")
        },
        map = { response ->
            val wrapper: UserMeWrapperResponse = response.body()
            wrapper.data
        }
    )
}
