package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ScanHistoryEntity::class,
        VaultItemEntity::class,
        QuarantineEntity::class,
        TrustedContactEntity::class,
        IncidentEntity::class,
        FeedbackEntity::class,
        TrustedAllowlistEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RakshakDatabase : RoomDatabase() {
    abstract fun rakshakDao(): RakshakDao

    companion object {
        @Volatile
        private var INSTANCE: RakshakDatabase? = null

        fun getInstance(context: Context): RakshakDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RakshakDatabase::class.java,
                    "rakshakai.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
