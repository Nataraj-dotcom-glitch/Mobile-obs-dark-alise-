package com.darkalise.obs.core.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.log10
import kotlin.math.sqrt

data class AudioChannelState(
    val id: String,
    val name: String,
    val volume: Float = 1.0f, // 0.0 to 1.5 (gain boost)
    val isMuted: Boolean = false,
    val gainDb: Float = 0.0f,
    val peakDb: Float = -60.0f, // -60dBFS to 0dBFS
    val rmsLevel: Float = 0.0f,  // 0.0 to 1.0 normalized
    val statusText: String = "Ready",
    val isHardwareAvailable: Boolean = true
)

class AudioMixer {

    private val _channels = MutableStateFlow<List<AudioChannelState>>(
        listOf(
            AudioChannelState(id = "mic", name = "Microphone", volume = 0.85f, statusText = "Active"),
            AudioChannelState(id = "device", name = "Device Audio", volume = 1.0f, statusText = "Playback Capture"),
            AudioChannelState(id = "camera", name = "Camera Audio", volume = 0.8f, statusText = "Sync"),
            AudioChannelState(id = "media", name = "Media Audio", volume = 0.75f, statusText = "Idle")
        )
    )
    val channels: StateFlow<List<AudioChannelState>> = _channels.asStateFlow()

    fun setVolume(channelId: String, volume: Float) {
        _channels.value = _channels.value.map { channel ->
            if (channel.id == channelId) channel.copy(volume = volume.coerceIn(0f, 1.5f)) else channel
        }
    }

    fun toggleMute(channelId: String) {
        _channels.value = _channels.value.map { channel ->
            if (channel.id == channelId) channel.copy(isMuted = !channel.isMuted) else channel
        }
    }

    fun setGainDb(channelId: String, gainDb: Float) {
        _channels.value = _channels.value.map { channel ->
            if (channel.id == channelId) channel.copy(gainDb = gainDb.coerceIn(-30f, 30f)) else channel
        }
    }

    fun setStatusText(channelId: String, status: String, isAvailable: Boolean = true) {
        _channels.value = _channels.value.map { channel ->
            if (channel.id == channelId) channel.copy(statusText = status, isHardwareAvailable = isAvailable) else channel
        }
    }

    /**
     * Compute actual Peak dBFS and RMS from real 16-bit PCM byte array.
     */
    fun processPcmBuffer(channelId: String, pcmBuffer: ByteArray, readBytes: Int) {
        if (readBytes <= 0) return

        var sumSquares = 0.0
        var maxSample = 0

        val samplesCount = readBytes / 2
        for (i in 0 until samplesCount) {
            val low = pcmBuffer[i * 2].toInt() and 0xFF
            val high = pcmBuffer[i * 2 + 1].toInt()
            val sample = (high shl 8) or low // 16-bit signed PCM

            val absSample = Math.abs(sample)
            if (absSample > maxSample) {
                maxSample = absSample
            }
            sumSquares += (sample.toDouble() * sample.toDouble())
        }

        val rms = if (samplesCount > 0) sqrt(sumSquares / samplesCount) else 0.0
        val normalizedRms = (rms / 32768.0).toFloat().coerceIn(0f, 1f)

        // Peak in dBFS (0 is digital ceiling, -60 is silence)
        val peakRatio = if (maxSample > 0) maxSample.toDouble() / 32768.0 else 0.0001
        val peakDb = (20.0 * log10(peakRatio)).toFloat().coerceIn(-60f, 0f)

        _channels.value = _channels.value.map { channel ->
            if (channel.id == channelId) {
                if (channel.isMuted) {
                    channel.copy(peakDb = -60f, rmsLevel = 0f)
                } else {
                    // Apply channel volume to visual level
                    channel.copy(
                        peakDb = (peakDb + channel.gainDb).coerceIn(-60f, 0f),
                        rmsLevel = (normalizedRms * channel.volume).coerceIn(0f, 1f)
                    )
                }
            } else channel
        }
    }
}
