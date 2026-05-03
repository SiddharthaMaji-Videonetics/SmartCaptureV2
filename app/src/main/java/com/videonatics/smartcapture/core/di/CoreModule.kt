package com.videonatics.smartcapture.core.di

import android.content.Context
import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.core.data.datasource.local.LocalCoreDataSource
import com.videonatics.smartcapture.core.data.datasource.local.LocalCoreDataSourceImpl
import com.videonatics.smartcapture.core.data.repository.CoreRepositoryImpl
import com.videonatics.smartcapture.core.domain.repository.CoreRepository
import com.videonatics.smartcapture.feature.capturelist.data.repository.CaptureRepositoryImpl
import com.videonatics.smartcapture.feature.capturelist.domain.repository.CaptureRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {

    @Binds
    @Singleton
    abstract fun bindCoreRepository(
        coreRepositoryImpl: CoreRepositoryImpl
    ): CoreRepository

    @Binds
    @Singleton
    abstract fun bindLocalCoreDataSource(
        localCoreDataSourceImpl: LocalCoreDataSourceImpl
    ): LocalCoreDataSource

    @Binds
    @Singleton
    abstract fun bindCaptureRepository(
        captureRepositoryImpl: CaptureRepositoryImpl
    ): CaptureRepository

    companion object {
        @Provides
        @Singleton
        fun providePref(
            @ApplicationContext context: Context
        ): Pref {
            return Pref(context = context)
        }
    }
}
