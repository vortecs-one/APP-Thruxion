package com.thruxion.app.ui.healthy

import android.webkit.JavascriptInterface
import com.thruxion.app.utils.HealthManager
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Bridge between the Healthy WebView and native Health data providers.
 */
class HealthyJavascriptInterface(private val healthManager: HealthManager) {

    @JavascriptInterface
    fun getDailySteps(): Long = runBlocking {
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()
        healthManager.readTotalSteps(startOfDay, now)
    }

    @JavascriptInterface
    fun getHeartRate(): Int = runBlocking {
        healthManager.readLatestHeartRate() ?: 0
    }

    @JavascriptInterface
    fun getDailyDistance(): Double = runBlocking {
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()
        healthManager.readTotalDistance(startOfDay, now)
    }

    @JavascriptInterface
    fun getDailyCalories(): Double = runBlocking {
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()
        healthManager.readTotalCalories(startOfDay, now)
    }

    @JavascriptInterface
    fun getWeight(): Double = runBlocking {
        healthManager.readLatestWeight() ?: 0.0
    }

    @JavascriptInterface
    fun getSleepDuration(): Int = runBlocking {
        healthManager.readDailySleep()
    }

    @JavascriptInterface
    fun getStressScore(): Int = runBlocking {
        healthManager.readLatestStress() ?: 0
    }

    @JavascriptInterface
    fun getSpO2(): Int = runBlocking {
        healthManager.readLatestSpO2() ?: 0
    }

    @JavascriptInterface
    fun getBloodPressure(): String = runBlocking {
        healthManager.readLatestBloodPressure()?.let { "${it.first}/${it.second}" } ?: ""
    }

    @JavascriptInterface
    fun getSkinTemperature(): Double = runBlocking {
        healthManager.readLatestSkinTemperature() ?: 0.0
    }

    @JavascriptInterface
    fun isHealthConnected(): Boolean = runBlocking {
        healthManager.providers.any { it.isConnected(healthManager.context) }
    }
}
