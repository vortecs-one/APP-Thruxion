package com.thruxion.app.network.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface HuaweiHealthApi {
    
    @POST("sampleSet:polymerize")
    suspend fun polymerize(
        @Header("Authorization") token: String,
        @Body request: PolymerizeRequest
    ): Response<PolymerizeResponse>
}

data class PolymerizeRequest(
    val polymerizeWith: List<PolymerizeWith>,
    val startTime: Long,
    val endTime: Long,
    val groupByTime: GroupByTime? = null
)

data class PolymerizeWith(
    val dataTypeName: String
)

data class GroupByTime(
    val groupPeriod: GroupPeriod
)

data class GroupPeriod(
    val unit: String,
    val value: Int,
    val timeZone: String
)

data class PolymerizeResponse(
    val group: List<Group>?
)

data class Group(
    val sampleSet: List<SampleSet>?
)

data class SampleSet(
    val dataCollectorId: String?,
    val samplePoints: List<SamplePoint>?
)

data class SamplePoint(
    val startTime: Long,
    val endTime: Long,
    val value: List<SampleValue>?
)

data class SamplePointValue(
    val intValue: Int? = null,
    val fpValue: Double? = null
)

// Correcting the naming conflict with my thought vs implementation
data class SampleValue(
    val intValue: Int? = null,
    val fpValue: Double? = null
)
