package com.videonatics.smartcapture.feature.task.domain.model

data class TaskModel(
    val id: String,
    val title: String,
    val description: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val isLocked: Boolean = false
)

data class MyTaskModel(
    val id: String,
    val title: String,
    val description: String?,
    val assetId: String,
    val location: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val assignedToName: String,
    val stages: List<TaskStageModel>
)

data class TaskStageModel(
    val id: String,
    val stageType: String,
    val status: String,
    val order: Int
)
