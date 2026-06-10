package com.example.qhagoapp.data.dao

import androidx.room.*
import com.example.qhagoapp.data.model.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE userId = :userId")
    fun getAllContacts(userId: String): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE folderId = :folderId AND userId = :userId")
    fun getContactsInFolder(folderId: Long, userId: String): Flow<List<Contact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)
}
