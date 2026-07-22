package com.thruxion.app.utils

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectProvider : WearableProvider {
    override val id: String = "health_connect"
    override val displayName: String = "Google Health Connect"

    private fun getClient(context: Context) = HealthConnectClient.getOrCreate(context)

    override suspend fun isConnected(context: Context): Boolean {
        val manager = HealthManager(context)
        return manager.isHealthConnectAvailable() && manager.hasAllPermissions()
    }

    override suspend fun connect(context: Context): Boolean {
        // Connection flow is handled by the Fragment via activity result
        return false 
    }

    override suspend fun disconnect(context: Context) {
        // Health Connect is a system service, we just stop using it locally
    }

    override suspend fun getSteps(context: Context, startTime: Instant, endTime: Instant): Long {
        return try {
            val response = getClient(context).readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records.sumOf { it.count }
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun getLatestHeartRate(context: Context): Int? {
        return try {
            val endTime = Instant.now()
            val startTime = endTime.minus(24, ChronoUnit.HOURS)
            val response = getClient(context).readRecords(
                ReadRecordsRequest(
                    HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records.lastOrNull()?.samples?.lastOrNull()?.beatsPerMinute?.toInt()
        } catch (e: Exception) {
            null
        }
    }
}
