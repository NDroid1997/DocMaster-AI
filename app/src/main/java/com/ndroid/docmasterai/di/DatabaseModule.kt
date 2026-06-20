package com.ndroid.docmasterai.di

import android.content.Context
import androidx.room.Room
import com.ndroid.docmasterai.data.local.dao.*
import com.ndroid.docmasterai.data.local.db.DocMasterDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): DocMasterDatabase = Room.databaseBuilder(
        context,
        DocMasterDatabase::class.java,
        DocMasterDatabase.DATABASE_NAME
    ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideDocumentDao(db: DocMasterDatabase): DocumentDao = db.documentDao()

    @Provides
    fun providePageDao(db: DocMasterDatabase): PageDao = db.pageDao()

    @Provides
    fun provideRecentFileDao(db: DocMasterDatabase): RecentFileDao = db.recentFileDao()

    @Provides
    fun provideConversionHistoryDao(db: DocMasterDatabase): ConversionHistoryDao = db.conversionHistoryDao()
}
