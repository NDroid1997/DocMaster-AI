package com.ndroid.docmasterai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey
    val id: String,
    val documentId: String,
    val accessedAt: Long = System.currentTimeMillis(),
    val action: String = "view", // view, edit, share, convert
)

@Entity(tableName = "conversion_history")
data class ConversionHistoryEntity(
    @PrimaryKey
    val id: String,
    val sourceFile: String,
    val outputFile: String,
    val conversionType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "completed", // pending, in_progress, completed, failed
    val fileSize: Long = 0L,
)
