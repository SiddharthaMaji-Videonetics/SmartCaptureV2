package com.videonatics.smartcapture.core.di

import android.content.Context
import androidx.room.Room
import com.videonatics.smartcapture.core.data.local.AppDatabase
import com.videonatics.smartcapture.core.data.local.dao.CaptureDao
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
            "smart_capture_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideCaptureDao(database: AppDatabase): CaptureDao {
        return database.captureDao()
    }
}
