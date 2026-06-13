package com.chin.minddump.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * A chip that displays a document/file attachment with an appropriate icon.
 * Ported from LastChat.
 */
@Composable
fun DocumentChip(
    fileName: String,
    mimeType: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val icon = getFileIcon(fileName, mimeType)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .let { baseModifier ->
                if (onClick != null) {
                    baseModifier.clickable { onClick() }
                } else {
                    baseModifier
                }
            },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (onRemove != null) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(18.dp)
                    .clickable { onRemove() },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Returns appropriate icon based on file extension and MIME type.
 */
private fun getFileIcon(fileName: String, mimeType: String?): ImageVector {
    val extension = fileName.substringAfterLast('.', "").lowercase()

    return when (extension) {
        "pdf" -> Icons.Rounded.PictureAsPdf
        "doc", "docx", "odt", "rtf" -> Icons.Rounded.Description
        "txt", "md", "markdown" -> Icons.AutoMirrored.Rounded.TextSnippet
        "xls", "xlsx", "csv", "ods" -> Icons.Rounded.TableChart
        "py", "js", "ts", "tsx", "jsx", "kt", "java", "c", "cpp", "h", "hpp",
        "cs", "go", "rs", "rb", "php", "swift", "dart", "lua", "r", "scala",
        "html", "css", "scss", "sass", "less", "xml", "yaml", "yml", "toml" -> Icons.Rounded.Code
        "json" -> Icons.Rounded.DataObject
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "ico" -> Icons.Rounded.Image
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm" -> Icons.Rounded.VideoFile
        "mp3", "wav", "ogg", "flac", "aac", "m4a", "wma" -> Icons.Rounded.AudioFile
        "zip", "rar", "7z", "tar", "gz", "bz2" -> Icons.Rounded.FolderZip
        else -> {
            when {
                mimeType?.startsWith("text/") == true -> Icons.AutoMirrored.Rounded.TextSnippet
                mimeType?.startsWith("image/") == true -> Icons.Rounded.Image
                mimeType?.startsWith("video/") == true -> Icons.Rounded.VideoFile
                mimeType?.startsWith("audio/") == true -> Icons.Rounded.AudioFile
                mimeType == "application/pdf" -> Icons.Rounded.PictureAsPdf
                mimeType?.contains("zip") == true || mimeType?.contains("compressed") == true -> Icons.Rounded.FolderZip
                else -> Icons.AutoMirrored.Rounded.InsertDriveFile
            }
        }
    }
}
