package com.videonatics.smartcapture.feature.home.data.mapper

import com.videonatics.smartcapture.feature.home.data.model.DashboardResponse
import com.videonatics.smartcapture.feature.home.data.model.FailureReasonResponse
import com.videonatics.smartcapture.feature.home.data.model.StagePassRateResponse
import com.videonatics.smartcapture.feature.home.data.model.UserMeResponse
import com.videonatics.smartcapture.feature.home.domain.model.DashboardModel
import com.videonatics.smartcapture.feature.home.domain.model.FailureReasonModel
import com.videonatics.smartcapture.feature.home.domain.model.StagePassRateModel
import com.videonatics.smartcapture.feature.home.domain.model.UserMeModel

fun DashboardResponse.toDomain() = DashboardModel(
    activeTasks = active_tasks,
    passRate = pass_rate,
    completedTasks = completed_tasks,
    overridesPending = overrides_pending,
    warningMessage = warning_message,
    infoMessage = info_message,
    stagePassRates = stage_pass_rates.map { it.toDomain() },
    failureReasons = failure_reasons.map { it.toDomain() }
)

fun StagePassRateResponse.toDomain() = StagePassRateModel(
    stage = stage,
    rate = rate
)

fun FailureReasonResponse.toDomain() = FailureReasonModel(
    reason = reason,
    count = count
)

fun UserMeResponse.toDomain() = UserMeModel(
    username = username,
    email = email,
    userType = user_type
)
