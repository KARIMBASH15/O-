package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FtpConnectionEntity::class,
        SyncJobEntity::class,
        FileBookmarkEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ftpConnectionDao(): FtpConnectionDao
    abstract fun syncJobDao(): SyncJobDao
    abstract fun fileBookmarkDao(): FileBookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "file_master_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
