package com.videonatics.smartcapture.feature.auth.presentation.di

import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.feature.auth.data.datasource.remote.AuthRemoteDataSource
import com.videonatics.smartcapture.feature.auth.data.datasource.remote.AuthRemoteDataSourceImpl
import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import com.videonatics.smartcapture.feature.auth.domain.repository.AuthRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthRemoteDataSource(client: HttpClient): AuthRemoteDataSource {
        return AuthRemoteDataSourceImpl(client)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(remote: AuthRemoteDataSource, pref: Pref): AuthRepository {
        return AuthRepositoryImpl(remote, pref)
    }
}
