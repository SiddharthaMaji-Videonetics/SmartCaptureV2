package com.videonatics.smartcapture.feature.auth.domain.repository

import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.feature.auth.data.datasource.remote.AuthRemoteDataSource
import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remote: AuthRemoteDataSource,
    private val pref: Pref
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<Boolean> {
        // Fake login for testing
        delay(2000) // Simulate network delay
        return if (email == "admin@admin.com" && password == "admin123") {
            Result.success(true)
        } else {
            // Fallback to real remote call if not the fake credentials
            remote.login(email, password)
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
