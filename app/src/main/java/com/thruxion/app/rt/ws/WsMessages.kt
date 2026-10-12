package com.thruxion.app.rt.ws

import com.thruxion.app.rt.db.PendingPoint
import kotlinx.serialization.Serializable

@Serializable
data class WsEnvelope<T>(
    val v: Int = 1,
    val type: String,
    val id: String? = null,
    val ref: String? = null,
    val data: T
)

@Serializable
data class BatchPointsData(
    val points: List<PendingPoint>
)

@Serializable
data class AuthRefreshData(
    val token: String
)

@Serializable
data class AckData(
    val seqs: List<Long> = emptyList(),
    val rejected: List<RejectedSeq> = emptyList(),
    val expiresAt: Long? = null
)

@Serializable
data class RejectedSeq(
    val seq: Long,
    val reason: String? = null
)

@Serializable
data class ErrorData(
    val code: String
)

@Serializable
data class SessionExpiringData(
    val expiresAt: Long
)
