package com.ndroid.docmasterai.data.local.dao

import androidx.room.*
import com.ndroid.docmasterai.data.local.entity.PageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    fun getPagesByDocumentId(documentId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE id = :id")
    suspend fun getPageById(id: String): PageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>)

    @Update
    suspend fun updatePage(page: PageEntity)

    @Query("DELETE FROM pages WHERE id = :id")
    suspend fun deletePage(id: String)

    @Query("DELETE FROM pages WHERE documentId = :documentId")
    suspend fun deletePagesByDocumentId(documentId: String)

    @Query("SELECT COUNT(*) FROM pages WHERE documentId = :documentId")
    suspend fun getPageCount(documentId: String): Int
}
