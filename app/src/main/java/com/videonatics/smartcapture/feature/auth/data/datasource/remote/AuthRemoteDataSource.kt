package com.videonatics.smartcapture.feature.auth.data.datasource.remote

import com.videonatics.smartcapture.core.Constants
import com.videonatics.smartcapture.core.data.safePost
import com.videonatics.smartcapture.feature.auth.data.model.LoginRequest
import io.ktor.client.HttpClient

interface AuthRemoteDataSource {
    suspend fun login(email: String, password: String): Result<Boolean>
}

class AuthRemoteDataSourceImpl(private val client: HttpClient) : AuthRemoteDataSource {
    
    override suspend fun login(email: String, password: String): Result<Boolean> = 
        client.safePost(Constants.Endpoints.LOGIN, LoginRequest(email, password)) { true }
}
