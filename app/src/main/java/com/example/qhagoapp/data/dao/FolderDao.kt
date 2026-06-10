package com.example.qhagoapp.data.dao

import androidx.room.*
import com.example.qhagoapp.data.model.Folder
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE userId = :userId ORDER BY isDefault DESC, name ASC")
    fun getAllFolders(userId: String): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE userId = :userId AND name = :name AND type = :type LIMIT 1")
    suspend fun getFolderByName(userId: String, name: String, type: String): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)
}
