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
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
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
    private var backoffMs: Long = BACKOFF_INITIAL_MS

    // Subscriptions keyed by action type — last one per action wins on reconnect.
    // Guarded by `synchronized` since subscribe* can be called from any coroutine.
    private val activeSubscriptions = LinkedHashMap<String, Map<String, Any>>()

    private val _events = MutableSharedFlow<SocketEvent>(replay = 0)
    val events = _events.asSharedFlow()

    // Parsed flow — consumers filter by TaskStreamEvent.event name.
    val taskEvents: Flow<TaskStreamEvent> = events
        .filterIsInstance<SocketEvent.Message>()
        .mapNotNull { parseTaskEvent(it.data) }

    fun connect() {
        if (connectionJob?.isActive == true) return
        connectionJob = socketScope.launch {
            val token = pref.accessToken
            if (token.isNullOrBlank()) {
                Log.e(TAG, "No token found, skipping connection")
                return@launch
            }
            while (isActive) {
                try {
                    val wsUrl = resolveWsUrl()
                    Log.d(TAG, "Connecting to $wsUrl")
                    session = client.webSocketSession { url(wsUrl) }
                    _events.emit(SocketEvent.Connected)
                    backoffMs = BACKOFF_INITIAL_MS

                    // Re-subscribe to all active channels after (re)connect.
                    resubscribeAll()

                    session?.incoming?.consumeAsFlow()
                        ?.filterIsInstance<Frame.Text>()
                        ?.collect { frame ->
                            val text = frame.readText()
                            Log.d(TAG, "Received: $text")
                            _events.emit(SocketEvent.Message(text))
                        }

                    Log.d(TAG, "Session closed")
                    _events.emit(SocketEvent.Disconnected)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Connection error: ${e.message}")
                    _events.emit(SocketEvent.Error(e.message ?: "Unknown error"))
                }

                session = null
                val delay = backoffMs
                Log.d(TAG, "Reconnecting in ${delay}ms")
                delay(delay)
                // Doubled-with-cap exponential backoff matching the Flutter implementation.
                backoffMs = minOf(backoffMs * 2, BACKOFF_MAX_MS)
            }
        }
    }

    // --- Subscription API ---

    suspend fun subscribeToTask(taskId: String) {
        val payload = mapOf<String, Any>("action" to "subscribe", "taskId" to taskId)
        synchronized(activeSubscriptions) {
            activeSubscriptions["subscribe"] = payload
        }
        sendJson(payload)
    }

    suspend fun subscribeToTasks(taskIds: List<String>) {
        if (taskIds.isEmpty()) return
        val ids = taskIds.distinct().sorted().take(50)
        val payload = mapOf<String, Any>("action" to "subscribe_many", "taskIds" to ids)
        synchronized(activeSubscriptions) {
            activeSubscriptions["subscribe_many"] = payload
        }
        sendJson(payload)
    }

    suspend fun subscribeToUserTasks(userId: String) {
        if (userId.isBlank()) return
        val payload = mapOf<String, Any>("action" to "subscribe_user_tasks", "userId" to userId)
        synchronized(activeSubscriptions) {
            activeSubscriptions["subscribe_user_tasks"] = payload
        }
        sendJson(payload)
    }

    // --- Generic send ---

    suspend fun send(message: String) {
        try {
            session?.send(Frame.Text(message))
        } catch (e: Exception) {
            Log.e(TAG, "Send error: ${e.message}")
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

    // --- Internals ---

    private fun resolveWsUrl(): String {
        val base = pref.baseUrlPref.ifBlank { Constants.BASE_URL }.trimEnd('/')
        val wsBase = when {
            base.startsWith("https://") -> base.replaceFirst("https://", "wss://")
            base.startsWith("http://") -> base.replaceFirst("http://", "ws://")
            else -> "wss://$base"
        }
        val token = pref.accessToken
        val qs = if (!token.isNullOrBlank()) "?token=${URLEncoder.encode(token, "UTF-8")}" else ""
        return "$wsBase/ws$qs"
    }

    private suspend fun resubscribeAll() {
        val subs = synchronized(activeSubscriptions) { activeSubscriptions.values.toList() }
        subs.forEach { sendJson(it) }
    }

    private suspend fun sendJson(payload: Map<String, Any>) {
        try {
            val json = buildJson(payload)
            Log.d(TAG, "Sending: $json")
            session?.send(Frame.Text(json))
        } catch (e: Exception) {
            Log.e(TAG, "Send error: ${e.message}")
        }
    }

    private fun buildJson(payload: Map<String, Any>): String {
        val obj = JSONObject()
        payload.forEach { (key, value) ->
            when (value) {
                is List<*> -> obj.put(key, JSONArray(value))
                else -> obj.put(key, value)
            }
        }
        return obj.toString()
    }

    private fun parseTaskEvent(raw: String): TaskStreamEvent? = try {
        val json = JSONObject(raw)
        val event = json.optString("event", "")
        val data = mutableMapOf<String, Any>()
        json.keys().forEach { key ->
            val v = json.get(key)
            if (v != JSONObject.NULL) data[key] = v
        }
        TaskStreamEvent(event = event, data = data)
    } catch (e: Exception) {
        Log.w(TAG, "Parse error: ${e.message}")
        null
    }

    companion object {
        private const val TAG = "SocketService"
        private const val BACKOFF_INITIAL_MS = 1_000L
        private const val BACKOFF_MAX_MS = 30_000L
    }
}

// --- Models ---

sealed class SocketEvent {
    object Connected : SocketEvent()
    object Disconnected : SocketEvent()
    data class Message(val data: String) : SocketEvent()
    data class Error(val error: String) : SocketEvent()
}

data class TaskStreamEvent(
    val event: String,
    val data: Map<String, Any>
)
