package com.videonatics.smartcapture.feature.language.di

import com.videonatics.smartcapture.feature.language.data.repository.LanguageRepositoryImpl
import com.videonatics.smartcapture.feature.language.domain.repository.LanguageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LanguageModule {

    @Binds
    @Singleton
    abstract fun bindLanguageRepository(
        languageRepositoryImpl: LanguageRepositoryImpl
    ): LanguageRepository
}
