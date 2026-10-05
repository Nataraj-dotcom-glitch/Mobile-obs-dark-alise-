package com.darkalise.obs.core.overlay

enum class OverlayShape {
    RECTANGLE,
    ROUNDED_RECTANGLE,
    CIRCLE
}

data class CameraOverlayConfig(
    val shape: OverlayShape = OverlayShape.ROUNDED_RECTANGLE,
    val cornerRadiusDp: Float = 16f,
    val borderWidthDp: Float = 2f,
    val borderColorHex: Long = 0xFFA855F7, // Neon purple
    val positionX: Float = 0.7f,
    val positionY: Float = 0.65f,
    val widthFraction: Float = 0.28f,
    val heightFraction: Float = 0.28f,
    val rotation: Float = 0f,
    val opacity: Float = 1.0f
)
