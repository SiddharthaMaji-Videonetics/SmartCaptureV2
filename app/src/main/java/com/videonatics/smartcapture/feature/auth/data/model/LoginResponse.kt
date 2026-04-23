package com.videonatics.smartcapture.feature.auth.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val success: Boolean,
    val data: LoginData
)

@Serializable
data class LoginData(
    val accessToken: String,
    val refreshToken: String,
    val user: UserInfo
)

@Serializable
data class UserInfo(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    val locale: String
)
