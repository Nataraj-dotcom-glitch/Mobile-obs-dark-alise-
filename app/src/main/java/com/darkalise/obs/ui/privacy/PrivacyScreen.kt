package com.darkalise.obs.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkalise.obs.ui.theme.DarkAliseBlack
import com.darkalise.obs.ui.theme.DarkAliseBorder
import com.darkalise.obs.ui.theme.DarkAliseNeon
import com.darkalise.obs.ui.theme.DarkAliseSurface
import com.darkalise.obs.ui.theme.DarkAliseText
import com.darkalise.obs.ui.theme.DarkAliseTextMuted

@Composable
fun PrivacyScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkAliseBlack)
            .padding(16.dp)
    ) {
        Text(
            text = "PRIVACY & SECURITY CHARTER",
            color = DarkAliseNeon,
            fontSize = 16.sp,
            letterSpacing = 1.sp
        )
        Text(
            text = "Local-First Architecture · Zero Telemetry · No Third-Party Tracking",
            color = DarkAliseTextMuted,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PrivacyItem(
                    title = "1. Screen Capture (MediaProjection)",
                    description = "Screen capture frames are encoded directly into your local hardware encoder (MediaCodec). No screen content is ever uploaded to any cloud server or analytics provider."
                )
            }
            item {
                PrivacyItem(
                    title = "2. Camera & Microphone",
                    description = "Camera frames and microphone audio are mixed in real time strictly for your active broadcast or recording session. Dark Alise OBS does not monitor background audio when idle."
                )
            }
            item {
                PrivacyItem(
                    title = "3. Scoped Storage & MediaStore",
                    description = "Recordings are saved strictly to the standard Android Movies collection. No broad legacy external storage access is required or requested."
                )
            }
            item {
                PrivacyItem(
                    title = "4. Stream Keys & RTMP",
                    description = "Your RTMP stream keys and credentials are stored strictly in private device SharedPreferences. They are never sent to external telemetry or third-party servers."
                )
            }
            item {
                PrivacyItem(
                    title = "5. No Accounts & No Analytics",
                    description = "Dark Alise OBS requires no account, no login, and no subscription. It runs entirely offline for local recording and connects directly to your user-specified RTMP server for live streaming."
                )
            }
        }
    }
}

@Composable
fun PrivacyItem(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkAliseSurface, RoundedCornerShape(8.dp))
            .border(1.dp, DarkAliseBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(text = title, color = DarkAliseNeon, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = description, color = DarkAliseText, fontSize = 11.sp, lineHeight = 16.sp)
    }
}
