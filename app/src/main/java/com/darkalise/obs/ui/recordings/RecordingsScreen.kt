package com.darkalise.obs.ui.recordings

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkalise.obs.core.storage.RecordingItem
import com.darkalise.obs.core.storage.RecordingsManager
import com.darkalise.obs.ui.theme.DarkAliseBlack
import com.darkalise.obs.ui.theme.DarkAliseBorder
import com.darkalise.obs.ui.theme.DarkAliseNeon
import com.darkalise.obs.ui.theme.DarkAlisePurpleDark
import com.darkalise.obs.ui.theme.DarkAliseRed
import com.darkalise.obs.ui.theme.DarkAliseSurface
import com.darkalise.obs.ui.theme.DarkAliseSurfaceVariant
import com.darkalise.obs.ui.theme.DarkAliseText
import com.darkalise.obs.ui.theme.DarkAliseTextMuted
import kotlinx.coroutines.launch

@Composable
fun RecordingsScreen(
    recordingsManager: RecordingsManager,
    modifier: Modifier = Modifier
) {
    val recordings by recordingsManager.recordings.collectAsState()
    val scope = rememberCoroutineScope()

    var itemToDelete by remember { mutableStateOf<RecordingItem?>(null) }
    var itemToRename by remember { mutableStateOf<RecordingItem?>(null) }
    var renameInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkAliseBlack)
            .padding(16.dp)
    ) {
        Text(
            text = "RECORDINGS LIBRARY",
            color = DarkAliseNeon,
            fontSize = 16.sp,
            letterSpacing = 1.sp
        )
        Text(
            text = "Stored in Movies/Dark Alise OBS (MediaStore)",
            color = DarkAliseTextMuted,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkAliseSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, DarkAliseBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recordings found yet.\nPress Record in Studio to create an MP4.",
                    color = DarkAliseTextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recordings) { item ->
                    RecordingCard(
                        item = item,
                        onPlay = { recordingsManager.openInExternalApp(item) },
                        onShare = { recordingsManager.shareRecording(item) },
                        onRename = {
                            itemToRename = item
                            renameInput = item.displayName.removeSuffix(".mp4")
                        },
                        onDelete = { itemToDelete = item }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Recording?") },
            text = { Text("Are you sure you want to permanently delete '${item.displayName}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { recordingsManager.deleteRecording(item) }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkAliseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    itemToRename?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = { Text("Rename Recording") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Filename") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { recordingsManager.renameRecording(item, renameInput) }
                        itemToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkAliseNeon)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecordingCard(
    item: RecordingItem,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkAliseSurface, RoundedCornerShape(8.dp))
            .border(1.dp, DarkAliseBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail or Placeholder
        Box(
            modifier = Modifier
                .size(80.dp, 50.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DarkAliseSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (item.thumbnailBitmap != null) {
                Image(
                    bitmap = item.thumbnailBitmap.asImageBitmap(),
                    contentDescription = "Thumbnail",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("MP4", color = DarkAliseTextMuted, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                color = DarkAliseText,
                fontSize = 12.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.formattedDuration} · ${item.formattedSize} · ${item.formattedDate}",
                color = DarkAliseTextMuted,
                fontSize = 10.sp
            )
        }

        // Actions
        Row {
            IconButton(onClick = onPlay, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = DarkAliseNeon, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = DarkAliseTextMuted, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DarkAliseRed, modifier = Modifier.size(16.dp))
            }
        }
    }
}
