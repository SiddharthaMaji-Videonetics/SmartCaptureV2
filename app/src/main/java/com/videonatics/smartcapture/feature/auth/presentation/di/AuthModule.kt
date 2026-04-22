package com.videonatics.smartcapture.feature.auth.presentation.di

import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.feature.auth.data.repository.AuthRepository
import com.videonatics.smartcapture.feature.auth.domain.repository.AuthRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideAuthRepository(pref: Pref): AuthRepository {
        return AuthRepositoryImpl(pref)
    }
}