package com.videonatics.smartcapture.core.data.remote.websocket

import android.util.Log
import com.videonatics.smartcapture.core.Constants
import com.videonatics.smartcapture.core.Pref
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketService @Inject constructor(
    private val client: HttpClient,
    private val pref: Pref
) {
    private var session: WebSocketSession? = null
    private val socketScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionJob: Job? = null
    
    private val _events = MutableSharedFlow<SocketEvent>(replay = 0)
    val events = _events.asSharedFlow()

    fun connect() {
        if (session != null && session?.isActive == true) return
        
        connectionJob?.cancel()
        connectionJob = socketScope.launch {
            val token = pref.accessToken
            if (token.isNullOrBlank()) {
                Log.e("SocketService", "No token found, skipping connection")
                return@launch
            }
            
            while (isActive) {
                try {
                    Log.d("SocketService", "Connecting to ${Constants.WS_BASE_URL}")
                    session = client.webSocketSession {
                        url("${Constants.WS_BASE_URL}?token=$token")
                    }
                    
                    _events.emit(SocketEvent.Connected)
                    Log.d("SocketService", "Connected")

                    session?.incoming?.consumeAsFlow()
                        ?.filterIsInstance<Frame.Text>()
                        ?.mapNotNull { it.readText() }
                        ?.collect { text ->
                            Log.d("SocketService", "Received: $text")
                            _events.emit(SocketEvent.Message(text))
                        }
                    
                    Log.d("SocketService", "Session closed")
                    _events.emit(SocketEvent.Disconnected)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e("SocketService", "Connection error: ${e.message}")
                    _events.emit(SocketEvent.Error(e.message ?: "Unknown error"))
                }
                
                session = null
                delay(5000) // Retry delay
            }
        }
    }

    suspend fun send(message: String) {
        try {
            session?.send(Frame.Text(message))
        } catch (e: Exception) {
            Log.e("SocketService", "Send error: ${e.message}")
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        socketScope.launch {
            session?.close()
            session = null
            _events.emit(SocketEvent.Disconnected)
        }
    }
}

sealed class SocketEvent {
    object Connected : SocketEvent()
    object Disconnected : SocketEvent()
    data class Message(val data: String) : SocketEvent()
    data class Error(val error: String) : SocketEvent()
}
