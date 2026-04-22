package com.videonatics.smartcapture.feature.task.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MyTaskResponse(
    val id: Int,
    val site_name: String,
    val client_name: String,
    val city: String,
    val status: String,
    val description: String?,
    val stages: List<TaskResponse>
)
