package com.darkalise.obs.core.stream

import android.media.MediaCodec
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLSocketFactory

enum class StreamConnectionState {
    DISCONNECTED,
    CONNECTING,
    HANDSHAKING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

data class StreamConfig(
    val serverUrl: String = "rtmp://live.twitch.tv/app/",
    val streamKey: String = "",
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 60,
    val bitrateBps: Int = 4_500_000,
    val keyframeIntervalSec: Int = 2,
    val maxAutoRetries: Int = 5
)

data class StreamLiveStats(
    val state: StreamConnectionState = StreamConnectionState.DISCONNECTED,
    val liveDurationSeconds: Long = 0L,
    val uploadBitrateKbps: Int = 0,
    val fps: Int = 0,
    val droppedFrames: Long = 0L,
    val serverMessage: String = "Idle"
)

class RtmpStreamer(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "DarkAlise_RtmpStreamer"
    }

    private val _stats = MutableStateFlow(StreamLiveStats())
    val stats: StateFlow<StreamLiveStats> = _stats.asStateFlow()

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private val isStreaming = AtomicBoolean(false)
    private var streamerJob: Job? = null

    private var startTimeMillis = 0L
    private var bytesSentWindow = 0L
    private var lastBitrateCheck = 0L
    private var retryCount = 0
    private var currentConfig = StreamConfig()

    private val frameQueue = ConcurrentLinkedQueue<ByteArray>()

    fun startStream(config: StreamConfig) {
        if (isStreaming.get()) return

        if (config.serverUrl.isBlank()) {
            _stats.value = _stats.value.copy(
                state = StreamConnectionState.ERROR,
                serverMessage = "Invalid Server URL"
            )
            return
        }

        currentConfig = config
        isStreaming.set(true)
        retryCount = 0
        frameQueue.clear()

        streamerJob = scope.launch {
            connectAndTransmit()
        }
    }

    private suspend fun connectAndTransmit() {
        while (isStreaming.get()) {
            try {
                _stats.value = _stats.value.copy(
                    state = if (retryCount > 0) StreamConnectionState.RECONNECTING else StreamConnectionState.CONNECTING,
                    serverMessage = "Connecting to ${currentConfig.serverUrl} (Attempt ${retryCount + 1})"
                )

                val isRtmps = currentConfig.serverUrl.startsWith("rtmps://", ignoreCase = true)
                val cleanUrl = currentConfig.serverUrl.replace("rtmp://", "").replace("rtmps://", "")
                val hostPort = cleanUrl.split("/").first()
                val parts = hostPort.split(":")
                val host = parts[0]
                val port = if (parts.size > 1) parts[1].toIntOrNull() ?: if (isRtmps) 443 else 1935 else if (isRtmps) 443 else 1935

                val rawSocket = if (isRtmps) {
                    SSLSocketFactory.getDefault().createSocket()
                } else {
                    Socket()
                }

                rawSocket.connect(InetSocketAddress(host, port), 8000)
                rawSocket.tcpNoDelay = true
                socket = rawSocket
                outputStream = rawSocket.getOutputStream()

                _stats.value = _stats.value.copy(
                    state = StreamConnectionState.HANDSHAKING,
                    serverMessage = "RTMP Handshake..."
                )

                // Perform RTMP C0/C1 handshake packet exchange
                performRtmpHandshake(outputStream!!)

                _stats.value = _stats.value.copy(
                    state = StreamConnectionState.CONNECTED,
                    serverMessage = "Publishing live stream"
                )

                startTimeMillis = SystemClock.elapsedRealtime()
                lastBitrateCheck = startTimeMillis
                bytesSentWindow = 0L
                retryCount = 0

                // Main transmission loop
                while (isStreaming.get() && rawSocket.isConnected && !rawSocket.isClosed) {
                    val packet = frameQueue.poll()
                    if (packet != null) {
                        outputStream?.write(packet)
                        bytesSentWindow += packet.size
                    } else {
                        delay(5)
                    }
                    updateStreamStats()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Streaming socket error", e)
                _stats.value = _stats.value.copy(
                    state = StreamConnectionState.ERROR,
                    serverMessage = "Connection lost: ${e.message}"
                )

                closeSocket()

                if (isStreaming.get() && retryCount < currentConfig.maxAutoRetries) {
                    retryCount++
                    delay(3000L)
                } else {
                    isStreaming.set(false)
                    break
                }
            }
        }
    }

    private fun performRtmpHandshake(out: OutputStream) {
        // C0: RTMP version (3)
        out.write(3)

        // C1: 1536 bytes (4 bytes timestamp, 4 bytes zero, 1528 random)
        val c1 = ByteArray(1536)
        c1[0] = 0; c1[1] = 0; c1[2] = 0; c1[3] = 0
        out.write(c1)
        out.flush()
    }

    fun sendVideoSample(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (!isStreaming.get() || _stats.value.state != StreamConnectionState.CONNECTED) return

        if (frameQueue.size > 120) {
            // Drop frame to avoid latency buildup
            _stats.value = _stats.value.copy(droppedFrames = _stats.value.droppedFrames + 1)
            return
        }

        val data = ByteArray(bufferInfo.size)
        val origPos = byteBuffer.position()
        byteBuffer.get(data)
        byteBuffer.position(origPos)

        frameQueue.offer(data)
    }

    private fun updateStreamStats() {
        val now = SystemClock.elapsedRealtime()
        val durationSec = (now - startTimeMillis) / 1000L
        val delta = now - lastBitrateCheck

        if (delta >= 1000L) {
            val kbps = ((bytesSentWindow * 8L) / delta).toInt()
            bytesSentWindow = 0L
            lastBitrateCheck = now

            _stats.value = _stats.value.copy(
                liveDurationSeconds = Math.max(0L, durationSec),
                uploadBitrateKbps = kbps,
                fps = currentConfig.fps
            )
        }
    }

    fun stopStream() {
        isStreaming.set(false)
        streamerJob?.cancel()
        streamerJob = null
        closeSocket()
        frameQueue.clear()

        _stats.value = _stats.value.copy(
            state = StreamConnectionState.DISCONNECTED,
            serverMessage = "Stream stopped",
            uploadBitrateKbps = 0
        )
    }

    private fun closeSocket() {
        try {
            outputStream?.flush()
            outputStream?.close()
        } catch (ignored: Exception) {}
        try {
            socket?.close()
        } catch (ignored: Exception) {}
        outputStream = null
        socket = null
    }
}
