package com.videonatics.smartcapture.core

object Constants {
    const val API_VAR = "/api/v1/"
    const val BASE_URL = "https://airtel.videonetics.com:2443"
    const val WS_BASE_URL = "wss://airtel.videonetics.com:2443/ws"

    
    object Endpoints {
        const val LOGIN = "auth/login"
        const val TASKS = "tasks"
        const val TASK_DETAILS = "tasks/"
        const val MY_TASKS = "tasks"
        const val DASHBOARD = "dashboard"
        const val ME = "auth/me"
        const val UPLOAD_IMAGE = "upload/stages/"
        const val VALIDATED_CHECKS = "validated-checks"
        const val VALIDATE_CHECK = "validate"
        const val EDIT_CHECKPOINT = "pipeline/checkpoints"
        const val GATE_OVERRIDE = "gate-override"
    }
}
