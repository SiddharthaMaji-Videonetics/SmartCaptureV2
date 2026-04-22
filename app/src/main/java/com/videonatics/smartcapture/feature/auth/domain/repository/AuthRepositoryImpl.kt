package com.videonatics.smartcapture.feature.auth.domain.repository

import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.feature.auth.data.model.LoginRequest
import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val client: HttpClient,
    private val pref: Pref
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<Boolean> {
        return try {
            val response = client.post("http://172.16.1.214/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }
            if (response.status.isSuccess()) {
                Result.success(true)
            } else {
                Result.failure(Exception("Login failed with status: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setLoggedIn(isLogged: Boolean) {
        pref.isLoggedInPref = isLogged
    }

    override suspend fun getLoggedIn(): Boolean {
        return pref.isLoggedInPref
    }

    override suspend fun logout() {
        pref.isLoggedInPref = false
    }
}
