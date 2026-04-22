package com.videonatics.smartcapture.feature.home.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DashboardResponse(
    val active_tasks: Int,
    val pass_rate: Int,
    val completed_tasks: Int,
    val overrides_pending: Int,
    val warning_message: String? = null,
    val info_message: String? = null,
    val stage_pass_rates: List<StagePassRateResponse>,
    val failure_reasons: List<FailureReasonResponse>
)

@Serializable
data class StagePassRateResponse(
    val stage: String,
    val rate: Int
)

@Serializable
data class FailureReasonResponse(
    val reason: String,
    val count: Int
)
