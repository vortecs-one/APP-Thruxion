package com.thruxion.app.rt.device

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequest(
    val name: String,
    val appVersion: String
)

@Serializable
data class DeviceDto(
    val id: String,
    val name: String,
    val appVersion: String,
    val status: String,
    val createdAt: String? = null,
    val lastSeenAt: String? = null
)

@Serializable
data class DevicesListResponse(
    val devices: List<DeviceDto> = emptyList()
)

@Serializable
data class ErrorResponse(
    val error: String,
    val max: Int? = null
)

sealed interface DeviceRegistrationResult {
    data class Success(val deviceId: String) : DeviceRegistrationResult
    data class DeviceLimitReached(val activeDevices: List<DeviceDto>) : DeviceRegistrationResult
    object Revoked : DeviceRegistrationResult
    object RateLimited : DeviceRegistrationResult
    data class Error(val message: String) : DeviceRegistrationResult
}
