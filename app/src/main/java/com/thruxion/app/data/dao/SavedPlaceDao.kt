package com.thruxion.app.data.dao

import androidx.room.*
import com.thruxion.app.data.model.SavedPlace
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_places WHERE userId = :userId")
    fun getAllSavedPlaces(userId: String): Flow<List<SavedPlace>>

    @Query("SELECT * FROM saved_places WHERE folderId = :folderId AND userId = :userId")
    fun getPlacesInFolder(folderId: Long, userId: String): Flow<List<SavedPlace>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: SavedPlace)

    @Delete
    suspend fun deletePlace(place: SavedPlace)
}
