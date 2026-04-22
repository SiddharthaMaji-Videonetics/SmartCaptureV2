package com.videonatics.smartcapture.feature.task.presentation.di

import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.feature.task.data.datasource.remote.TaskRemoteDataSource
import com.videonatics.smartcapture.feature.task.data.datasource.remote.TaskRemoteDataSourceImpl
import com.videonatics.smartcapture.feature.task.data.repository.TaskRepositoryImpl
import com.videonatics.smartcapture.feature.task.domain.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TaskModule {

    @Provides
    @Singleton
    fun provideTaskRemoteDataSource(
        client: HttpClient,
        pref: Pref
    ): TaskRemoteDataSource {
        return TaskRemoteDataSourceImpl(client, pref)
    }

    @Provides
    @Singleton
    fun provideTaskRepository(
        remoteDataSource: TaskRemoteDataSource
    ): TaskRepository {
        return TaskRepositoryImpl(remoteDataSource)
    }
}
