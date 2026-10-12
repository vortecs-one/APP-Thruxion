package com.thruxion.app.rt.device

import android.content.Context
import android.os.Build
import com.thruxion.app.BuildConfig
import com.thruxion.app.rt.auth.RtAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class RtDeviceManager(context: Context) {
    private val appContext = context.applicationContext
    private val deviceStore = DeviceStore(appContext)
    private val authManager = RtAuthManager.getInstance(appContext)
    private val okHttpClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun getStoredDeviceId(): String? {
        val sub = authManager.getSub() ?: return null
        return deviceStore.getDeviceId(sub)
    }

    fun isRevoked(): Boolean {
        val sub = authManager.getSub() ?: return false
        return deviceStore.isRevoked(sub)
    }

    fun setRevoked(revoked: Boolean) {
        val sub = authManager.getSub() ?: return
        deviceStore.setRevoked(sub, revoked)
    }

    suspend fun registerDeviceIfNeeded(): DeviceRegistrationResult {
        val sub = authManager.getSub()
            ?: return DeviceRegistrationResult.Error("User not logged in")

        if (deviceStore.isRevoked(sub)) {
            return DeviceRegistrationResult.Revoked
        }

        val existingId = deviceStore.getDeviceId(sub)
        if (!existingId.isNullOrEmpty()) {
            return DeviceRegistrationResult.Success(existingId)
        }

        val token = authManager.getFreshAccessToken()
            ?: return DeviceRegistrationResult.Error("Could not obtain access token")

        val sanitizedName = sanitizeDeviceName(Build.MANUFACTURER, Build.MODEL)
        val sanitizedVersion = sanitizeAppVersion(BuildConfig.VERSION_NAME)

        val requestBody = json.encodeToString(
            RegisterDeviceRequest.serializer(),
            RegisterDeviceRequest(name = sanitizedName, appVersion = sanitizedVersion)
        ).toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("${BuildConfig.API_BASE}/v1/devices")
            .header("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = okHttpClient.newCall(request).execute()
                val responseBodyStr = response.body?.string() ?: ""

                when (response.code) {
                    201 -> {
                        val deviceDto = json.decodeFromString(DeviceDto.serializer(), responseBodyStr)
                        deviceStore.saveDeviceId(sub, deviceDto.id)
                        DeviceRegistrationResult.Success(deviceDto.id)
                    }
                    409 -> {
                        val activeDevices = fetchActiveDevices(token)
                        DeviceRegistrationResult.DeviceLimitReached(activeDevices)
                    }
                    429 -> {
                        DeviceRegistrationResult.RateLimited
                    }
                    403 -> {
                        deviceStore.setRevoked(sub, true)
                        DeviceRegistrationResult.Revoked
                    }
                    else -> {
                        DeviceRegistrationResult.Error("Server error ${response.code}: $responseBodyStr")
                    }
                }
            } catch (e: Exception) {
                DeviceRegistrationResult.Error(e.message ?: "Network error during device registration")
            }
        }
    }

    suspend fun getActiveDevices(): List<DeviceDto> {
        val token = authManager.getFreshAccessToken() ?: return emptyList()
        return fetchActiveDevices(token)
    }

    private suspend fun fetchActiveDevices(token: String): List<DeviceDto> {
        val request = Request.Builder()
            .url("${BuildConfig.API_BASE}/v1/devices")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val listResponse = json.decodeFromString(DevicesListResponse.serializer(), bodyStr)
                    listResponse.devices.filter { it.status == "active" }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun deleteDevice(deviceId: String): Boolean {
        val token = authManager.getFreshAccessToken() ?: return false
        val request = Request.Builder()
            .url("${BuildConfig.API_BASE}/v1/devices/$deviceId")
            .header("Authorization", "Bearer $token")
            .delete()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = okHttpClient.newCall(request).execute()
                response.code == 204 || response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
    }

    companion object {
        fun sanitizeDeviceName(manufacturer: String, model: String): String {
            val raw = "$manufacturer $model"
            val filtered = raw.replace(Regex("[^a-zA-Z0-9 _.'-]"), " ")
            val collapsed = filtered.replace(Regex("\\s+"), " ").trim()
            return collapsed.take(64)
        }

        fun sanitizeAppVersion(versionName: String): String {
            val filtered = versionName.replace(Regex("[^0-9A-Za-z.+-]"), "")
            return filtered.take(32)
        }
    }
}
