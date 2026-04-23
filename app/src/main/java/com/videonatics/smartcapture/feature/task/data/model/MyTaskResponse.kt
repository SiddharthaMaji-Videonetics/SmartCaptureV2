package com.videonatics.smartcapture.feature.task.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MyTaskResponse(
    val success: Boolean,
    val data: List<MyTaskData>,
    val meta: MetaInfo
)

@Serializable
data class MyTaskData(
    val id: String,
    val title: String,
    val description: String?,
    val assetId: String,
    val location: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val assignedToId: String,
    val createdById: String,
    val reviewedById: String?,
    val assignedTo: AssignedUser,
    val reviewedBy: AssignedUser?,
    val stages: List<TaskStage>
)

@Serializable
data class AssignedUser(
    val id: String,
    val name: String,
    val email: String
)

@Serializable
data class TaskStage(
    val id: String,
    val stageType: String,
    val status: String,
    val order: Int
)

@Serializable
data class MetaInfo(
    val total: Int,
    val page: Int,
    val limit: Int
)
