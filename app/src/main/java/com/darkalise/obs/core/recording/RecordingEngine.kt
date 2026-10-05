package com.darkalise.obs.core.recording

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordingStats(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val durationSeconds: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val fps: Int = 0,
    val bitrateKbps: Int = 0,
    val droppedFrames: Long = 0L,
    val outputUri: Uri? = null
)

class RecordingEngine(private val context: Context) {

    companion object {
        private const val TAG = "DarkAlise_RecordingEngine"
        const val SUB_FOLDER_NAME = "Dark Alise OBS"
    }

    private var mediaMuxer: MediaMuxer? = null
    private var pfd: ParcelFileDescriptor? = null
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var isMuxerStarted = false

    private val _stats = MutableStateFlow(RecordingStats())
    val stats: StateFlow<RecordingStats> = _stats.asStateFlow()

    private var startTimeMillis = 0L
    private var pauseStartTime = 0L
    private var accumulatedPauseMillis = 0L
    private var totalBytesWritten = 0L
    private var frameCounter = 0L
    private var lastFpsCheckTime = 0L

    @Synchronized
    fun start(outputFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4): Uri? {
        if (_stats.value.isRecording) return _stats.value.outputUri

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "DarkAlise_${timestamp}.mp4"

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$SUB_FOLDER_NAME")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val uri = context.contentResolver.insert(collection, contentValues) ?: return null
            pfd = context.contentResolver.openFileDescriptor(uri, "rw")
            val fd = pfd?.fileDescriptor ?: return null

            mediaMuxer = MediaMuxer(fd, outputFormat)
            isMuxerStarted = false
            videoTrackIndex = -1
            audioTrackIndex = -1
            totalBytesWritten = 0L
            frameCounter = 0L
            startTimeMillis = SystemClock.elapsedRealtime()
            lastFpsCheckTime = startTimeMillis
            accumulatedPauseMillis = 0L

            _stats.value = RecordingStats(
                isRecording = true,
                isPaused = false,
                outputUri = uri
            )
            Log.i(TAG, "RecordingEngine started, writing to: $uri")
            return uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaMuxer", e)
            stop()
            return null
        }
    }

    @Synchronized
    fun addVideoTrack(format: MediaFormat) {
        val muxer = mediaMuxer ?: return
        if (isMuxerStarted) return
        videoTrackIndex = muxer.addTrack(format)
        checkAndStartMuxer()
    }

    @Synchronized
    fun addAudioTrack(format: MediaFormat) {
        val muxer = mediaMuxer ?: return
        if (isMuxerStarted) return
        audioTrackIndex = muxer.addTrack(format)
        checkAndStartMuxer()
    }

    private fun checkAndStartMuxer() {
        val muxer = mediaMuxer ?: return
        if (!isMuxerStarted && videoTrackIndex >= 0) {
            try {
                muxer.start()
                isMuxerStarted = true
                Log.i(TAG, "MediaMuxer started with video track: $videoTrackIndex, audio track: $audioTrackIndex")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting MediaMuxer", e)
            }
        }
    }

    @Synchronized
    fun writeVideoSampleData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (!isMuxerStarted || videoTrackIndex < 0 || _stats.value.isPaused) return
        try {
            mediaMuxer?.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo)
            totalBytesWritten += bufferInfo.size
            frameCounter++
            updateLiveStats()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing video sample", e)
        }
    }

    @Synchronized
    fun writeAudioSampleData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (!isMuxerStarted || audioTrackIndex < 0 || _stats.value.isPaused) return
        try {
            mediaMuxer?.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo)
            totalBytesWritten += bufferInfo.size
        } catch (e: Exception) {
            Log.e(TAG, "Error writing audio sample", e)
        }
    }

    fun pause() {
        if (!_stats.value.isRecording || _stats.value.isPaused) return
        pauseStartTime = SystemClock.elapsedRealtime()
        _stats.value = _stats.value.copy(isPaused = true)
    }

    fun resume() {
        if (!_stats.value.isRecording || !_stats.value.isPaused) return
        val pauseDuration = SystemClock.elapsedRealtime() - pauseStartTime
        accumulatedPauseMillis += pauseDuration
        _stats.value = _stats.value.copy(isPaused = false)
    }

    private fun updateLiveStats() {
        val now = SystemClock.elapsedRealtime()
        val durationMs = (now - startTimeMillis) - accumulatedPauseMillis
        val durationSec = Math.max(0L, durationMs / 1000L)

        val deltaMs = now - lastFpsCheckTime
        var currentFps = _stats.value.fps
        if (deltaMs >= 1000L) {
            currentFps = ((frameCounter * 1000L) / deltaMs).toInt()
            frameCounter = 0L
            lastFpsCheckTime = now
        }

        val kbps = if (durationSec > 0) ((totalBytesWritten * 8L) / (durationSec * 1000L)).toInt() else 0

        _stats.value = _stats.value.copy(
            durationSeconds = durationSec,
            fileSizeBytes = totalBytesWritten,
            fps = currentFps,
            bitrateKbps = kbps
        )
    }

    @Synchronized
    fun stop(): Uri? {
        val recordedUri = _stats.value.outputUri
        try {
            if (isMuxerStarted) {
                mediaMuxer?.stop()
            }
            mediaMuxer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping MediaMuxer: ${e.message}")
        } finally {
            mediaMuxer = null
            isMuxerStarted = false
            try {
                pfd?.close()
            } catch (ignored: Exception) {}
            pfd = null
        }

        // Clear pending flag on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && recordedUri != null) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            try {
                context.contentResolver.update(recordedUri, contentValues, null, null)
            } catch (e: Exception) {
                Log.w(TAG, "Error updating pending flag: ${e.message}")
            }
        }

        _stats.value = _stats.value.copy(isRecording = false, isPaused = false)
        Log.i(TAG, "Recording completed: $recordedUri")
        return recordedUri
    }
}
