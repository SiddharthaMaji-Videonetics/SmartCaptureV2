package com.videonatics.smartcapture.feature.home.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DashboardWrapperResponse(
    val success: Boolean,
    val data: DashboardResponse
)

@Serializable
data class DashboardResponse(
    val role: String,
    val range: DashboardRangeResponse,
    val kpis: List<DashboardKpiResponse>,
    val passRateTrend: List<Int>,
    val stageBreakdown: List<StageBreakdownResponse>,
    val commonFailures: List<CommonFailureResponse>,
    val activeTasks: List<DashboardActiveTaskResponse>,
    val insights: List<DashboardInsightResponse>
)

@Serializable
data class DashboardRangeResponse(
    val window: String,
    val from: String,
    val to: String,
    val previousFrom: String? = null,
    val previousTo: String? = null,
    val bucket: String,
    val bucketCount: Int
)

@Serializable
data class DashboardKpiResponse(
    val key: String,
    val label: String,
    val value: Float,
    val previousValue: Float? = null,
    val delta: Float? = null,
    val unit: String,
    val severity: String,
    val sparkline: List<Int>? = null
)

@Serializable
data class StageBreakdownResponse(
    val stageType: String,
    val passed: Int,
    val total: Int,
    val rate: Float
)

@Serializable
data class CommonFailureResponse(
    val reason: String,
    val count: Int,
    val correctiveAction: String? = null
)

@Serializable
data class DashboardActiveTaskResponse(
    val id: String,
    val title: String,
    val assetId: String,
    val status: String,
    val progress: List<TaskProgressResponse>,
    val createdAt: String
)

@Serializable
data class TaskProgressResponse(
    val order: Int,
    val status: String
)

@Serializable
data class DashboardInsightResponse(
    val severity: String,
    val rule: String,
    val message: String
)
