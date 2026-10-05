package com.darkalise.obs.core.storage

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordingItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAddedMillis: Long,
    val durationMillis: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val thumbnailBitmap: Bitmap? = null
) {
    val formattedDuration: String
        get() {
            val totalSec = durationMillis / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return String.format(Locale.US, "%02d:%02d", min, sec)
        }

    val formattedSize: String
        get() {
            val mb = sizeBytes.toDouble() / (1024 * 1024)
            return String.format(Locale.US, "%.1f MB", mb)
        }

    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(dateAddedMillis))
}

class RecordingsManager(private val context: Context) {

    companion object {
        private const val TAG = "DarkAlise_Recordings"
    }

    private val _recordings = MutableStateFlow<List<RecordingItem>>(emptyList())
    val recordings: StateFlow<List<RecordingItem>> = _recordings.asStateFlow()

    suspend fun loadRecordings() = withContext(Dispatchers.IO) {
        val list = mutableListOf<RecordingItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )

        // Query files with DarkAlise prefix
        val selection = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("DarkAlise_%")
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol)
                    val dateAdded = cursor.getLong(dateCol) * 1000L
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    var thumb: Bitmap? = null
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            thumb = context.contentResolver.loadThumbnail(contentUri, Size(320, 180), null)
                        }
                    } catch (ignored: Exception) {}

                    list.add(
                        RecordingItem(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            dateAddedMillis = dateAdded,
                            durationMillis = duration,
                            sizeBytes = size,
                            width = width,
                            height = height,
                            thumbnailBitmap = thumb
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore recordings", e)
        }

        _recordings.value = list
    }

    fun shareRecording(item: RecordingItem) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Recording").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun openInExternalApp(item: RecordingItem) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(item.uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun renameRecording(item: RecordingItem, newName: String): Boolean = withContext(Dispatchers.IO) {
        val cleanName = if (newName.endsWith(".mp4", ignoreCase = true)) newName else "$newName.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, cleanName)
        }
        try {
            val rows = context.contentResolver.update(item.uri, values, null, null)
            if (rows > 0) {
                loadRecordings()
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rename recording", e)
        }
        false
    }

    suspend fun deleteRecording(item: RecordingItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val rows = context.contentResolver.delete(item.uri, null, null)
            if (rows > 0) {
                loadRecordings()
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recording", e)
        }
        false
    }
}
