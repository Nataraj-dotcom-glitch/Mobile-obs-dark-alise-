package com.darkalise.obs.core.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class VideoSettings(
    val baseResolutionWidth: Int = 1920,
    val baseResolutionHeight: Int = 1080,
    val outputResolutionWidth: Int = 1920,
    val outputResolutionHeight: Int = 1080,
    val fps: Int = 60,
    val videoBitrateKbps: Int = 6000,
    val codec: String = "H.264 / AVC",
    val keyframeIntervalSec: Int = 2
)

data class AudioSettings(
    val sampleRate: Int = 44100,
    val channels: Int = 2,
    val audioBitrateKbps: Int = 160,
    val micEnabled: Boolean = true,
    val deviceAudioEnabled: Boolean = true
)

data class StreamSettings(
    val serverUrl: String = "rtmp://live.twitch.tv/app/",
    val streamKey: String = "",
    val autoReconnect: Boolean = true,
    val retryDelaySec: Int = 3,
    val maxRetries: Int = 5
)

data class ObsProfile(
    val id: String,
    val name: String,
    val video: VideoSettings,
    val audio: AudioSettings,
    val stream: StreamSettings,
    val isLowEndOptimized: Boolean = false
)

class SettingsRepository(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "dark_alise_obs_settings"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val defaultProfiles = listOf(
        ObsProfile(
            id = "profile_1080p_rec",
            name = "1080p Recording",
            video = VideoSettings(1920, 1080, 1920, 1080, 60, 12000),
            audio = AudioSettings(48000, 2, 192),
            stream = StreamSettings()
        ),
        ObsProfile(
            id = "profile_720p_rec",
            name = "720p Recording",
            video = VideoSettings(1280, 720, 1280, 720, 60, 6000),
            audio = AudioSettings(44100, 2, 128),
            stream = StreamSettings()
        ),
        ObsProfile(
            id = "profile_1080p_stream",
            name = "1080p Streaming",
            video = VideoSettings(1920, 1080, 1920, 1080, 60, 6000),
            audio = AudioSettings(44100, 2, 160),
            stream = StreamSettings()
        ),
        ObsProfile(
            id = "profile_mobile_gaming",
            name = "Mobile Gaming",
            video = VideoSettings(1920, 1080, 1280, 720, 60, 4500),
            audio = AudioSettings(44100, 2, 160),
            stream = StreamSettings()
        ),
        ObsProfile(
            id = "profile_low_end",
            name = "Low-End Device",
            video = VideoSettings(1280, 720, 854, 480, 30, 2000),
            audio = AudioSettings(44100, 1, 96),
            stream = StreamSettings(),
            isLowEndOptimized = true
        )
    )

    private val _profiles = MutableStateFlow(defaultProfiles)
    val profiles: StateFlow<List<ObsProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow(defaultProfiles[0])
    val activeProfile: StateFlow<ObsProfile> = _activeProfile.asStateFlow()

    init {
        val savedId = prefs.getString(KEY_ACTIVE_PROFILE_ID, defaultProfiles[0].id)
        _activeProfile.value = defaultProfiles.find { it.id == savedId } ?: defaultProfiles[0]
    }

    fun selectProfile(id: String) {
        val found = _profiles.value.find { it.id == id } ?: return
        _activeProfile.value = found
        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, id).apply()
    }

    fun updateActiveProfile(updated: ObsProfile) {
        _activeProfile.value = updated
        _profiles.value = _profiles.value.map {
            if (it.id == updated.id) updated else it
        }
    }

    fun exportToJson(includeStreamKey: Boolean = false): String {
        val current = _activeProfile.value
        val root = JSONObject()
        root.put("appName", "Dark Alise OBS")
        root.put("version", "1.0.0")

        val profileJson = JSONObject().apply {
            put("name", current.name)
            put("video", JSONObject().apply {
                put("outputWidth", current.video.outputResolutionWidth)
                put("outputHeight", current.video.outputResolutionHeight)
                put("fps", current.video.fps)
                put("bitrateKbps", current.video.videoBitrateKbps)
                put("codec", current.video.codec)
            })
            put("audio", JSONObject().apply {
                put("sampleRate", current.audio.sampleRate)
                put("channels", current.audio.channels)
                put("bitrateKbps", current.audio.audioBitrateKbps)
            })
            put("stream", JSONObject().apply {
                put("serverUrl", current.stream.serverUrl)
                if (includeStreamKey) {
                    put("streamKey", current.stream.streamKey)
                }
            })
        }
        root.put("profile", profileJson)
        return root.toString(2)
    }

    fun importFromJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val p = root.getJSONObject("profile")
            val v = p.getJSONObject("video")
            val a = p.getJSONObject("audio")
            val s = p.getJSONObject("stream")

            val imported = ObsProfile(
                id = "imported_${System.currentTimeMillis()}",
                name = p.optString("name", "Imported Profile"),
                video = VideoSettings(
                    outputResolutionWidth = v.optInt("outputWidth", 1920),
                    outputResolutionHeight = v.optInt("outputHeight", 1080),
                    fps = v.optInt("fps", 60),
                    videoBitrateKbps = v.optInt("bitrateKbps", 6000)
                ),
                audio = AudioSettings(
                    sampleRate = a.optInt("sampleRate", 44100),
                    audioBitrateKbps = a.optInt("bitrateKbps", 160)
                ),
                stream = StreamSettings(
                    serverUrl = s.optString("serverUrl", "rtmp://live.twitch.tv/app/"),
                    streamKey = s.optString("streamKey", "")
                )
            )

            _profiles.value = _profiles.value + imported
            selectProfile(imported.id)
            true
        } catch (e: Exception) {
            false
        }
    }
}
