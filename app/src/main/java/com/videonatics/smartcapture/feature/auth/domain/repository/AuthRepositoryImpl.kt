package com.videonatics.smartcapture.feature.auth.domain.repository

import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    val pref: Pref
): AuthRepository{
    override suspend fun login(email: String, password: String): Result<Boolean>{
        delay(1500)
        return Result.success(true)
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

