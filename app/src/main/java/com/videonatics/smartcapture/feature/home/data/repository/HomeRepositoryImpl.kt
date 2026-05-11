package com.videonatics.smartcapture.feature.home.data.repository

import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import com.videonatics.smartcapture.feature.home.data.datasource.remote.HomeRemoteDataSource
import com.videonatics.smartcapture.feature.home.data.mapper.toDomain
import com.videonatics.smartcapture.feature.home.domain.model.DashboardModel
import com.videonatics.smartcapture.feature.home.domain.model.UserMeModel
import com.videonatics.smartcapture.feature.home.domain.repository.HomeRepository

class HomeRepositoryImpl(
    private val remoteDataSource: HomeRemoteDataSource,
    private val authRepository: AuthRepository
) : HomeRepository {
    override suspend fun getDashboardData(window: String): Result<DashboardModel> {
        if (authRepository.isTestUser()) {
            return Result.success(
                DashboardModel(
                    role = "ADMIN",
                    kpis = emptyList(),
                    passRateTrend = emptyList(),
                    stageBreakdown = emptyList(),
                    commonFailures = emptyList(),
                    activeTasks = emptyList(),
                    insights = emptyList()
                )
            )
        }
        return remoteDataSource.getDashboardData(window).map { it.toDomain() }
    }

    override suspend fun getUserMe(): Result<UserMeModel> {
        if (authRepository.isTestUser()) {
            return Result.success(
                UserMeModel(
                    id = "",
                    username = "Admin User",
                    email = "admin@admin.com",
                    userType = "ADMIN"
                )
            )
        }
        return remoteDataSource.getUserMe().map { it.toDomain() }
    }
}
