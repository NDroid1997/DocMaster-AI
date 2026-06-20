package com.ndroid.docmasterai.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ndroid.docmasterai.data.local.dao.*
import com.ndroid.docmasterai.data.local.entity.*

@Database(
    entities = [
        DocumentEntity::class,
        PageEntity::class,
        RecentFileEntity::class,
        ConversionHistoryEntity::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class DocMasterDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun pageDao(): PageDao
    abstract fun recentFileDao(): RecentFileDao
    abstract fun conversionHistoryDao(): ConversionHistoryDao

    companion object {
        const val DATABASE_NAME = "docmaster_database"
    }
}
