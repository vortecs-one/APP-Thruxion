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
    fun isHealthConnected(): Boolean = runBlocking {
        healthManager.providers.any { it.isConnected(healthManager.context) }
    }
}
