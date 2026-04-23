package com.videonatics.smartcapture.feature.task.data.mapper

import com.videonatics.smartcapture.feature.task.data.model.MyTaskData
import com.videonatics.smartcapture.feature.task.data.model.TaskResponse
import com.videonatics.smartcapture.feature.task.data.model.TaskStage
import com.videonatics.smartcapture.feature.task.domain.model.MyTaskModel
import com.videonatics.smartcapture.feature.task.domain.model.TaskModel
import com.videonatics.smartcapture.feature.task.domain.model.TaskStageModel

fun MyTaskData.toDomain() = MyTaskModel(
    id = id,
    title = title,
    description = description,
    assetId = assetId,
    location = location,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    assignedToName = assignedTo.name,
    stages = stages.map { it.toDomain() }
)

fun TaskResponse.toDomain() = TaskModel(
    id = id,
    title = title,
    description = description,
    status = status,
    createdAt = created_at,
    updatedAt = updated_at,
    isLocked = is_locked
)

fun TaskStage.toDomain() = TaskStageModel(
    id = id,
    stageType = stageType,
    status = status,
    order = order
)
