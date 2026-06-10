package com.example.qhagoapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.qhagoapp.data.dao.ContactDao
import com.example.qhagoapp.data.dao.FolderDao
import com.example.qhagoapp.data.dao.SavedPlaceDao
import com.example.qhagoapp.data.model.Contact
import com.example.qhagoapp.data.model.Folder
import com.example.qhagoapp.data.model.SavedPlace

@Database(entities = [Folder::class, SavedPlace::class, Contact::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun contactDao(): ContactDao

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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
