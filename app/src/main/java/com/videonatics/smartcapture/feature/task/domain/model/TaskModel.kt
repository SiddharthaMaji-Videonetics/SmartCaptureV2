package com.videonatics.smartcapture.feature.task.domain.model

data class TaskModel(
    val id: Int,
    val title: String,
    val description: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val isLocked: Boolean = false
)

data class MyTaskModel(
    val id: Int,
    val siteName: String,
    val clientName: String,
    val city: String,
    val status: String,
    val description: String?,
    val stages: List<TaskModel>
)
