package com.zayaanify.infinitewhiteboard.di

import android.content.Context
import androidx.room.Room
import com.zayaanify.infinitewhiteboard.data.local.BoardPageDao
import com.zayaanify.infinitewhiteboard.data.local.WhiteboardDatabase
import com.zayaanify.infinitewhiteboard.data.repository.WhiteboardRepository
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
    fun provideDatabase(@ApplicationContext context: Context): WhiteboardDatabase {
        return WhiteboardDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideBoardPageDao(database: WhiteboardDatabase): BoardPageDao {
        return database.boardPageDao()
    }

    @Provides
    @Singleton
    fun provideWhiteboardRepository(dao: BoardPageDao): WhiteboardRepository {
        return WhiteboardRepository(dao)
    }
}