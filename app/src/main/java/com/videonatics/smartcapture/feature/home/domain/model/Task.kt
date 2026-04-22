package com.videonatics.smartcapture.feature.home.domain.model

data class Task(
    val title: String,
    val name: String,
    val location: String,
    val status: TaskStatus
)

enum class TaskStatus {
    IN_PROGRESS,
    COMPLETED,
    PENDING
}