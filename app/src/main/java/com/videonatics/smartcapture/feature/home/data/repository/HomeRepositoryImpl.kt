package com.videonatics.smartcapture.feature.home.data.repository

import com.videonatics.smartcapture.feature.home.data.datasource.remote.HomeRemoteDataSource
import com.videonatics.smartcapture.feature.home.data.mapper.toDomain
import com.videonatics.smartcapture.feature.home.domain.model.DashboardModel
import com.videonatics.smartcapture.feature.home.domain.model.UserMeModel
import com.videonatics.smartcapture.feature.home.domain.repository.HomeRepository

class HomeRepositoryImpl(
    private val remoteDataSource: HomeRemoteDataSource
) : HomeRepository {
    override suspend fun getDashboardData(window: String): Result<DashboardModel> {
        return remoteDataSource.getDashboardData(window).map { it.toDomain() }
    }

    override suspend fun getUserMe(): Result<UserMeModel> {
        return remoteDataSource.getUserMe().map { it.toDomain() }
    }
}
