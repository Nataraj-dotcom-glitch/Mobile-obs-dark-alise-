package com.darkalise.obs.core.capture

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CaptureResolution(val width: Int, val height: Int, val name: String)

data class ScreenCaptureConfig(
    val resolution: CaptureResolution = CaptureResolution(1920, 1080, "1080p Full HD"),
    val fps: Int = 60,
    val bitrateBps: Int = 6_000_000
)

class ScreenCaptureManager(private val context: Context) {

    companion object {
        private const val TAG = "DarkAlise_ScreenCapture"
        const val VIRTUAL_DISPLAY_NAME = "DarkAliseOBS_VirtualDisplay"
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _currentConfig = MutableStateFlow(ScreenCaptureConfig())
    val currentConfig: StateFlow<ScreenCaptureConfig> = _currentConfig.asStateFlow()

    fun getDeviceNativeResolution(): CaptureResolution {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val w = metrics.widthPixels
        val h = metrics.heightPixels

        return when {
            Math.min(w, h) >= 1440 -> CaptureResolution(2560, 1440, "1440p QHD")
            Math.min(w, h) >= 1080 -> CaptureResolution(1920, 1080, "1080p FHD")
            else -> CaptureResolution(1280, 720, "720p HD")
        }
    }

    fun startCapture(
        projection: MediaProjection,
        targetSurface: Surface,
        config: ScreenCaptureConfig = _currentConfig.value
    ): Boolean {
        if (_isCapturing.value) return true

        try {
            mediaProjection = projection
            _currentConfig.value = config

            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            val dpi = metrics.densityDpi

            virtualDisplay = projection.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                config.resolution.width,
                config.resolution.height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                targetSurface,
                null,
                null
            )

            _isCapturing.value = true
            Log.i(TAG, "VirtualDisplay created: ${config.resolution.width}x${config.resolution.height} @ ${config.fps}fps")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create VirtualDisplay", e)
            stopCapture()
            return false
        }
    }

    fun stopCapture() {
        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing virtual display: ${e.message}")
        }
        virtualDisplay = null

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping media projection: ${e.message}")
        }
        mediaProjection = null
        _isCapturing.value = false
        Log.i(TAG, "Screen capture stopped successfully")
    }
}
