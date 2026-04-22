package com.videonatics.smartcapture.feature.task.data.mapper

import com.videonatics.smartcapture.feature.task.data.model.MyTaskResponse
import com.videonatics.smartcapture.feature.task.data.model.TaskResponse
import com.videonatics.smartcapture.feature.task.domain.model.MyTaskModel
import com.videonatics.smartcapture.feature.task.domain.model.TaskModel

fun TaskResponse.toDomain() = TaskModel(
    id = id,
    title = title,
    description = description,
    status = status,
    createdAt = created_at,
    updatedAt = updated_at,
    isLocked = is_locked
)

fun MyTaskResponse.toDomain() = MyTaskModel(
    id = id,
    siteName = site_name,
    clientName = client_name,
    city = city,
    status = status,
    description = description,
    stages = stages.map { it.toDomain() }
)
