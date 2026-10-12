package com.thruxion.app.rt.ws

sealed interface HttpStatusAction {
    object StopClientError : HttpStatusAction
    object RefreshTokenAndReconnect : HttpStatusAction
    object StopAndRequireSignIn : HttpStatusAction
    data class Backoff(val delayMs: Long?) : HttpStatusAction
}

sealed interface CloseCodeAction {
    object ReconnectIfTracking : CloseCodeAction
    data class Backoff(val delayMs: Long?) : CloseCodeAction
    object RefreshTokenAndReconnect : CloseCodeAction
    object StopAndRequireSignIn : CloseCodeAction
    object DoNotReconnect : CloseCodeAction
}

object RtDecisionTable {
    fun handleHttpStatus(statusCode: Int): HttpStatusAction {
        return when (statusCode) {
            400 -> HttpStatusAction.StopClientError
            401 -> HttpStatusAction.RefreshTokenAndReconnect
            403 -> HttpStatusAction.StopAndRequireSignIn
            429 -> HttpStatusAction.Backoff(30_000L)
            else -> HttpStatusAction.Backoff(null)
        }
    }

    fun handleCloseCode(code: Int): CloseCodeAction {
        return when (code) {
            1000 -> CloseCodeAction.ReconnectIfTracking
            1003, 1009, 1013 -> CloseCodeAction.Backoff(null)
            1008 -> CloseCodeAction.Backoff(60_000L)
            1012 -> CloseCodeAction.Backoff(null)
            4001 -> CloseCodeAction.RefreshTokenAndReconnect
            4003 -> CloseCodeAction.StopAndRequireSignIn
            4009 -> CloseCodeAction.DoNotReconnect
            else -> CloseCodeAction.Backoff(null)
        }
    }
}
