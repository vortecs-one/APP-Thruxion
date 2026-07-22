package com.thruxion.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Manager for Wearable and Health operations.
 * Coordinates multiple providers (Health Connect, Huawei, etc.).
 */
class HealthManager(private val context: Context) {

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    /**
     * List of all supported providers.
     */
    val providers = listOf(
        HealthConnectProvider(),
        HuaweiHealthProvider()
    )

    /**
     * Set of permissions required for Health Connect (legacy support).
     */
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    fun isHealthConnectAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    fun installHealthConnect() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun hasAllPermissions(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    /**
     * Aggregates steps from all connected providers.
     */
    suspend fun readTotalSteps(startTime: Instant, endTime: Instant): Long {
        var total = 0L
        for (provider in providers) {
            if (provider.isConnected(context)) {
                total += provider.getSteps(context, startTime, endTime)
            }
        }
        return total
    }

    /**
     * Gets the latest heart rate from any connected provider.
     */
    suspend fun readLatestHeartRate(): Int? {
        for (provider in providers) {
            if (provider.isConnected(context)) {
                val hr = provider.getLatestHeartRate(context)
                if (hr != null) return hr
            }
        }
        return null
    }

    // --- LEGACY METHODS (Maintained for existing UI compatibility) ---
    
    suspend fun readSteps(startTime: Instant, endTime: Instant): List<StepsRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun readHeartRateLast24Hours(): List<HeartRateRecord> {
        val endTime = Instant.now()
        val startTime = endTime.minus(24, ChronoUnit.HOURS)
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records
        } catch (e: Exception) {
            emptyList()
        }
    }
}
