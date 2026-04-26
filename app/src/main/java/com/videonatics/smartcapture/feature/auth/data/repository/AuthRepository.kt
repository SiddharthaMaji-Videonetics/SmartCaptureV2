package com.videonatics.smartcapture.feature.auth.data.repository

import com.videonatics.smartcapture.feature.auth.data.model.LoginResponse

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<LoginResponse>
    suspend fun setLoggedIn(isLogged: Boolean)
    suspend fun getLoggedIn(): Boolean
    suspend fun logout()
    fun isTestUser(): Boolean
}
