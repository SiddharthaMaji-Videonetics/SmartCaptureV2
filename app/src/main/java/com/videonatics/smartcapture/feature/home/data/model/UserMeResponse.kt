package com.videonatics.smartcapture.feature.home.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserMeWrapperResponse(
    val success: Boolean,
    val data: UserMeResponse
)

@Serializable
data class UserMeResponse(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    val locale: String,
    val createdAt: String
)
