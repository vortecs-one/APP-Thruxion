package com.thruxion.app.utils

import android.content.Context
import java.time.Instant

/**
 * Common interface for all wearable/health data sources (Huawei, Garmin, Health Connect, etc.)
 */
interface WearableProvider {
    val id: String
    val displayName: String
    
    /**
     * Returns true if this provider is currently connected/authorized.
     */
    suspend fun isConnected(context: Context): Boolean

    /**
     * Triggers the connection/login flow.
     */
    suspend fun connect(context: Context): Boolean

    /**
     * Disconnects/logs out from this provider.
     */
    suspend fun disconnect(context: Context)

    /**
     * Fetches steps for the given time range.
     */
    suspend fun getSteps(context: Context, startTime: Instant, endTime: Instant): Long

    /**
     * Fetches the latest heart rate reading.
     */
    suspend fun getLatestHeartRate(context: Context): Int?

    /**
     * Fetches total distance (meters) for the given time range.
     */
    suspend fun getDistance(context: Context, startTime: Instant, endTime: Instant): Double

    /**
     * Fetches total calories (kcal) for the given time range.
     */
    suspend fun getCalories(context: Context, startTime: Instant, endTime: Instant): Double

    /**
     * Fetches latest weight reading (kg).
     */
    suspend fun getLatestWeight(context: Context): Double?

    /**
     * Fetches daily sleep duration (minutes).
     */
    suspend fun getDailySleep(context: Context): Int

    /**
     * Fetches latest stress reading.
     */
    suspend fun getLatestStress(context: Context): Int?

    /**
     * Fetches latest SpO2 reading (percentage).
     */
    suspend fun getLatestSpO2(context: Context): Int?

    /**
     * Fetches latest blood pressure reading (systolic/diastolic).
     */
    suspend fun getLatestBloodPressure(context: Context): Pair<Int, Int>?

    /**
     * Fetches latest skin temperature reading (Celsius).
     */
    suspend fun getLatestSkinTemperature(context: Context): Double?
}
