package com.darkalise.obs.core.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

class AudioCaptureManager(
    private val context: Context,
    private val mixer: AudioMixer,
    private val onPcmDataAvailable: (ByteArray, Int) -> Unit
) {
    companion object {
        private const val TAG = "DarkAlise_AudioCapture"
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var micAudioRecord: AudioRecord? = null
    private var playbackAudioRecord: AudioRecord? = null

    private val isRecording = AtomicBoolean(false)
    private var micCaptureThread: Thread? = null
    private var playbackCaptureThread: Thread? = null

    @SuppressLint("MissingPermission")
    fun startMicrophoneCapture(): Boolean {
        try {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = Math.max(minBufferSize, 8192)

            micAudioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (micAudioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                mixer.setStatusText("mic", "Initialization Failed", isAvailable = false)
                return false
            }

            micAudioRecord?.startRecording()
            isRecording.set(true)
            mixer.setStatusText("mic", "Recording")

            micCaptureThread = Thread({
                val audioBuffer = ByteArray(bufferSize)
                while (isRecording.get()) {
                    val read = micAudioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                    if (read > 0) {
                        mixer.processPcmBuffer("mic", audioBuffer, read)
                        onPcmDataAvailable(audioBuffer, read)
                    }
                }
            }, "DarkAlise_MicCapture").apply { start() }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start microphone capture", e)
            mixer.setStatusText("mic", "Error: ${e.message}", isAvailable = false)
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun startDeviceAudioCapture(mediaProjection: MediaProjection?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            mixer.setStatusText("device", "Requires Android 10+", isAvailable = false)
            return false
        }

        if (mediaProjection == null) {
            mixer.setStatusText("device", "No MediaProjection", isAvailable = false)
            return false
        }

        try {
            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AUDIO_FORMAT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()

            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = Math.max(minBufferSize, 8192)

            playbackAudioRecord = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(config)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (playbackAudioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                mixer.setStatusText("device", "Device/application does not allow audio capture.", isAvailable = false)
                return false
            }

            playbackAudioRecord?.startRecording()
            mixer.setStatusText("device", "Capturing Playback")

            playbackCaptureThread = Thread({
                val buffer = ByteArray(bufferSize)
                while (isRecording.get()) {
                    val read = playbackAudioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        mixer.processPcmBuffer("device", buffer, read)
                    }
                }
            }, "DarkAlise_PlaybackCapture").apply { start() }

            return true
        } catch (e: SecurityException) {
            mixer.setStatusText("device", "Device/application does not allow audio capture.", isAvailable = false)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Device audio capture failed", e)
            mixer.setStatusText("device", "Device/application does not allow audio capture.", isAvailable = false)
            return false
        }
    }

    fun stop() {
        isRecording.set(false)

        micCaptureThread?.interrupt()
        micCaptureThread = null

        playbackCaptureThread?.interrupt()
        playbackCaptureThread = null

        try {
            micAudioRecord?.stop()
            micAudioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing mic: ${e.message}")
        }
        micAudioRecord = null

        try {
            playbackAudioRecord?.stop()
            playbackAudioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing playback capture: ${e.message}")
        }
        playbackAudioRecord = null

        mixer.setStatusText("mic", "Stopped")
        mixer.setStatusText("device", "Stopped")
    }
}
