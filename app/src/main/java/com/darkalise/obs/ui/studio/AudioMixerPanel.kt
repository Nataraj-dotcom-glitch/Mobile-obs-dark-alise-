package com.darkalise.obs.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkalise.obs.core.audio.AudioChannelState
import com.darkalise.obs.ui.theme.DarkAliseBorder
import com.darkalise.obs.ui.theme.DarkAliseGreen
import com.darkalise.obs.ui.theme.DarkAliseNeon
import com.darkalise.obs.ui.theme.DarkAliseRed
import com.darkalise.obs.ui.theme.DarkAliseSurface
import com.darkalise.obs.ui.theme.DarkAliseSurfaceVariant
import com.darkalise.obs.ui.theme.DarkAliseText
import com.darkalise.obs.ui.theme.DarkAliseTextMuted
import java.util.Locale

@Composable
fun AudioMixerPanel(
    channels: List<AudioChannelState>,
    onVolumeChanged: (String, Float) -> Unit,
    onMuteToggled: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkAliseSurface, RoundedCornerShape(8.dp))
            .border(1.dp, DarkAliseBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AUDIO MIXER",
                color = DarkAliseNeon,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = "dBFS PEAK / RMS",
                color = DarkAliseTextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        channels.forEach { channel ->
            AudioChannelRow(
                channel = channel,
                onVolumeChanged = { onVolumeChanged(channel.id, it) },
                onMuteToggled = { onMuteToggled(channel.id) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
fun AudioChannelRow(
    channel: AudioChannelState,
    onVolumeChanged: (Float) -> Unit,
    onMuteToggled: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkAliseSurfaceVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute / Unmute Button
        IconButton(
            onClick = onMuteToggled,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (channel.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                contentDescription = "Mute",
                tint = if (channel.isMuted) DarkAliseRed else DarkAliseNeon,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Channel Info & Level Meter
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = channel.name,
                    color = DarkAliseText,
                    fontSize = 12.sp
                )
                Text(
                    text = if (channel.isMuted) "MUTED" else String.format(Locale.US, "%.1f dB", channel.peakDb),
                    color = if (channel.isMuted) DarkAliseRed else DarkAliseTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Peak Meter Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1E293B))
            ) {
                val fillRatio = if (channel.isMuted) 0f else channel.rmsLevel.coerceIn(0f, 1f)
                val meterColor = when {
                    channel.peakDb > -3f -> DarkAliseRed
                    channel.peakDb > -12f -> Color(0xFFEAB308) // Yellow
                    else -> DarkAliseGreen
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fillRatio)
                        .background(meterColor)
                )
            }

            // Volume Slider
            Slider(
                value = channel.volume,
                onValueChange = onVolumeChanged,
                valueRange = 0f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = DarkAliseNeon,
                    activeTrackColor = DarkAliseNeon,
                    inactiveTrackColor = Color(0xFF2E2A42)
                ),
                modifier = Modifier.height(24.dp)
            )
        }
    }
}
