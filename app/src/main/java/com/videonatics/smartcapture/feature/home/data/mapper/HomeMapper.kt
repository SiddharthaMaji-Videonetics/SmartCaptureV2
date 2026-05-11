package com.videonatics.smartcapture.feature.home.data.mapper

import com.videonatics.smartcapture.feature.home.data.model.*
import com.videonatics.smartcapture.feature.home.domain.model.*

fun DashboardResponse.toDomain() = DashboardModel(
    role = role,
    kpis = kpis.map { it.toDomain() },
    passRateTrend = passRateTrend,
    stageBreakdown = stageBreakdown.map { it.toDomain() },
    commonFailures = commonFailures.map { it.toDomain() },
    activeTasks = activeTasks.map { it.toDomain() },
    insights = insights.map { it.toDomain() }
)

fun DashboardKpiResponse.toDomain() = DashboardKpiModel(
    key = key,
    label = label,
    value = value,
    unit = unit,
    severity = severity
)

fun StageBreakdownResponse.toDomain() = StageBreakdownModel(
    stageType = stageType,
    passed = passed,
    total = total,
    rate = rate
)

fun CommonFailureResponse.toDomain() = CommonFailureModel(
    reason = reason,
    count = count,
    correctiveAction = correctiveAction
)

fun DashboardActiveTaskResponse.toDomain() = DashboardActiveTaskModel(
    id = id,
    title = title,
    assetId = assetId,
    status = status,
    progress = progress.map { it.toDomain() }
)

fun TaskProgressResponse.toDomain() = TaskProgressModel(
    order = order,
    status = status
)

fun DashboardInsightResponse.toDomain() = DashboardInsightModel(
    severity = severity,
    message = message
)

fun UserMeResponse.toDomain() = UserMeModel(
    id = id,
    username = name,
    email = email,
    userType = role
)
