package com.thruxion.app.rt.db

import android.location.Location
import android.os.Build
import kotlin.math.roundToInt

object LocationPointConverter {

    fun round1Decimal(value: Float): Double = (value.toDouble() * 10.0).roundToInt() / 10.0
    fun round1Decimal(value: Double): Double = (value * 10.0).roundToInt() / 10.0
    fun round6Decimals(value: Double): Double = (value * 1000000.0).roundToInt() / 1000000.0

    fun convertLocationToPoint(location: Location): PendingPoint? {
        if (!location.hasAccuracy()) {
            return null
        }

        val acc = round1Decimal(location.accuracy)
        if (acc <= 0.0 || acc > 10000.0) {
            return null
        }

        val lat = round6Decimals(location.latitude)
        val lon = round6Decimals(location.longitude)

        val alt = if (location.hasAltitude()) {
            val roundedAlt = round1Decimal(location.altitude)
            if (roundedAlt in -1000.0..20000.0) roundedAlt else null
        } else null

        val spd = if (location.hasSpeed()) {
            val roundedSpd = round1Decimal(location.speed)
            if (roundedSpd in 0.0..350.0) roundedSpd else null
        } else null

        val brg = if (location.hasBearing()) {
            val roundedBrg = round1Decimal(location.bearing)
            if (roundedBrg >= 360.0) 0.0 else roundedBrg
        } else null

        val src = location.provider ?: "unknown"

        @Suppress("DEPRECATION")
        val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            location.isFromMockProvider
        }

        return PendingPoint(
            seq = 0L,
            t = location.time,
            lat = lat,
            lon = lon,
            acc = acc,
            alt = alt,
            spd = spd,
            brg = brg,
            src = src,
            mock = isMock
        )
    }
}
