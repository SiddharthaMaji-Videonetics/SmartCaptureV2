package com.videonatics.smartcapture.feature.home.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserMeResponse(
    val username: String,
    val email: String,
    val user_type: String
)
