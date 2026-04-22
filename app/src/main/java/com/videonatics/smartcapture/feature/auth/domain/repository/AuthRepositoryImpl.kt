package com.videonatics.smartcapture.feature.auth.domain.repository

import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.feature.auth.data.datasource.remote.AuthRemoteDataSource
import com.videonatics.smartcapture.feature.auth.data.model.LoginRequest
import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remote: AuthRemoteDataSource,
    private val pref: Pref
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<Boolean> {
        return remote.login(email, password)
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
