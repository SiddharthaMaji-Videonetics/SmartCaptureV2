package com.videonatics.smartcapture.feature.home.data.repository

import com.videonatics.smartcapture.feature.home.data.datasource.remote.HomeRemoteDataSource
import com.videonatics.smartcapture.feature.home.data.model.DashboardResponse
import com.videonatics.smartcapture.feature.home.data.model.UserMeResponse
import com.videonatics.smartcapture.feature.home.domain.repository.HomeRepository

class HomeRepositoryImpl(
    private val remoteDataSource: HomeRemoteDataSource
) : HomeRepository {
    override suspend fun getDashboardData(): Result<DashboardResponse> {
        return remoteDataSource.getDashboardData()
    }

    override suspend fun getUserMe(): Result<UserMeResponse> {
        return remoteDataSource.getUserMe()
    }
}
