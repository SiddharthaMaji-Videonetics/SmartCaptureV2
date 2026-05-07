package com.videonatics.smartcapture.feature.analysis.presenation.di

import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.feature.analysis.data.datasource.remote.AnalysisRemoteDataSource
import com.videonatics.smartcapture.feature.analysis.data.datasource.remote.AnalysisRemoteDataSourceImpl
import com.videonatics.smartcapture.feature.analysis.data.repository.AnalysisRepositoryImpl
import com.videonatics.smartcapture.feature.analysis.domain.repository.AnalysisRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalysisModule {

    @Provides
    @Singleton
    fun provideAnalysisDataSource(client: HttpClient, pref: Pref): AnalysisRemoteDataSource {
        return AnalysisRemoteDataSourceImpl(client,pref)
    }


    @Provides
    @Singleton
    fun provideAnalysisRepo(dataSource: AnalysisRemoteDataSource): AnalysisRepository {
        return AnalysisRepositoryImpl(dataSource)
    }
}
