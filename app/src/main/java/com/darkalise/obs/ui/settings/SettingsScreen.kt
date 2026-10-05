package com.darkalise.obs.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkalise.obs.core.settings.SettingsRepository
import com.darkalise.obs.ui.theme.DarkAliseBlack
import com.darkalise.obs.ui.theme.DarkAliseBorder
import com.darkalise.obs.ui.theme.DarkAliseNeon
import com.darkalise.obs.ui.theme.DarkAlisePurpleDark
import com.darkalise.obs.ui.theme.DarkAliseSurface
import com.darkalise.obs.ui.theme.DarkAliseSurfaceVariant
import com.darkalise.obs.ui.theme.DarkAliseText
import com.darkalise.obs.ui.theme.DarkAliseTextMuted

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    val profiles by settingsRepository.profiles.collectAsState()
    val activeProfile by settingsRepository.activeProfile.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("VIDEO", "AUDIO", "STREAM", "PROFILES", "EXPORT", "VIRTUAL CAM")

    var streamUrl by remember(activeProfile) { mutableStateOf(activeProfile.stream.serverUrl) }
    var streamKey by remember(activeProfile) { mutableStateOf(activeProfile.stream.streamKey) }
    var exportJsonString by remember { mutableStateOf("") }
    var importStatus by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkAliseBlack)
            .padding(16.dp)
    ) {
        Text(
            text = "SETTINGS & PROFILES",
            color = DarkAliseNeon,
            fontSize = 16.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = DarkAliseSurface,
            contentColor = DarkAliseNeon,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = DarkAliseNeon
                )
            },
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            color = if (selectedTabIndex == index) DarkAliseNeon else DarkAliseTextMuted
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (selectedTabIndex) {
                0 -> { // Video
                    item {
                        SettingsCard(title = "Video Output Settings") {
                            Text("Base Canvas: ${activeProfile.video.baseResolutionWidth}x${activeProfile.video.baseResolutionHeight}", color = DarkAliseText, fontSize = 12.sp)
                            Text("Output Scale: ${activeProfile.video.outputResolutionWidth}x${activeProfile.video.outputResolutionHeight}", color = DarkAliseText, fontSize = 12.sp)
                            Text("Frame Rate: ${activeProfile.video.fps} FPS", color = DarkAliseText, fontSize = 12.sp)
                            Text("Bitrate: ${activeProfile.video.videoBitrateKbps} kbps", color = DarkAliseText, fontSize = 12.sp)
                            Text("Hardware Codec: ${activeProfile.video.codec}", color = DarkAliseNeon, fontSize = 12.sp)
                        }
                    }
                }
                1 -> { // Audio
                    item {
                        SettingsCard(title = "Audio Architecture") {
                            Text("Sample Rate: ${activeProfile.audio.sampleRate} Hz", color = DarkAliseText, fontSize = 12.sp)
                            Text("Channels: ${if (activeProfile.audio.channels == 2) "Stereo" else "Mono"}", color = DarkAliseText, fontSize = 12.sp)
                            Text("Audio Bitrate: ${activeProfile.audio.audioBitrateKbps} kbps", color = DarkAliseText, fontSize = 12.sp)
                            Text("Capture Engine: AudioRecord + AudioPlaybackCapture", color = DarkAliseNeon, fontSize = 12.sp)
                        }
                    }
                }
                2 -> { // Stream
                    item {
                        SettingsCard(title = "RTMP Server Configuration") {
                            OutlinedTextField(
                                value = streamUrl,
                                onValueChange = { streamUrl = it },
                                label = { Text("Server URL (rtmp:// or rtmps://)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DarkAliseNeon,
                                    unfocusedBorderColor = DarkAliseBorder,
                                    focusedTextColor = DarkAliseText,
                                    unfocusedTextColor = DarkAliseText
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = streamKey,
                                onValueChange = { streamKey = it },
                                label = { Text("Stream Key (Stored locally)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DarkAliseNeon,
                                    unfocusedBorderColor = DarkAliseBorder,
                                    focusedTextColor = DarkAliseText,
                                    unfocusedTextColor = DarkAliseText
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val updated = activeProfile.copy(
                                        stream = activeProfile.stream.copy(serverUrl = streamUrl, streamKey = streamKey)
                                    )
                                    settingsRepository.updateActiveProfile(updated)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkAliseNeon)
                            ) {
                                Text("Save RTMP Settings", color = DarkAliseBlack)
                            }
                        }
                    }
                }
                3 -> { // Profiles
                    item {
                        SettingsCard(title = "Switch Profile Preset") {
                            profiles.forEach { profile ->
                                val isSelected = profile.id == activeProfile.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${profile.name} (${profile.video.outputResolutionWidth}x${profile.video.outputResolutionHeight} @ ${profile.video.fps}fps)",
                                        color = if (isSelected) DarkAliseNeon else DarkAliseText,
                                        fontSize = 12.sp
                                    )
                                    Button(
                                        onClick = { settingsRepository.selectProfile(profile.id) },
                                        enabled = !isSelected,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) DarkAlisePurpleDark else DarkAliseNeon
                                        ),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(if (isSelected) "Active" else "Select", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> { // Export
                    item {
                        SettingsCard(title = "Export / Import Configuration") {
                            Button(
                                onClick = {
                                    exportJsonString = settingsRepository.exportToJson(includeStreamKey = false)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkAlisePurpleDark)
                            ) {
                                Text("Generate Clean JSON Export", color = DarkAliseNeon)
                            }
                            if (exportJsonString.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = exportJsonString,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Exported JSON") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                5 -> { // Virtual Camera
                    item {
                        SettingsCard(title = "Virtual Camera Output") {
                            Text(
                                text = "Virtual camera output is not supported on this Android configuration.",
                                color = DarkAliseTextMuted,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Android does not publicly expose a virtual camera hardware HAL driver to third-party applications without system-level firmware privileges. The Dark Alise OBS architecture remains decoupled and extensible for future Android VirtualCameraProvider APIs.",
                                color = DarkAliseTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkAliseSurface, RoundedCornerShape(8.dp))
            .border(1.dp, DarkAliseBorder, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Text(
            text = title,
            color = DarkAliseNeon,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
