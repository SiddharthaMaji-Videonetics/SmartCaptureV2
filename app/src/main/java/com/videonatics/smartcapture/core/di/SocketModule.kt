package com.videonatics.smartcapture.core.di

import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.core.data.remote.websocket.SocketService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SocketModule {

    @Provides
    @Singleton
    fun provideSocketService(client: HttpClient, pref: Pref): SocketService {
        return SocketService(client, pref)
    }
}
