package com.thruxion.app.data.dao

import androidx.room.*
import com.thruxion.app.data.model.Folder
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE userId = :userId ORDER BY CASE WHEN systemTag LIKE 'FAV_%' THEN 0 WHEN isDefault = 1 THEN 1 ELSE 2 END, name ASC")
    fun getAllFolders(userId: String): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE userId = :userId AND name = :name AND type = :type LIMIT 1")
    suspend fun getFolderByName(userId: String, name: String, type: String): Folder?

    @Query("SELECT * FROM folders WHERE userId = :userId AND systemTag = :tag LIMIT 1")
    suspend fun getFolderByTag(userId: String, tag: String): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)
}
