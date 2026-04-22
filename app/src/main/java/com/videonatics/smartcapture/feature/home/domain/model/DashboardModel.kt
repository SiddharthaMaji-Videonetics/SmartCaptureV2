package com.videonatics.smartcapture.feature.home.domain.model

data class DashboardModel(
    val activeTasks: Int,
    val passRate: Int,
    val completedTasks: Int,
    val overridesPending: Int,
    val warningMessage: String? = null,
    val infoMessage: String? = null,
    val stagePassRates: List<StagePassRateModel>,
    val failureReasons: List<FailureReasonModel>
)

data class StagePassRateModel(
    val stage: String,
    val rate: Int
)

data class FailureReasonModel(
    val reason: String,
    val count: Int
)

data class UserMeModel(
    val username: String,
    val email: String,
    val userType: String
)
