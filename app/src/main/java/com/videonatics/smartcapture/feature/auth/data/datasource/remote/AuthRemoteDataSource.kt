package com.videonatics.smartcapture.feature.auth.data.datasource.remote

import com.videonatics.smartcapture.core.Constants
import com.videonatics.smartcapture.core.data.safePost
import com.videonatics.smartcapture.feature.auth.data.model.LoginRequest
import com.videonatics.smartcapture.feature.auth.data.model.LoginResponse
import com.videonatics.smartcapture.feature.auth.data.model.LogoutRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body

interface AuthRemoteDataSource {
    suspend fun login(email: String, password: String): Result<LoginResponse>
    suspend fun logout(refreshToken: String): Result<Unit>
}

class AuthRemoteDataSourceImpl(private val client: HttpClient) : AuthRemoteDataSource {

    override suspend fun login(email: String, password: String): Result<LoginResponse> =
        client.safePost(Constants.Endpoints.LOGIN, LoginRequest(email, password)) { it.body<LoginResponse>() }

    override suspend fun logout(refreshToken: String): Result<Unit> =
        client.safePost(Constants.Endpoints.LOGOUT, LogoutRequest(refreshToken)) {}
}
