package com.example.qhagoapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val name: String,
    val type: String = "PLACE", // "PLACE" or "CONTACT"
    val icon: String? = null,
    val color: String? = null,
    val concept: String? = null,
    val city: String? = null,
    val isShared: Boolean = false
)
