package com.videonatics.smartcapture.feature.home.domain.repository

import com.videonatics.smartcapture.feature.home.domain.model.DashboardModel
import com.videonatics.smartcapture.feature.home.domain.model.UserMeModel

interface HomeRepository {
    suspend fun getDashboardData(): Result<DashboardModel>
    suspend fun getUserMe(): Result<UserMeModel>
}
