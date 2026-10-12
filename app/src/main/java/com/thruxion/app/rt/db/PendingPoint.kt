package com.thruxion.app.rt.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "pending_points")
@Serializable
data class PendingPoint(
    @PrimaryKey(autoGenerate = true)
    val seq: Long = 0L,
    val t: Long,
    val lat: Double,
    val lon: Double,
    val acc: Double,
    val alt: Double? = null,
    val spd: Double? = null,
    val brg: Double? = null,
    val src: String,
    val mock: Boolean
)
