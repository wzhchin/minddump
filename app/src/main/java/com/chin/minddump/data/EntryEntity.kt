package com.chin.minddump.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.Space
import java.io.File

@Entity(
    tableName = "entries",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["space", "dateFolder"]),
        Index(value = ["space", "type"]),
    ],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val type: EntryType,
    val space: Space,
    val dateFolder: String,
    val timestamp: String,
    val contentPreview: String,
    val lastModified: Long,
    val isEncrypted: Boolean = false,
)

fun EntryEntity.toEntry(): MindDumpEntry =
    MindDumpEntry(
        file = File(filePath),
        type = type,
        space = space,
        dateFolder = dateFolder,
        timestamp = timestamp,
    )

fun MindDumpEntry.toEntity(contentPreview: String = "", isEncrypted: Boolean = false): EntryEntity =
    EntryEntity(
        filePath = file.absolutePath,
        type = type,
        space = space,
        dateFolder = dateFolder,
        timestamp = timestamp,
        contentPreview = contentPreview,
        lastModified = file.lastModified(),
        isEncrypted = isEncrypted,
    )
