package com.thruxion.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.thruxion.app.data.dao.ChatMessageDao
import com.thruxion.app.data.dao.ContactDao
import com.thruxion.app.data.dao.FolderDao
import com.thruxion.app.data.dao.SavedPlaceDao
import com.thruxion.app.data.model.ChatMessage
import com.thruxion.app.data.model.Contact
import com.thruxion.app.data.model.Folder
import com.thruxion.app.data.model.SavedPlace

@Database(entities = [Folder::class, SavedPlace::class, Contact::class, ChatMessage::class], version = 12, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun contactDao(): ContactDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qhago_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
