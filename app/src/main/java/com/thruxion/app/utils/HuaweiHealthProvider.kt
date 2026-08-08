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
        
        return try {
            val request = PolymerizeRequest(
                polymerizeWith = listOf(PolymerizeWith("com.huawei.continuous.steps.delta")),
                startTime = startTime.toEpochMilli(),
                endTime = endTime.toEpochMilli()
            )
            
            val response = ApiRegistry.huaweiHealthApi.polymerize("Bearer $token", request)
            if (response.isSuccessful) {
                var totalSteps = 0L
                response.body()?.group?.forEach { group ->
                    group.sampleSet?.forEach { sampleSet ->
                        sampleSet.samplePoints?.forEach { point ->
                            point.value?.forEach { value ->
                                totalSteps += value.intValue ?: 0
                            }
                        }
                    }
                }
                totalSteps
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.e("HuaweiProvider", "Error fetching steps", e)
            0L
        }
    }

    override suspend fun getLatestHeartRate(context: Context): Int? {
        val token = HuaweiAuthManager.getValidAccessToken(context) ?: return null
        val now = Instant.now()
        val oneHourAgo = now.minusSeconds(3600)

        return try {
            val request = PolymerizeRequest(
                polymerizeWith = listOf(PolymerizeWith("com.huawei.instantaneous.heart_rate")),
                startTime = oneHourAgo.toEpochMilli(),
                endTime = now.toEpochMilli()
            )

            val response = ApiRegistry.huaweiHealthApi.polymerize("Bearer $token", request)
            if (response.isSuccessful) {
                response.body()?.group?.lastOrNull()?.sampleSet?.lastOrNull()?.samplePoints?.lastOrNull()?.let { point ->
                    point.value?.lastOrNull()?.fpValue?.toInt()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("HuaweiProvider", "Error fetching heart rate", e)
            null
        }
    }

    override suspend fun getDistance(context: Context, startTime: Instant, endTime: Instant): Double {
        val token = HuaweiAuthManager.getValidAccessToken(context) ?: return 0.0
        return fetchMetric(token, "com.huawei.continuous.distance.delta", startTime, endTime)
    }

    override suspend fun getCalories(context: Context, startTime: Instant, endTime: Instant): Double {
        val token = HuaweiAuthManager.getValidAccessToken(context) ?: return 0.0
        return fetchMetric(token, "com.huawei.continuous.calories.burnt", startTime, endTime)
    }

    private suspend fun fetchMetric(token: String, dataTypeName: String, startTime: Instant, endTime: Instant): Double {
        return try {
            val request = PolymerizeRequest(
                polymerizeWith = listOf(PolymerizeWith(dataTypeName)),
                startTime = startTime.toEpochMilli(),
                endTime = endTime.toEpochMilli()
            )
            val response = ApiRegistry.huaweiHealthApi.polymerize("Bearer $token", request)
            if (response.isSuccessful) {
                var total = 0.0
                response.body()?.group?.forEach { group ->
                    group.sampleSet?.forEach { sampleSet ->
                        sampleSet.samplePoints?.forEach { point ->
                            point.value?.forEach { value ->
                                total += value.fpValue ?: value.intValue?.toDouble() ?: 0.0
                            }
                        }
                    }
                }
                total
            } else 0.0
        } catch (e: Exception) {
            Log.e("HuaweiProvider", "Error fetching $dataTypeName", e)
            0.0
        }
    }
}
