package com.thruxion.app.utils

import android.content.Context
import android.util.Log
import com.thruxion.app.network.ApiRegistry
import com.thruxion.app.network.api.PolymerizeRequest
import com.thruxion.app.network.api.PolymerizeWith
import java.time.Instant

/**
 * Direct implementation for Huawei Health Kit REST API.
 */
class HuaweiHealthProvider : WearableProvider {
    override val id: String = "huawei"
    override val displayName: String = "Huawei Health"

    override suspend fun isConnected(context: Context): Boolean {
        return HuaweiAuthManager.getValidAccessToken(context) != null
    }

    override suspend fun connect(context: Context): Boolean {
        HuaweiAuthManager.startLogin(context)
        return true
    }

    override suspend fun disconnect(context: Context) {
        HuaweiAuthManager.disconnect(context)
    }

    override suspend fun getSteps(context: Context, startTime: Instant, endTime: Instant): Long {
        val token = HuaweiAuthManager.getValidAccessToken(context) ?: return 0L
        return fetchMetric(token, "com.huawei.continuous.steps.delta", "steps", startTime, endTime).toLong()
    }

    override suspend fun getLatestHeartRate(context: Context): Int? {
        val token = HuaweiAuthManager.getValidAccessToken(context) ?: return null
        val now = Instant.now()
        val searchStart = now.minusSeconds(86400) // Look back 24 hours

        return try {
            val request = PolymerizeRequest(
                polymerizeWith = listOf(PolymerizeWith("com.huawei.instantaneous.heart_rate")),
                startTime = searchStart.toEpochMilli(),
                endTime = now.toEpochMilli()
            )

            val response = ApiRegistry.huaweiHealthApi.polymerize("Bearer $token", request)
            Log.d("HuaweiProvider", "Heart Rate Response Status: ${response.code()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d("HuaweiProvider", "Heart Rate Body: $body")
                // Find the latest valid heart rate point
                body?.group?.flatMap { it.sampleSet ?: emptyList() }
                    ?.flatMap { it.samplePoints ?: emptyList() }
                    ?.lastOrNull()?.value?.let { values ->
                        values.find { it.fieldName == "bpm" }?.floatValue?.toInt()
                            ?: values.firstOrNull()?.let { it.floatValue?.toInt() ?: it.integerValue }
                    }
            } else {
                Log.e("HuaweiProvider", "Heart Rate Error Body: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("HuaweiProvider", "Exception fetching heart rate", e)
            null
        }
    }

    override suspend fun getDistance(context: Context, startTime: Instant, endTime: Instant): Double {
        val token = HuaweiAuthManager.getValidAccessToken(context) ?: return 0.0
        return fetchMetric(token, "com.huawei.continuous.distance.delta", "distance", startTime, endTime)
    }

    override suspend fun getCalories(context: Context, startTime: Instant, endTime: Instant): Double {
        val token = HuaweiAuthManager.getValidAccessToken(context) ?: return 0.0
        return fetchMetric(token, "com.huawei.continuous.calories.burnt", "calories", startTime, endTime)
    }

    private suspend fun fetchMetric(token: String, dataTypeName: String, fieldName: String, startTime: Instant, endTime: Instant): Double {
        return try {
            val request = PolymerizeRequest(
                polymerizeWith = listOf(PolymerizeWith(dataTypeName)),
                startTime = startTime.toEpochMilli(),
                endTime = endTime.toEpochMilli(),
                groupByTime = com.thruxion.app.network.api.GroupByTime(
                    groupPeriod = com.thruxion.app.network.api.GroupPeriod("day", 1, "+0000")
                )
            )
            val response = ApiRegistry.huaweiHealthApi.polymerize("Bearer $token", request)
            Log.d("HuaweiProvider", "Fetching metrics for $dataTypeName. Status: ${response.code()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d("HuaweiProvider", "Raw JSON for $dataTypeName: $body")
                var total = 0.0
                body?.group?.forEach { group ->
                    group.sampleSet?.forEach { sampleSet ->
                        sampleSet.samplePoints?.forEach { point ->
                            point.value?.forEach { value ->
                                // Resilience: Try specific field first, then fallback to any available number
                                if (value.fieldName == fieldName || fieldName.isEmpty()) {
                                    total += value.floatValue ?: value.integerValue?.toDouble() ?: 0.0
                                } else if (value.floatValue != null || value.integerValue != null) {
                                    total += value.floatValue ?: value.integerValue?.toDouble() ?: 0.0
                                }
                            }
                        }
                    }
                }
                total
            } else {
                Log.e("HuaweiProvider", "Error body for $dataTypeName: ${response.errorBody()?.string()}")
                0.0
            }
        } catch (e: Exception) {
            Log.e("HuaweiProvider", "Exception fetching $dataTypeName", e)
            0.0
        }
    }

    override suspend fun getLatestWeight(context: Context): Double? {
        val token = HuaweiAuthManager.getValidAccessToken(context) ?: return null
        return fetchLatestValue(token, "com.huawei.instantaneous.body_weight", "weight")
    }

    override suspend fun getDailySleep(context: Context): Int {
        val token = HuaweiAuthManager.getValidAccessToken(context) ?: return 0
        // Sleep often uses a different polymerization logic or fragments. 
        // For simplicity, let's try to get the daily aggregated sleep if available.
        val startOfDay = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
        val now = Instant.now()
        
        return try {
            val request = PolymerizeRequest(
                polymerizeWith = listOf(PolymerizeWith("com.huawei.continuous.sleep.fragment")),
                startTime = startOfDay.toEpochMilli(),
                endTime = now.toEpochMilli()
            )
            val response = ApiRegistry.huaweiHealthApi.polymerize("Bearer $token", request)
            if (response.isSuccessful) {
                var totalMinutes = 0
                response.body()?.group?.forEach { group ->
                    group.sampleSet?.forEach { set ->
                        set.samplePoints?.forEach { point ->
                            // Calculate duration of each fragment
                            val duration = (point.endTime - point.startTime) / 60000
                            totalMinutes += duration.toInt()
                        }
                    }
                }
                totalMinutes
            } else 0
        } catch (e: Exception) { 0 }
    }

    override suspend fun getLatestStress(context: Context): Int? {
        val token = HuaweiAuthManager.getValidAccessToken(context) ?: return null
        return fetchLatestValue(token, "com.huawei.instantaneous.stress", "stress_score")?.toInt()
    }

    override suspend fun getLatestSpO2(context: Context): Int? {
        val token = HuaweiAuthManager.getValidAccessToken(context) ?: return null
        return fetchLatestValue(token, "com.huawei.instantaneous.spo2", "spo2")?.toInt()
    }

    override suspend fun getLatestBloodPressure(context: Context): Pair<Int, Int>? {
        val token = HuaweiAuthManager.getValidAccessToken(context) ?: return null
        val now = Instant.now()
        val searchStart = now.minusSeconds(86400)
        
        try {
            val request = PolymerizeRequest(
                polymerizeWith = listOf(PolymerizeWith("com.huawei.instantaneous.blood_pressure")),
                startTime = searchStart.toEpochMilli(),
                endTime = now.toEpochMilli()
            )
            val response = ApiRegistry.huaweiHealthApi.polymerize("Bearer $token", request)
            if (response.isSuccessful) {
                val point = response.body()?.group?.flatMap { it.sampleSet ?: emptyList() }
                    ?.flatMap { it.samplePoints ?: emptyList() }
                    ?.lastOrNull()
                
                val values = point?.value
                val systolic = values?.find { it.fieldName == "systolic" }?.let { it.floatValue ?: it.integerValue?.toDouble() }?.toInt()
                val diastolic = values?.find { it.fieldName == "diastolic" }?.let { it.floatValue ?: it.integerValue?.toDouble() }?.toInt()
                
                if (systolic != null && diastolic != null) return Pair(systolic, diastolic)
            }
        } catch (e: Exception) {}
        return null
    }

    override suspend fun getLatestSkinTemperature(context: Context): Double? {
        val token = HuaweiAuthManager.getValidAccessToken(context) ?: return null
        return fetchLatestValue(token, "com.huawei.instantaneous.skin_temperature", "skin_temperature")
    }

    private suspend fun fetchLatestValue(token: String, dataTypeName: String, fieldName: String): Double? {
        val now = Instant.now()
        val searchStart = now.minusSeconds(86400)

        return try {
            val request = PolymerizeRequest(
                polymerizeWith = listOf(PolymerizeWith(dataTypeName)),
                startTime = searchStart.toEpochMilli(),
                endTime = now.toEpochMilli()
            )
            val response = ApiRegistry.huaweiHealthApi.polymerize("Bearer $token", request)
            Log.d("HuaweiProvider", "Fetching latest value for $dataTypeName. Status: ${response.code()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d("HuaweiProvider", "Raw JSON for latest $dataTypeName: $body")
                body?.group?.flatMap { it.sampleSet ?: emptyList() }
                    ?.flatMap { it.samplePoints ?: emptyList() }
                    ?.lastOrNull()?.value?.let { values ->
                        // Try matching fieldName, otherwise return the first available number
                        values.find { it.fieldName == fieldName }?.let { it.floatValue ?: it.integerValue?.toDouble() }
                            ?: values.firstOrNull()?.let { it.floatValue ?: it.integerValue?.toDouble() }
                    }
            } else {
                Log.e("HuaweiProvider", "Error body for latest $dataTypeName: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("HuaweiProvider", "Exception fetching latest $dataTypeName", e)
            null
        }
    }
}
