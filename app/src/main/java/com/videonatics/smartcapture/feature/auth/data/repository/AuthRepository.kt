package com.videonatics.smartcapture.feature.auth.data.repository

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Boolean>
    suspend fun setLoggedIn(isLogged: Boolean)
    suspend fun getLoggedIn(): Boolean
    suspend fun logout()
}

