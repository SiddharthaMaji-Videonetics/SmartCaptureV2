package com.videonatics.smartcapture.feature.auth.domain.repository

import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.feature.auth.data.datasource.remote.AuthRemoteDataSource
import com.videonatics.smartcapture.feature.auth.data.model.LoginData
import com.videonatics.smartcapture.feature.auth.data.model.LoginResponse
import com.videonatics.smartcapture.feature.auth.data.model.UserInfo
import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remote: AuthRemoteDataSource,
    private val pref: Pref
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<LoginResponse> {
        // Fake login for testing
        if (email == "admin@admin.com" && password == "admin123") {
            delay(1000)
            val fakeResponse = LoginResponse(
                success = true,
                data = LoginData(
                    accessToken = "fake_access_token",
                    refreshToken = "fake_refresh_token",
                    user = UserInfo(
                        id = "1",
                        email = email,
                        name = "Admin User",
                        role = "ADMIN",
                        locale = "en"
                    )
                )
            )
            pref.isTestUser = true
            saveUserData(fakeResponse.data)
            return Result.success(fakeResponse)
        }

        val result = remote.login(email, password)
        result.onSuccess {
            pref.isTestUser = false
            saveUserData(it.data)
        }
        return result
    }

    private fun saveUserData(data: LoginData) {
        pref.isLoggedInPref = true
        pref.accessToken = data.accessToken
        pref.userName = data.user.name
        pref.userEmail = data.user.email
        pref.userType = data.user.role
    }

    override suspend fun setLoggedIn(isLogged: Boolean) {
        pref.isLoggedInPref = isLogged
    }

    override suspend fun getLoggedIn(): Boolean {
        return pref.isLoggedInPref
    }

    override suspend fun logout() {
        pref.isLoggedInPref = false
        pref.isTestUser = false
        pref.accessToken = null
        pref.userName = ""
        pref.userEmail = ""
        pref.userType = ""
    }

    override fun isTestUser(): Boolean {
        return pref.isTestUser
    }
}
