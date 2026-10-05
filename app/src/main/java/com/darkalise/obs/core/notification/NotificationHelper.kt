package com.darkalise.obs.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.darkalise.obs.R
import com.darkalise.obs.ui.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "dark_alise_obs_recording_channel"
    const val NOTIFICATION_ID = 4040

    const val ACTION_STOP = "com.darkalise.obs.ACTION_STOP"
    const val ACTION_PAUSE = "com.darkalise.obs.ACTION_PAUSE"
    const val ACTION_RESUME = "com.darkalise.obs.ACTION_RESUME"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_recording_name)
            val desc = context.getString(R.string.channel_recording_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = desc
                setShowBadge(false)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildForegroundNotification(
        context: Context,
        isRecording: Boolean,
        isStreaming: Boolean,
        durationFormatted: String,
        details: String
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            isRecording && isStreaming -> "DARK ALISE OBS: LIVE & RECORDING ($durationFormatted)"
            isStreaming -> "DARK ALISE OBS: LIVE ($durationFormatted)"
            isRecording -> "DARK ALISE OBS: RECORDING ($durationFormatted)"
            else -> "DARK ALISE OBS: BROADCAST READY"
        }

        val stopIntent = Intent(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(details)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
