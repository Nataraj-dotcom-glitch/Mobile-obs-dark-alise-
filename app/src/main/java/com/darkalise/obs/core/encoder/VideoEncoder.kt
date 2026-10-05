package com.darkalise.obs.core.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class VideoEncoder(
    private val width: Int = 1920,
    private val height: Int = 1080,
    private val fps: Int = 30,
    private val bitrateBps: Int = 4_500_000,
    private val iFrameIntervalSec: Int = 2,
    private val preferHevc: Boolean = false,
    private val onOutputFormatChanged: (MediaFormat) -> Unit,
    private val onSampleAvailable: (ByteBuffer, MediaCodec.BufferInfo) -> Unit,
    private val onError: (Exception) -> Unit
) {
    companion object {
        private const val TAG = "DarkAlise_VideoEncoder"
        const val MIME_AVC = MediaFormat.MIMETYPE_VIDEO_AVC
        const val MIME_HEVC = MediaFormat.MIMETYPE_VIDEO_HEVC
    }

    private var mediaCodec: MediaCodec? = null
    private var inputSurface: Surface? = null
    private val isRunning = AtomicBoolean(false)
    private var drainThread: Thread? = null

    val surface: Surface?
        get() = inputSurface

    fun isEncoding(): Boolean = isRunning.get()

    fun start() {
        if (isRunning.get()) return

        try {
            val mime = if (preferHevc && isMimeSupported(MIME_HEVC)) MIME_HEVC else MIME_AVC
            val format = MediaFormat.createVideoFormat(mime, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameIntervalSec)

                // Configure profile if AVC
                if (mime == MIME_AVC) {
                    setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
                    setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel41)
                }

                // Bitrate mode: CBR or VBR
                setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
            }

            mediaCodec = MediaCodec.createEncoderByType(mime)
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = mediaCodec?.createInputSurface()
            mediaCodec?.start()

            isRunning.set(true)
            startDrainLoop()
            Log.i(TAG, "Hardware encoder started successfully: $mime @ ${width}x${height} ${fps}fps ${bitrateBps / 1000}kbps")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start hardware encoder, attempting safe recovery", e)
            stop()
            onError(e)
        }
    }

    private fun startDrainLoop() {
        drainThread = Thread({
            val bufferInfo = MediaCodec.BufferInfo()
            val codec = mediaCodec ?: return@Thread

            while (isRunning.get()) {
                try {
                    val status = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                    when {
                        status == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            // No output available yet
                        }
                        status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val newFormat = codec.outputFormat
                            Log.i(TAG, "Encoder output format changed: $newFormat")
                            onOutputFormatChanged(newFormat)
                        }
                        status >= 0 -> {
                            val outputBuffer = codec.getOutputBuffer(status)
                            if (outputBuffer != null && bufferInfo.size > 0) {
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                    outputBuffer.position(bufferInfo.offset)
                                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                    onSampleAvailable(outputBuffer, bufferInfo)
                                }
                            }
                            codec.releaseOutputBuffer(status, false)

                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isRunning.get()) {
                        Log.e(TAG, "Encoder drain loop exception", e)
                        onError(e)
                    }
                    break
                }
            }
        }, "DarkAlise_EncoderDrain").apply { start() }
    }

    fun stop() {
        isRunning.set(false)
        drainThread?.interrupt()
        drainThread = null

        try {
            mediaCodec?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Exception while stopping encoder: ${e.message}")
        } finally {
            try {
                mediaCodec?.release()
            } catch (ignored: Exception) {}
            mediaCodec = null
            inputSurface?.release()
            inputSurface = null
        }
        Log.i(TAG, "Video encoder stopped and resources released")
    }

    private fun isMimeSupported(mime: String): Boolean {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in list.codecInfos) {
            if (!info.isEncoder) continue
            for (type in info.supportedTypes) {
                if (type.equals(mime, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }
}
