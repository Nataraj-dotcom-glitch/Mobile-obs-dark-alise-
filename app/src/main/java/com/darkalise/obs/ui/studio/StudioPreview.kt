package com.darkalise.obs.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkalise.obs.core.overlay.CameraOverlayConfig
import com.darkalise.obs.core.overlay.OverlayShape
import com.darkalise.obs.ui.theme.DarkAliseBorder
import com.darkalise.obs.ui.theme.DarkAliseNeon
import com.darkalise.obs.ui.theme.DarkAlisePurpleDark
import com.darkalise.obs.ui.theme.DarkAliseSurface
import com.darkalise.obs.ui.theme.DarkAliseTextMuted
import kotlin.math.roundToInt

@Composable
fun StudioPreview(
    activeSceneName: String,
    isLive: Boolean,
    isRecording: Boolean,
    cameraEnabled: Boolean,
    overlayConfig: CameraOverlayConfig,
    onOverlayMoved: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(DarkAliseSurface, RoundedCornerShape(8.dp))
            .border(1.dp, DarkAliseBorder, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Main Screen Backdrop Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0E17)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SCENE: $activeSceneName",
                color = DarkAliseTextMuted,
                fontSize = 14.sp
            )
        }

        // Camera Overlay (Draggable)
        if (cameraEnabled) {
            val shape = when (overlayConfig.shape) {
                OverlayShape.CIRCLE -> CircleShape
                OverlayShape.ROUNDED_RECTANGLE -> RoundedCornerShape(overlayConfig.cornerRadiusDp.dp)
                OverlayShape.RECTANGLE -> RoundedCornerShape(0.dp)
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(130.dp, 90.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                            onOverlayMoved(offsetX, offsetY)
                        }
                    }
                    .clip(shape)
                    .background(DarkAlisePurpleDark)
                    .border(overlayConfig.borderWidthDp.dp, DarkAliseNeon, shape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CAM 1",
                    color = DarkAliseNeon,
                    fontSize = 11.sp
                )
            }
        }

        // On-screen Program HUD
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "PROGRAM 1080p60",
                color = Color.White,
                fontSize = 10.sp
            )
        }
    }
}
