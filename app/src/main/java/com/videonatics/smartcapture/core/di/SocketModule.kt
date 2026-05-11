package com.videonatics.smartcapture.core.di

import com.videonatics.smartcapture.core.Pref
import com.videonatics.smartcapture.core.data.remote.websocket.SocketService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SocketModule {

    // A dedicated HttpClient backed by OkHttp — the only Ktor engine on Android
    // that supports WebSocketCapability. The main REST client (NetworkModule) uses
    // the Android engine which explicitly lacks this capability.
    @Provides
    @Singleton
    fun provideSocketService(pref: Pref): SocketService {
        val wsClient = HttpClient(OkHttp) {
            install(WebSockets)
        }
        return SocketService(wsClient, pref)
    }
}
