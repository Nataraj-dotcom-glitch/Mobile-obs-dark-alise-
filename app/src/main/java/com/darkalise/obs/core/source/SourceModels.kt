package com.darkalise.obs.core.source

import java.util.UUID

enum class SourceType {
    DISPLAY_CAPTURE,
    CAMERA,
    IMAGE,
    TEXT,
    COLOR,
    AUDIO_INPUT,
    AUDIO_OUTPUT,
    MEDIA,
    BROWSER_WEBVIEW
}

data class SourceCrop(
    val top: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f,
    val right: Float = 0f
)

data class SourceTransform(
    val x: Float = 0f,
    val y: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val crop: SourceCrop = SourceCrop()
)

data class Source(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: SourceType,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val isMuted: Boolean = false,
    val volume: Float = 1.0f,
    val transform: SourceTransform = SourceTransform(),
    val extraParams: Map<String, String> = emptyMap()
)
