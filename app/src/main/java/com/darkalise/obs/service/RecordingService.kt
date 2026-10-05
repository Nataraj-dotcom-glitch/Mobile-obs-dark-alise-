package com.darkalise.obs.service

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.darkalise.obs.core.audio.AudioCaptureManager
import com.darkalise.obs.core.audio.AudioMixer
import com.darkalise.obs.core.capture.ScreenCaptureManager
import com.darkalise.obs.core.encoder.VideoEncoder
import com.darkalise.obs.core.notification.NotificationHelper
import com.darkalise.obs.core.recording.RecordingEngine
import com.darkalise.obs.core.stream.RtmpStreamer
import com.darkalise.obs.core.stream.StreamConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecordingService : Service() {

    companion object {
        private const val TAG = "DarkAlise_RecService"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        var isServiceRunning = false
            private set
    }

    inner class LocalBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val audioMixer = AudioMixer()
    lateinit var screenCaptureManager: ScreenCaptureManager
    lateinit var audioCaptureManager: AudioCaptureManager
    lateinit var recordingEngine: RecordingEngine
    val rtmpStreamer = RtmpStreamer()

    private var videoEncoder: VideoEncoder? = null
    private var mediaProjection: MediaProjection? = null

    private val _isRecordingActive = MutableStateFlow(false)
    val isRecordingActive = _isRecordingActive.asStateFlow()

    private val _isStreamingActive = MutableStateFlow(false)
    val isStreamingActive = _isStreamingActive.asStateFlow()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                NotificationHelper.ACTION_STOP -> {
                    stopBroadcastOrRecording()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        NotificationHelper.createNotificationChannel(this)

        screenCaptureManager = ScreenCaptureManager(this)
        recordingEngine = RecordingEngine(this)
        audioCaptureManager = AudioCaptureManager(this, audioMixer) { pcmBytes, size ->
            // Mux or encode audio buffer
        }

        val filter = IntentFilter(NotificationHelper.ACTION_STOP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(broadcastReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialNotification = NotificationHelper.buildForegroundNotification(
            this,
            isRecording = false,
            isStreaming = false,
            durationFormatted = "00:00",
            details = "Dark Alise OBS is standing by"
        )

        val foregroundTypes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            }
            type
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(this, NotificationHelper.NOTIFICATION_ID, initialNotification, foregroundTypes)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

        if (resultCode == Activity.RESULT_OK && resultData != null && mediaProjection == null) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
        }

        return START_STICKY
    }

    fun startRecordingEngine(): Boolean {
        val projection = mediaProjection ?: return false
        if (_isRecordingActive.value) return true

        val recUri = recordingEngine.start() ?: return false

        videoEncoder = VideoEncoder(
            width = 1920,
            height = 1080,
            fps = 60,
            bitrateBps = 8_000_000,
            onOutputFormatChanged = { format ->
                recordingEngine.addVideoTrack(format)
            },
            onSampleAvailable = { buffer, info ->
                recordingEngine.writeVideoSampleData(buffer, info)
                if (_isStreamingActive.value) {
                    rtmpStreamer.sendVideoSample(buffer, info)
                }
            },
            onError = { err ->
                Log.e(TAG, "Video encoder error", err)
            }
        )

        videoEncoder?.start()
        val encoderSurface = videoEncoder?.surface ?: return false

        screenCaptureManager.startCapture(projection, encoderSurface)
        audioCaptureManager.startMicrophoneCapture()
        audioCaptureManager.startDeviceAudioCapture(projection)

        _isRecordingActive.value = true
        startNotificationUpdater()
        return true
    }

    fun stopRecordingEngine() {
        if (!_isRecordingActive.value) return
        recordingEngine.stop()
        if (!_isStreamingActive.value) {
            videoEncoder?.stop()
            screenCaptureManager.stopCapture()
            audioCaptureManager.stop()
        }
        _isRecordingActive.value = false
    }

    fun startLiveStream(config: StreamConfig) {
        rtmpStreamer.startStream(config)
        _isStreamingActive.value = true
        if (!_isRecordingActive.value) {
            // Start capture & encoder for streaming
            val projection = mediaProjection ?: return
            videoEncoder = VideoEncoder(
                width = config.width,
                height = config.height,
                fps = config.fps,
                bitrateBps = config.bitrateBps,
                onOutputFormatChanged = {},
                onSampleAvailable = { buffer, info ->
                    rtmpStreamer.sendVideoSample(buffer, info)
                },
                onError = { err -> Log.e(TAG, "Streaming encoder error", err) }
            )
            videoEncoder?.start()
            val surface = videoEncoder?.surface ?: return
            screenCaptureManager.startCapture(projection, surface)
            audioCaptureManager.startMicrophoneCapture()
            audioCaptureManager.startDeviceAudioCapture(projection)
        }
        startNotificationUpdater()
    }

    fun stopLiveStream() {
        rtmpStreamer.stopStream()
        _isStreamingActive.value = false
        if (!_isRecordingActive.value) {
            videoEncoder?.stop()
            screenCaptureManager.stopCapture()
            audioCaptureManager.stop()
        }
    }

    fun stopBroadcastOrRecording() {
        stopRecordingEngine()
        stopLiveStream()
        stopSelf()
    }

    private fun startNotificationUpdater() {
        serviceScope.launch {
            while (_isRecordingActive.value || _isStreamingActive.value) {
                val durationSec = recordingEngine.stats.value.durationSeconds
                val min = durationSec / 60
                val sec = durationSec % 60
                val formatted = String.format("%02d:%02d", min, sec)

                val details = if (_isStreamingActive.value) {
                    "Live ${rtmpStreamer.stats.value.uploadBitrateKbps} kbps · ${videoEncoder?.surface != null} HW Encoder"
                } else {
                    "Recording 1080p 60FPS · ${(recordingEngine.stats.value.fileSizeBytes / (1024 * 1024))} MB"
                }

                val notification = NotificationHelper.buildForegroundNotification(
                    this@RecordingService,
                    isRecording = _isRecordingActive.value,
                    isStreaming = _isStreamingActive.value,
                    durationFormatted = formatted,
                    details = details
                )

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notification)
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        stopBroadcastOrRecording()
        try {
            unregisterReceiver(broadcastReceiver)
        } catch (ignored: Exception) {}
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
