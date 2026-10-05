package com.darkalise.obs.core.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CameraLens {
    FRONT,
    BACK
}

data class CameraStatus(
    val isEnabled: Boolean = false,
    val lens: CameraLens = CameraLens.FRONT,
    val isTorchOn: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val maxZoom: Float = 5.0f,
    val minZoom: Float = 1.0f,
    val exposureCompensationIndex: Int = 0
)

class CameraManager(private val context: Context) {

    companion object {
        private const val TAG = "DarkAlise_CameraManager"
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var previewUseCase: Preview? = null

    private val _status = MutableStateFlow(CameraStatus())
    val status: StateFlow<CameraStatus> = _status.asStateFlow()

    fun initialize(onReady: () -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cameraProvider = future.get()
                onReady()
            } catch (e: Exception) {
                Log.e(TAG, "ProcessCameraProvider initialization error", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        lens: CameraLens = _status.value.lens,
        surfaceProvider: Preview.SurfaceProvider? = null
    ) {
        val provider = cameraProvider ?: return
        try {
            provider.unbindAll()

            val selector = if (lens == CameraLens.FRONT) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            previewUseCase = Preview.Builder().build().also {
                if (surfaceProvider != null) {
                    it.setSurfaceProvider(surfaceProvider)
                }
            }

            camera = provider.bindToLifecycle(lifecycleOwner, selector, previewUseCase)
            cameraControl = camera?.cameraControl
            cameraInfo = camera?.cameraInfo

            val maxZoom = cameraInfo?.zoomState?.value?.maxZoomRatio ?: 5.0f
            val minZoom = cameraInfo?.zoomState?.value?.minZoomRatio ?: 1.0f

            _status.value = _status.value.copy(
                isEnabled = true,
                lens = lens,
                maxZoom = maxZoom,
                minZoom = minZoom
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind CameraX use cases", e)
            _status.value = _status.value.copy(isEnabled = false)
        }
    }

    fun toggleLens(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider? = null) {
        val nextLens = if (_status.value.lens == CameraLens.FRONT) CameraLens.BACK else CameraLens.FRONT
        startCamera(lifecycleOwner, nextLens, surfaceProvider)
    }

    fun setTorch(enable: Boolean) {
        cameraControl?.enableTorch(enable)
        _status.value = _status.value.copy(isTorchOn = enable)
    }

    fun setZoom(ratio: Float) {
        val clamped = ratio.coerceIn(_status.value.minZoom, _status.value.maxZoom)
        cameraControl?.setZoomRatio(clamped)
        _status.value = _status.value.copy(zoomRatio = clamped)
    }

    fun setExposure(index: Int) {
        cameraControl?.setExposureCompensationIndex(index)
        _status.value = _status.value.copy(exposureCompensationIndex = index)
    }

    fun stop() {
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.w(TAG, "Error unbinding CameraX: ${e.message}")
        }
        camera = null
        cameraControl = null
        cameraInfo = null
        _status.value = _status.value.copy(isEnabled = false, isTorchOn = false)
    }
}
