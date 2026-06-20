package com.ndroid.docmasterai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class PageEntity(
    @PrimaryKey
    val id: String,
    val documentId: String,
    val pageNumber: Int,
    val imagePath: String,
    val enhancedImagePath: String? = null,
    val filterApplied: String = "original",
    val ocrText: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val rotation: Int = 0,
)
