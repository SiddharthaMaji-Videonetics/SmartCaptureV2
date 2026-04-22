package com.videonatics.smartcapture.feature.home.domain.repository

import com.videonatics.smartcapture.feature.home.data.model.DashboardResponse
import com.videonatics.smartcapture.feature.home.data.model.UserMeResponse

interface HomeRepository {
    suspend fun getDashboardData(): Result<DashboardResponse>
    suspend fun getUserMe(): Result<UserMeResponse>
}
