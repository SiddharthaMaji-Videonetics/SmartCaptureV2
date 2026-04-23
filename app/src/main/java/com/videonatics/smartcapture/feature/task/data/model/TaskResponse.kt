package com.videonatics.smartcapture.feature.task.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SingleTaskResponse(
    val success: Boolean,
    val data: MyTaskData
)

@Serializable
data class TasksResponse(
    val success: Boolean,
    val data: List<TaskResponse>,
    val meta: MetaInfo
)

@Serializable
data class TaskResponse(
    val id: String,
    val title: String,
    val description: String?,
    val status: String,
    val created_at: String,
    val updated_at: String,
    val is_locked: Boolean = false
)
