package com.chin.minddump.data

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.chin.minddump.storage.EntryRole
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.Space
import com.chin.minddump.storage.TodoState
import java.io.File

@Entity(
    tableName = "entries",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["space", "monthFolder"]),
        Index(value = ["space", "type"]),
        Index(value = ["space", "role"]),
    ],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val type: EntryType,
    val space: Space,
    val monthFolder: String,
    val timestamp: String,
    val contentPreview: String,
    val lastModified: Long,
    val isEncrypted: Boolean = false,
    val role: EntryRole = EntryRole.FILE,
    val targetTimestamp: String? = null,
    val groupPath: String? = null,
    val isPinned: Boolean = false,
    val todoState: TodoState = TodoState.NONE,
)

fun EntryEntity.toEntry(): MindDumpEntry =
    MindDumpEntry(
        file = File(filePath),
        type = type,
        space = space,
        monthFolder = monthFolder,
        timestamp = timestamp,
        role = role,
        targetTimestamp = targetTimestamp,
        groupPath = groupPath,
        isPinned = isPinned,
        todoState = todoState,
    )

fun MindDumpEntry.toEntity(contentPreview: String = "", isEncrypted: Boolean = false): EntryEntity =
    EntryEntity(
        filePath = file.absolutePath,
        type = type,
        space = space,
        monthFolder = monthFolder,
        timestamp = timestamp,
        contentPreview = contentPreview,
        lastModified = file.lastModified(),
        isEncrypted = isEncrypted,
        role = role,
        targetTimestamp = targetTimestamp,
        groupPath = groupPath,
        isPinned = isPinned,
        todoState = todoState,
    )
