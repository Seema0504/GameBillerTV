package com.gamebiller.tvlock.di

import android.content.Context
import androidx.room.Room
import com.gamebiller.tvlock.data.local.AppDatabase
import com.gamebiller.tvlock.data.local.dao.AuditDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gamebiller_tv_db"
        ).build()
    }

    @Provides
    fun provideAuditDao(database: AppDatabase): AuditDao {
        return database.auditDao()
    }
}
