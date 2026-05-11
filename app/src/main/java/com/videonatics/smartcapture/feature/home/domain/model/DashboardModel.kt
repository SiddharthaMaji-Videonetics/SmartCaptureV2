package com.videonatics.smartcapture.feature.home.domain.model

data class DashboardModel(
    val role: String,
    val kpis: List<DashboardKpiModel>,
    val passRateTrend: List<Int>,
    val stageBreakdown: List<StageBreakdownModel>,
    val commonFailures: List<CommonFailureModel>,
    val activeTasks: List<DashboardActiveTaskModel>,
    val insights: List<DashboardInsightModel>
)

data class DashboardKpiModel(
    val key: String,
    val label: String,
    val value: Float,
    val unit: String,
    val severity: String
)

data class StageBreakdownModel(
    val stageType: String,
    val passed: Int,
    val total: Int,
    val rate: Float
)

data class CommonFailureModel(
    val reason: String,
    val count: Int,
    val correctiveAction: String?
)

data class DashboardActiveTaskModel(
    val id: String,
    val title: String,
    val assetId: String,
    val status: String,
    val progress: List<TaskProgressModel>
)

data class TaskProgressModel(
    val order: Int,
    val status: String
)

data class DashboardInsightModel(
    val severity: String,
    val message: String
)

data class UserMeModel(
    val id: String,
    val username: String,
    val email: String,
    val userType: String
)
