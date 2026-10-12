package com.thruxion.app.rt.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingPointDao {
    @Insert
    suspend fun insert(point: PendingPoint): Long

    @Query("SELECT * FROM pending_points ORDER BY seq ASC LIMIT :limit")
    suspend fun getOldest(limit: Int = 50): List<PendingPoint>

    @Query("DELETE FROM pending_points WHERE seq IN (:seqs)")
    suspend fun deleteBySeqs(seqs: List<Long>)

    @Query("DELETE FROM pending_points WHERE t < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)

    @Query("DELETE FROM pending_points WHERE seq NOT IN (SELECT seq FROM pending_points ORDER BY seq DESC LIMIT 50000)")
    suspend fun trimToMaxRows()

    @Query("SELECT COUNT(*) FROM pending_points")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_points")
    suspend fun getPendingCount(): Int
}
