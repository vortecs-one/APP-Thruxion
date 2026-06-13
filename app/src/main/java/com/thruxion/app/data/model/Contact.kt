package com.thruxion.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("folderId")]
)
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String, // The owner of the contact
    val name: String,
    val avatarIndex: Int,
    val latitude: Double,
    val longitude: Double,
    val folderId: Long,
    val remoteUserId: String? = null,
    val phone: String? = null,
    val email: String? = null
)
