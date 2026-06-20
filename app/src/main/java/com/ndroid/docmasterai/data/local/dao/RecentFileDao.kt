package com.ndroid.docmasterai.data.local.dao

import androidx.room.*
import com.ndroid.docmasterai.data.local.entity.ConversionHistoryEntity
import com.ndroid.docmasterai.data.local.entity.RecentFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {

    @Query("SELECT * FROM recent_files ORDER BY accessedAt DESC LIMIT :limit")
    fun getRecentFiles(limit: Int = 20): Flow<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(recentFile: RecentFileEntity)

    @Query("DELETE FROM recent_files WHERE accessedAt < :cutoff")
    suspend fun purgeOldEntries(cutoff: Long)

    @Query("DELETE FROM recent_files")
    suspend fun clearAll()
}

@Dao
interface ConversionHistoryDao {

    @Query("SELECT * FROM conversion_history ORDER BY timestamp DESC LIMIT :limit")
    fun getConversionHistory(limit: Int = 50): Flow<List<ConversionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversion(conversion: ConversionHistoryEntity)

    @Query("DELETE FROM conversion_history")
    suspend fun clearAll()
}
