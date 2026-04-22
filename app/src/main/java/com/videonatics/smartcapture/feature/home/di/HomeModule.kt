package com.videonatics.smartcapture.feature.home.di

import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.feature.home.data.datasource.remote.HomeRemoteDataSource
import com.videonatics.smartcapture.feature.home.data.datasource.remote.HomeRemoteDataSourceImpl
import com.videonatics.smartcapture.feature.home.data.repository.HomeRepositoryImpl
import com.videonatics.smartcapture.feature.home.domain.repository.HomeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeModule {

    @Provides
    @Singleton
    fun provideHomeRemoteDataSource(
        client: HttpClient,
        pref: Pref
    ): HomeRemoteDataSource {
        return HomeRemoteDataSourceImpl(client, pref)
    }

    @Provides
    @Singleton
    fun provideHomeRepository(
        remoteDataSource: HomeRemoteDataSource
    ): HomeRepository {
        return HomeRepositoryImpl(remoteDataSource)
    }
}
