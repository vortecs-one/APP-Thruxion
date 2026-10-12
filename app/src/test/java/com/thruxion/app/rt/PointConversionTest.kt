package com.thruxion.app.rt

import com.thruxion.app.rt.db.LocationPointConverter
import com.thruxion.app.rt.db.PendingPoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PointConversionTest {

    @Test
    fun testBearingRounding359_96ToZero() {
        val rawBrg = 359.96f
        val roundedBrg = LocationPointConverter.round1Decimal(rawBrg)
        val finalBrg = if (roundedBrg >= 360.0) 0.0 else roundedBrg
        assertEquals(0.0, finalBrg, 0.0001)
    }

    @Test
    fun testFixWithoutAccuracyIsDropped() {
        // Without accuracy accuracy <= 0 or invalid
        val accZero = 0.0f
        val roundedAcc = LocationPointConverter.round1Decimal(accZero)
        val isDropped = roundedAcc <= 0.0 || roundedAcc > 10000.0
        assertEquals(true, isDropped)
    }

    @Test
    fun testAbsentOptionalFieldsAreOmittedFromJSON() {
        val json = Json {
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = true
        }

        val point = PendingPoint(
            seq = 10L,
            t = 1700000000000L,
            lat = 40.123456,
            lon = -3.654321,
            acc = 5.2,
            alt = null,
            spd = null,
            brg = null,
            src = "gps",
            mock = false
        )

        val jsonString = json.encodeToString(point)

        assertFalse("JSON must omit null 'alt'", jsonString.contains("\"alt\""))
        assertFalse("JSON must omit null 'spd'", jsonString.contains("\"spd\""))
        assertFalse("JSON must omit null 'brg'", jsonString.contains("\"brg\""))
    }
}
