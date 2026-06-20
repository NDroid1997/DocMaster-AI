package com.ndroid.docmasterai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val filePath: String,
    val thumbnailPath: String? = null,
    val fileType: String, // pdf, jpg, png, etc.
    val fileSize: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val tags: String? = null,
    val folderPath: String = "",
    val pageCount: Int = 1,
    val sourceType: String = "scan", // scan, import, convert
)
