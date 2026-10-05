package com.darkalise.obs.ui.studio

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkalise.obs.core.audio.AudioMixer
import com.darkalise.obs.core.overlay.CameraOverlayConfig
import com.darkalise.obs.core.performance.PerformanceTelemetry
import com.darkalise.obs.core.recording.RecordingStats
import com.darkalise.obs.core.scene.Scene
import com.darkalise.obs.core.scene.SceneManager
import com.darkalise.obs.core.source.Source
import com.darkalise.obs.core.stream.StreamLiveStats
import com.darkalise.obs.ui.theme.DarkAliseBlack
import com.darkalise.obs.ui.theme.DarkAliseBorder
import com.darkalise.obs.ui.theme.DarkAliseGreen
import com.darkalise.obs.ui.theme.DarkAliseNeon
import com.darkalise.obs.ui.theme.DarkAlisePurpleDark
import com.darkalise.obs.ui.theme.DarkAliseRed
import com.darkalise.obs.ui.theme.DarkAliseSurface
import com.darkalise.obs.ui.theme.DarkAliseSurfaceVariant
import com.darkalise.obs.ui.theme.DarkAliseText
import com.darkalise.obs.ui.theme.DarkAliseTextMuted
import java.util.Locale

@Composable
fun StudioScreen(
    sceneManager: SceneManager,
    audioMixer: AudioMixer,
    recordingStats: RecordingStats,
    streamStats: StreamLiveStats,
    telemetry: PerformanceTelemetry,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onStartStreaming: () -> Unit,
    onStopStreaming: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val scenes by sceneManager.scenes.collectAsState()
    val activeSceneId by sceneManager.activeSceneId.collectAsState()
    val sourcesMap by sceneManager.sourcesMap.collectAsState()
    val channels by audioMixer.channels.collectAsState()

    val activeScene = scenes.find { it.id == activeSceneId } ?: scenes.firstOrNull()
    var overlayConfig by remember { mutableStateOf(CameraOverlayConfig()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkAliseBlack)
    ) {
        // Top Broadcast Status Bar
        StudioTopBar(
            recordingStats = recordingStats,
            streamStats = streamStats,
            telemetry = telemetry
        )

        if (isLandscape) {
            // Landscape Studio Layout: Preview on Left, Panels (Scenes/Sources/Mixer/Controls) on Right
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Left Column: Preview + Transport Controls
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                ) {
                    StudioPreview(
                        activeSceneName = activeScene?.name ?: "MAIN",
                        isLive = streamStats.state.name == "CONNECTED",
                        isRecording = recordingStats.isRecording,
                        cameraEnabled = true,
                        overlayConfig = overlayConfig,
                        onOverlayMoved = { _, _ -> },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StudioControlsBar(
                        isRecording = recordingStats.isRecording,
                        isStreaming = streamStats.state.name == "CONNECTED",
                        onStartRecording = onStartRecording,
                        onStopRecording = onStopRecording,
                        onStartStreaming = onStartStreaming,
                        onStopStreaming = onStopStreaming
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right Column: Scenes, Sources & Audio Mixer
                Row(
                    modifier = Modifier
                        .weight(1.7f)
                        .fillMaxHeight()
                ) {
                    // Scenes & Sources
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        SceneListPanel(
                            scenes = scenes,
                            activeSceneId = activeSceneId,
                            onSceneSelected = { sceneManager.switchScene(it) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SourceListPanel(
                            sources = activeScene?.sourceIds?.mapNotNull { sourcesMap[it] } ?: emptyList(),
                            onToggleVisibility = { sceneManager.updateSourceVisibility(it.id, !it.isVisible) },
                            onToggleLock = { sceneManager.updateSourceLock(it.id, !it.isLocked) },
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Audio Mixer
                    AudioMixerPanel(
                        channels = channels,
                        onVolumeChanged = { id, vol -> audioMixer.setVolume(id, vol) },
                        onMuteToggled = { id -> audioMixer.toggleMute(id) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        } else {
            // Portrait Studio Layout: Stacked Preview, Scenes/Sources, Mixer & Controls
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                item {
                    StudioPreview(
                        activeSceneName = activeScene?.name ?: "MAIN",
                        isLive = streamStats.state.name == "CONNECTED",
                        isRecording = recordingStats.isRecording,
                        cameraEnabled = true,
                        overlayConfig = overlayConfig,
                        onOverlayMoved = { _, _ -> }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    StudioControlsBar(
                        isRecording = recordingStats.isRecording,
                        isStreaming = streamStats.state.name == "CONNECTED",
                        onStartRecording = onStartRecording,
                        onStopRecording = onStopRecording,
                        onStartStreaming = onStartStreaming,
                        onStopStreaming = onStopStreaming
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    SceneListPanel(
                        scenes = scenes,
                        activeSceneId = activeSceneId,
                        onSceneSelected = { sceneManager.switchScene(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    SourceListPanel(
                        sources = activeScene?.sourceIds?.mapNotNull { sourcesMap[it] } ?: emptyList(),
                        onToggleVisibility = { sceneManager.updateSourceVisibility(it.id, !it.isVisible) },
                        onToggleLock = { sceneManager.updateSourceLock(it.id, !it.isLocked) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    AudioMixerPanel(
                        channels = channels,
                        onVolumeChanged = { id, vol -> audioMixer.setVolume(id, vol) },
                        onMuteToggled = { id -> audioMixer.toggleMute(id) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun StudioTopBar(
    recordingStats: RecordingStats,
    streamStats: StreamLiveStats,
    telemetry: PerformanceTelemetry
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkAliseSurface)
            .border(1.dp, DarkAliseBorder)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Branding
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "DARK ALISE OBS",
                color = DarkAliseNeon,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Status Dot
            val isLive = streamStats.state.name == "CONNECTED"
            val statusColor = when {
                isLive -> DarkAliseGreen
                recordingStats.isRecording -> DarkAliseRed
                else -> DarkAliseTextMuted
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when {
                    isLive -> "LIVE"
                    recordingStats.isRecording -> "REC"
                    else -> "IDLE"
                },
                color = statusColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Live Telemetry
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "FPS: ${telemetry.fps}",
                color = DarkAliseText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RAM: ${telemetry.ramUsedMb}M",
                color = DarkAliseTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "DROP: ${recordingStats.droppedFrames + streamStats.droppedFrames}",
                color = if (streamStats.droppedFrames > 0) DarkAliseRed else DarkAliseTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun StudioControlsBar(
    isRecording: Boolean,
    isStreaming: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onStartStreaming: () -> Unit,
    onStopStreaming: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkAliseSurface, RoundedCornerShape(8.dp))
            .border(1.dp, DarkAliseBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Stream Button
        Button(
            onClick = { if (isStreaming) onStopStreaming() else onStartStreaming() },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isStreaming) DarkAliseRed else DarkAlisePurpleDark
            ),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Icon(
                imageVector = if (isStreaming) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = "Stream",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isStreaming) "STOP STREAM" else "START STREAM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Record Button
        Button(
            onClick = { if (isRecording) onStopRecording() else onStartRecording() },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) DarkAliseRed else DarkAlisePurpleDark
            ),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                contentDescription = "Record",
                tint = if (isRecording) Color.White else DarkAliseRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isRecording) "STOP REC" else "RECORD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SceneListPanel(
    scenes: List<Scene>,
    activeSceneId: String,
    onSceneSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkAliseSurface, RoundedCornerShape(8.dp))
            .border(1.dp, DarkAliseBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(
            text = "SCENES",
            color = DarkAliseNeon,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(scenes) { scene ->
                val isSelected = scene.id == activeSceneId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) DarkAliseNeon else DarkAliseSurfaceVariant)
                        .border(1.dp, if (isSelected) DarkAliseNeon else DarkAliseBorder, RoundedCornerShape(4.dp))
                        .clickable { onSceneSelected(scene.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = scene.name,
                        color = if (isSelected) DarkAliseBlack else DarkAliseText,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun SourceListPanel(
    sources: List<Source>,
    onToggleVisibility: (Source) -> Unit,
    onToggleLock: (Source) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkAliseSurface, RoundedCornerShape(8.dp))
            .border(1.dp, DarkAliseBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(
            text = "SOURCES",
            color = DarkAliseNeon,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        sources.forEach { source ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .background(DarkAliseSurfaceVariant, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = source.name,
                    color = DarkAliseText,
                    fontSize = 11.sp
                )
                Row {
                    IconButton(
                        onClick = { onToggleVisibility(source) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (source.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Visibility",
                            tint = if (source.isVisible) DarkAliseNeon else DarkAliseTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = { onToggleLock(source) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (source.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock",
                            tint = if (source.isLocked) Color(0xFFEAB308) else DarkAliseTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
