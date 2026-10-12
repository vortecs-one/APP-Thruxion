package com.thruxion.app.rt.ws

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.thruxion.app.BuildConfig
import com.thruxion.app.data.AppDatabase
import com.thruxion.app.rt.TrackingStatus
import com.thruxion.app.rt.auth.RtAuthManager
import com.thruxion.app.rt.db.PendingPoint
import com.thruxion.app.rt.device.RtDeviceManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class RtWebSocketClient private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val authManager = RtAuthManager.getInstance(appContext)
    private val deviceManager = RtDeviceManager(appContext)
    private val pendingPointDao = AppDatabase.getDatabase(appContext).pendingPointDao()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clientMutex = Mutex()

    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var activeWebSocket: WebSocket? = null
    private var senderJob: Job? = null
    private var connectionJob: Job? = null

    private val messageCounter = AtomicLong(0)
    private val pendingResponses = ConcurrentHashMap<String, CompletableDeferred<IncomingMessage>>()

    private val _statusFlow = MutableStateFlow<TrackingStatus>(TrackingStatus.Stopped)
    val statusFlow: StateFlow<TrackingStatus> = _statusFlow.asStateFlow()

    @Volatile
    private var isTrackingActive = false

    @Volatile
    private var backoffAttempt = 0

    @Volatile
    private var isNetworkAvailable = true

    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            isNetworkAvailable = true
            if (isTrackingActive && activeWebSocket == null) {
                scope.launch {
                    reconnectImmediately()
                }
            }
        }

        override fun onLost(network: Network) {
            isNetworkAvailable = false
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            // Ignore if callback fails
        }
    }

    fun startTracking() {
        scope.launch {
            clientMutex.withLock {
                if (isTrackingActive) return@withLock
                isTrackingActive = true
                backoffAttempt = 0
                _statusFlow.value = TrackingStatus.Connecting
                connectWebSocket()
            }
        }
    }

    fun stopTracking() {
        scope.launch {
            clientMutex.withLock {
                isTrackingActive = false
                closeCurrentSocket(1000, "Tracking stopped")
                _statusFlow.value = TrackingStatus.Stopped
            }
        }
    }

    private suspend fun reconnectImmediately() {
        clientMutex.withLock {
            if (!isTrackingActive) return@withLock
            backoffAttempt = 0
            closeCurrentSocket(1000, "Reconnecting immediately")
            connectWebSocket()
        }
    }

    private suspend fun scheduleReconnect(customDelayMs: Long? = null) {
        if (!isTrackingActive) return
        closeCurrentSocket(1000, "Scheduling reconnect")

        val delayMs = customDelayMs ?: run {
            val attempt = backoffAttempt
            val cap = 60_000L
            val base = 1000L * (1L shl attempt.coerceAtMost(6))
            val maxDelay = base.coerceAtMost(cap)
            Random.nextLong(0, maxDelay + 1)
        }

        backoffAttempt++
        val pendingCount = pendingPointDao.getPendingCount()
        _statusFlow.value = TrackingStatus.Offline(pendingCount)

        delay(delayMs)
        if (isTrackingActive) {
            connectWebSocket()
        }
    }

    private suspend fun connectWebSocket() {
        if (!isTrackingActive) return

        val registrationResult = deviceManager.registerDeviceIfNeeded()
        val deviceId = when (registrationResult) {
            is com.thruxion.app.rt.device.DeviceRegistrationResult.Success -> registrationResult.deviceId
            is com.thruxion.app.rt.device.DeviceRegistrationResult.Revoked -> {
                handle403Forbidden()
                return
            }
            else -> {
                val pendingCount = pendingPointDao.getPendingCount()
                _statusFlow.value = TrackingStatus.Offline(pendingCount)
                scheduleReconnect()
                return
            }
        }

        val freshToken = authManager.getFreshAccessToken()
        if (freshToken == null) {
            handle403Forbidden()
            return
        }

        _statusFlow.value = TrackingStatus.Connecting

        val request = Request.Builder()
            .url(BuildConfig.WS_URL)
            .header("Authorization", "Bearer $freshToken")
            .header("X-Device-Id", deviceId)
            .header("Sec-WebSocket-Protocol", "rt.v1.json")
            .build()

        pendingResponses.clear()

        val listener = object : WebSocketListener() {
            private var connectedTimeMs = 0L

            override fun onOpen(webSocket: WebSocket, response: Response) {
                connectedTimeMs = System.currentTimeMillis()
                scope.launch {
                    clientMutex.withLock {
                        activeWebSocket = webSocket
                        _statusFlow.value = TrackingStatus.Live
                        startSenderLoop(webSocket)
                    }
                    delay(60_000)
                    if (isTrackingActive && activeWebSocket == webSocket) {
                        backoffAttempt = 0
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scope.launch {
                    clientMutex.withLock {
                        if (activeWebSocket == webSocket) {
                            activeWebSocket = null
                        }
                    }
                    if (response != null) {
                        when (response.code) {
                            400 -> {
                                if (BuildConfig.DEBUG) Log.e(TAG, "HTTP 400: Client bug")
                                stopTracking()
                                _statusFlow.value = TrackingStatus.SignInRequired
                            }
                            401 -> {
                                val newToken = authManager.getFreshAccessToken(forceRefresh = true)
                                if (newToken != null) {
                                    reconnectImmediately()
                                } else {
                                    scheduleReconnect()
                                }
                            }
                            403 -> {
                                handle403Forbidden()
                            }
                            429 -> {
                                scheduleReconnect(30_000L)
                            }
                            else -> {
                                scheduleReconnect()
                            }
                        }
                    } else {
                        scheduleReconnect()
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scope.launch {
                    clientMutex.withLock {
                        if (activeWebSocket == webSocket) {
                            activeWebSocket = null
                        }
                    }
                    if (!isTrackingActive) return@launch

                    when (code) {
                        1000 -> {
                            scheduleReconnect(1000L)
                        }
                        1003, 1009 -> {
                            scheduleReconnect()
                        }
                        1008 -> {
                            scheduleReconnect(60_000L)
                        }
                        1012 -> {
                            val jitter = Random.nextLong(1000, 5000)
                            scheduleReconnect(jitter)
                        }
                        1013 -> {
                            scheduleReconnect()
                        }
                        4001 -> {
                            val newToken = authManager.getFreshAccessToken(forceRefresh = true)
                            if (newToken != null) {
                                reconnectImmediately()
                            } else {
                                scheduleReconnect()
                            }
                        }
                        4003 -> {
                            handle403Forbidden()
                        }
                        4009 -> {
                            // Replaced by newer connection, do not reconnect from this socket
                            stopTracking()
                        }
                        else -> {
                            scheduleReconnect()
                        }
                    }
                }
            }
        }

        okHttpClient.newWebSocket(request, listener)
    }

    private suspend fun handle403Forbidden() {
        stopTracking()
        val token = authManager.getFreshAccessToken()
        if (token != null) {
            val devices = deviceManager.getActiveDevices()
            val storedId = deviceManager.getStoredDeviceId()
            if (storedId != null && devices.none { it.id == storedId }) {
                val sub = authManager.getSub()
                if (sub != null) {
                    deviceManager.setRevoked(true)
                }
            }
        }
        authManager.clearAuthState()
        _statusFlow.value = TrackingStatus.SignInRequired
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val jsonObj = json.parseToJsonElement(text).jsonObject
            val type = jsonObj["type"]?.jsonPrimitive?.content ?: return
            val ref = jsonObj["ref"]?.jsonPrimitive?.content

            if (type == "session.expiring") {
                scope.launch {
                    handleSessionExpiring()
                }
                return
            }

            if (ref != null) {
                val deferred = pendingResponses.remove(ref)
                if (deferred != null) {
                    deferred.complete(IncomingMessage(type = type, ref = ref, rawJson = jsonObj))
                }
            }

            if (type == "ack") {
                val dataObj = jsonObj["data"]?.jsonObject
                if (dataObj != null) {
                    val seqs = dataObj["seqs"]?.jsonArray?.mapNotNull { it.jsonPrimitive.longOrNull } ?: emptyList()
                    val rejectedSeqs = dataObj["rejected"]?.jsonArray?.mapNotNull {
                        it.jsonObject["seq"]?.jsonPrimitive?.longOrNull
                    } ?: emptyList()

                    val allToDelete = (seqs + rejectedSeqs).distinct()
                    if (allToDelete.isNotEmpty()) {
                        scope.launch {
                            pendingPointDao.deleteBySeqs(allToDelete)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error parsing incoming frame", e)
        }
    }

    private suspend fun handleSessionExpiring() {
        val newToken = authManager.getFreshAccessToken(forceRefresh = true) ?: return
        val msgId = generateMsgId()
        val payload = json.encodeToString(
            WsEnvelope.serializer(AuthRefreshData.serializer()),
            WsEnvelope(v = 1, type = "auth.refresh", id = msgId, data = AuthRefreshData(newToken))
        )

        val deferred = CompletableDeferred<IncomingMessage>()
        pendingResponses[msgId] = deferred

        val socket = activeWebSocket ?: return
        socket.send(payload)

        val response = withTimeoutOrNull(20_000) { deferred.await() }
        pendingResponses.remove(msgId)

        if (response != null && response.type == "error") {
            val code = response.rawJson["data"]?.jsonObject?.get("code")?.jsonPrimitive?.content
            if (code == "auth_failed") {
                closeCurrentSocket(1000, "Auth failed on session refresh")
                reconnectImmediately()
            }
        }
    }

    private fun startSenderLoop(webSocket: WebSocket) {
        senderJob?.cancel()
        senderJob = scope.launch {
            while (isTrackingActive && activeWebSocket == webSocket) {
                val points = pendingPointDao.getOldest(50)
                if (points.isEmpty()) {
                    val count = pendingPointDao.getPendingCount()
                    _statusFlow.value = if (count > 0) TrackingStatus.Offline(count) else TrackingStatus.Live
                    delay(1000)
                    continue
                }

                _statusFlow.value = TrackingStatus.Live

                val isBatch = points.size > 1
                val msgId = generateMsgId()

                val payload = if (isBatch) {
                    json.encodeToString(
                        WsEnvelope.serializer(BatchPointsData.serializer()),
                        WsEnvelope(v = 1, type = "location.batch", id = msgId, data = BatchPointsData(points))
                    )
                } else {
                    json.encodeToString(
                        WsEnvelope.serializer(PendingPoint.serializer()),
                        WsEnvelope(v = 1, type = "location.update", id = msgId, data = points.first())
                    )
                }

                val deferred = CompletableDeferred<IncomingMessage>()
                pendingResponses[msgId] = deferred

                val sent = webSocket.send(payload)
                if (!sent) {
                    pendingResponses.remove(msgId)
                    delay(1000)
                    continue
                }

                val response = withTimeoutOrNull(20_000) { deferred.await() }
                pendingResponses.remove(msgId)

                if (response == null) {
                    // Timeout -> close and reconnect
                    closeCurrentSocket(1000, "Response timeout")
                    scheduleReconnect()
                    break
                }

                if (response.type == "ack") {
                    // Points deleted in onMessage
                } else if (response.type == "error") {
                    val code = response.rawJson["data"]?.jsonObject?.get("code")?.jsonPrimitive?.content
                    when (code) {
                        "rate_limited", "internal" -> {
                            delay(5000)
                        }
                        "invalid_message" -> {
                            if (isBatch) {
                                // Resend one point at a time
                                sendPointsOneByOne(webSocket, points)
                            } else {
                                // Delete single invalid point
                                val singleSeq = points.first().seq
                                pendingPointDao.deleteBySeqs(listOf(singleSeq))
                                if (BuildConfig.DEBUG) Log.e(TAG, "Deleted invalid point seq $singleSeq")
                            }
                        }
                        "auth_failed" -> {
                            val newToken = authManager.getFreshAccessToken(forceRefresh = true)
                            if (newToken != null) {
                                reconnectImmediately()
                            } else {
                                scheduleReconnect()
                            }
                            break
                        }
                    }
                }

                delay(1000) // Minimum 1 s between location messages
            }
        }
    }

    private suspend fun sendPointsOneByOne(webSocket: WebSocket, points: List<PendingPoint>) {
        for (point in points) {
            if (!isTrackingActive || activeWebSocket != webSocket) break
            val msgId = generateMsgId()
            val payload = json.encodeToString(
                WsEnvelope.serializer(PendingPoint.serializer()),
                WsEnvelope(v = 1, type = "location.update", id = msgId, data = point)
            )

            val deferred = CompletableDeferred<IncomingMessage>()
            pendingResponses[msgId] = deferred

            if (!webSocket.send(payload)) {
                pendingResponses.remove(msgId)
                break
            }

            val response = withTimeoutOrNull(20_000) { deferred.await() }
            pendingResponses.remove(msgId)

            if (response == null) break

            if (response.type == "error") {
                val code = response.rawJson["data"]?.jsonObject?.get("code")?.jsonPrimitive?.content
                if (code == "invalid_message") {
                    pendingPointDao.deleteBySeqs(listOf(point.seq))
                    if (BuildConfig.DEBUG) Log.e(TAG, "Deleted single invalid point seq ${point.seq}")
                } else if (code == "rate_limited" || code == "internal") {
                    delay(5000)
                }
            }

            delay(1000)
        }
    }

    private fun closeCurrentSocket(code: Int, reason: String) {
        senderJob?.cancel()
        senderJob = null
        activeWebSocket?.close(code, reason)
        activeWebSocket = null
        pendingResponses.clear()
    }

    private fun generateMsgId(): String {
        return "m-${messageCounter.incrementAndGet()}"
    }

    private data class IncomingMessage(
        val type: String,
        val ref: String?,
        val rawJson: JsonObject
    )

    companion object {
        private const val TAG = "RtWebSocketClient"

        @Volatile
        private var INSTANCE: RtWebSocketClient? = null

        fun getInstance(context: Context): RtWebSocketClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RtWebSocketClient(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
