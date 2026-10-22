package com.purelilith.networkscanner.data.base

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DeviceEntity::class, ScanEntity::class, SightingEntity::class],
    version = 1,
    exportSchema = false,
)

abstract class AppDatabase : RoomDatabase() {
    companion object {
        private var instance: AppDatabase? = null

        @Synchronized
        fun get(context: Context): AppDatabase {
            return instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "scanner.db"
            ).build().also { instance = it }
        }
    }
}