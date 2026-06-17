package com.chin.minddump.data

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.chin.minddump.storage.EntryEvent
import com.chin.minddump.storage.EntryMeta
import com.chin.minddump.storage.EntryRole
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.META_TAGS_SEPARATOR
import com.chin.minddump.storage.MetaYamlCodec
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
    // Tags joined by META_TAGS_SEPARATOR for SQL filtering; "" when none.
    val tags: String = "",
    // Events serialized via MetaYamlCodec (the `events:` block text), "" when none.
    val events: String = "",
    // True when a Private sidecar exists but was not decrypted (lazy until unlock).
    val metaEncrypted: Boolean = false,
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
        tags = EntryTagsCodec.decode(tags),
        events = EntryEventCodec.decode(events),
        metaEncrypted = metaEncrypted,
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
        tags = EntryTagsCodec.encode(tags),
        events = EntryEventCodec.encode(events),
        metaEncrypted = metaEncrypted,
    )

/**
 * Encodes/decodes the tag list to/from a separator-joined DB column. The stored
 * form is wrapped with a leading and trailing [META_TAGS_SEPARATOR] sentinel so
 * that SQL `GLOB '*<SEP>tag<SEP>*'` matches any tag position (including a
 * single-tag row) unambiguously.
 */
object EntryTagsCodec {
    fun encode(tags: List<String>): String =
        if (tags.isEmpty()) {
            ""
        } else {
            META_TAGS_SEPARATOR + tags.joinToString(META_TAGS_SEPARATOR) + META_TAGS_SEPARATOR
        }

    fun decode(stored: String): List<String> {
        if (stored.isBlank()) return emptyList()
        return stored
            .split(META_TAGS_SEPARATOR)
            .filter { it.isNotEmpty() }
    }
}

/**
 * Encodes/decodes the event list to/from the DB column by reusing the YAML
 * codec's `events:` block. Round-trips through [MetaYamlCodec].
 */
object EntryEventCodec {
    fun encode(events: List<EntryEvent>): String =
        if (events.isEmpty()) "" else MetaYamlCodec.encode(EntryMeta(events = events))

    fun decode(stored: String): List<EntryEvent> =
        if (stored.isBlank()) emptyList() else MetaYamlCodec.decode(stored).events
}
