package com.videonatics.smartcapture.feature.task.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TaskResponse(
    val id: Int,
    val title: String,
    val description: String?,
    val status: String,
    val created_at: String,
    val updated_at: String,
    val is_locked: Boolean = false
)
