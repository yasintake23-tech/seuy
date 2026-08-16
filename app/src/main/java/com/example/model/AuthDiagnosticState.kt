package com.example.model

enum class StepStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    FAIL
}

data class AuthStageLog(
    val stageNumber: Int,
    val stageTitle: String,
    val status: StepStatus,
    val info: String = "",
    val exceptionClass: String? = null,
    val errorCode: String? = null,
    val exceptionMessage: String? = null
)

data class AuthDiagnosticState(
    val operation: String = "",
    val timestamp: Long = 0L,
    val isRunning: Boolean = false,
    val overallSuccess: Boolean? = null,
    val stages: List<AuthStageLog> = emptyList()
)
