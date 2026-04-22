package com.videonatics.smartcapture.feature.auth.presentation.di

import com.videonatics.smartcapture.core.Pref
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
    fun provideAuthRepository(client: HttpClient, pref: Pref): AuthRepository {
        return AuthRepositoryImpl(client, pref)
    }
}
