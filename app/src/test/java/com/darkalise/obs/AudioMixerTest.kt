package com.darkalise.obs

import com.darkalise.obs.core.audio.AudioMixer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AudioMixerTest {

    private lateinit var mixer: AudioMixer

    @Before
    fun setUp() {
        mixer = AudioMixer()
    }

    @Test
    fun defaultChannelsPresent() {
        val channels = mixer.channels.value
        assertEquals(4, channels.size)
        assertTrue(channels.any { it.id == "mic" })
        assertTrue(channels.any { it.id == "device" })
    }

    @Test
    fun setVolumeClampedCorrectly() {
        mixer.setVolume("mic", 2.0f)
        val channel = mixer.channels.value.find { it.id == "mic" }
        assertEquals(1.5f, channel?.volume ?: 0f, 0.01f)

        mixer.setVolume("mic", -0.5f)
        val clampedMin = mixer.channels.value.find { it.id == "mic" }
        assertEquals(0f, clampedMin?.volume ?: 0f, 0.01f)
    }

    @Test
    fun muteZeroesPeakAndRms() {
        mixer.toggleMute("mic")
        val channel = mixer.channels.value.find { it.id == "mic" }
        assertTrue(channel?.isMuted == true)

        // Process a non-zero PCM buffer
        val dummyPcm = ByteArray(1024) { 0x40.toByte() }
        mixer.processPcmBuffer("mic", dummyPcm, dummyPcm.size)

        val afterProcess = mixer.channels.value.find { it.id == "mic" }
        assertEquals(-60f, afterProcess?.peakDb ?: 0f, 0.01f)
        assertEquals(0f, afterProcess?.rmsLevel ?: 0f, 0.01f)
    }
}
