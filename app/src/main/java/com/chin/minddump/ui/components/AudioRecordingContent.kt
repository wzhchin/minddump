package com.chin.minddump.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.ui.openFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Inset/rhythm shared with the unified card scaffold in EntryItem.kt.
private val BODY_INSET = 16.dp
private val BODY_RHYTHM = 12.dp

/**
 * Recording card body (m3e-restrained-cards): a circular play control and the
 * file name + duration/size. The play control reuses the existing
 * external-playback path via [openFile] (no in-app audio player exists yet).
 *
 * @param interactable when false (e.g. multi-select), the play control is static.
 */
@Composable
fun AudioRecordingContent(
    entry: MindDumpEntry,
    interactable: Boolean = true,
) {
    val context = LocalContext.current
    val durationMillis by produceState<Long>(initialValue = 0L, key1 = entry.file) {
        value = withContext(Dispatchers.IO) { audioDurationMs(entry.file) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = BODY_INSET, end = BODY_INSET, bottom = BODY_RHYTHM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .size(44.dp)
                .then(
                    if (interactable) {
                        Modifier.clickable { openFile(context, entry.file) }
                    } else {
                        Modifier
                    },
                ),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier
                    .padding(12.dp)
                    .size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = entry.file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatAudioMeta(durationMillis, entry.file),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Read the recording's duration via MediaMetadataRetriever (off the main thread). */
private fun audioDurationMs(file: File): Long {
    val retriever = android.media.MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        val duration = retriever.extractMetadata(
            android.media.MediaMetadataRetriever.METADATA_KEY_DURATION,
        )
        duration?.toLongOrNull() ?: 0L
    } catch (_: Exception) {
        0L
    } finally {
        runCatching { retriever.release() }
    }
}

/** "m:ss · N KB" meta line for a recording. */
private fun formatAudioMeta(durationMillis: Long, file: File): String {
    val totalSeconds = (durationMillis / 1000).coerceAtLeast(0L)
    val mm = totalSeconds / 60
    val ss = totalSeconds % 60
    val sizeKb = file.length() / 1024
    return "%d:%02d · %d KB".format(mm, ss, sizeKb)
}
