package com.chin.minddump.data

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.chin.minddump.storage.EntryEvent
import com.chin.minddump.storage.EntryRole
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.Space
import com.chin.minddump.storage.TodoState
import java.io.File

/**
 * Content + group-container rows. A file is a note; a directory is a group
 * (type == GROUP). Comments are removed.
 *
 * Identity is [filePath]: the file's absolute path. The OS guarantees
 * uniqueness within a directory, so this is a globally unique, rebuild-stable
 * primary key — no synthesized `tid`. A `workDir` move always triggers a full
 * rebuild, so absolute paths re-root at rebuild time without staleness.
 * Membership in a group is positional, captured by [groupPath] (the owning
 * group directory's absolute path, null at the month-bucket root). Groups are a
 * single level, so [groupPath] never references another group's interior.
 */
@Entity(
    tableName = "entries",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["space", "monthFolder"]),
        Index(value = ["space", "type"]),
        Index(value = ["groupPath"]),
    ],
)
data class EntryEntity(
    @PrimaryKey
    val filePath: String,
    val type: EntryType,
    val space: Space,
    val monthFolder: String,
    val lastModified: Long,
    val isEncrypted: Boolean = false,
    val groupPath: String? = null,
    val isPinned: Boolean = false,
    val todoState: TodoState = TodoState.NONE,
    val metaEncrypted: Boolean = false,
    val contentPreview: String = "",
)

fun EntryEntity.toEntry(tags: List<String> = emptyList(), events: List<EntryEvent> = emptyList()): MindDumpEntry =
    MindDumpEntry(
        file = File(filePath),
        type = type,
        space = space,
        monthFolder = monthFolder,
        role = if (type == EntryType.GROUP) EntryRole.GROUP else EntryRole.FILE,
        groupPath = groupPath,
        isPinned = isPinned,
        todoState = todoState,
        tags = tags,
        events = events,
        metaEncrypted = metaEncrypted,
    )

fun MindDumpEntry.toEntity(
    contentPreview: String = "",
    isEncrypted: Boolean = false,
): EntryEntity =
    EntryEntity(
        filePath = file.absolutePath,
        type = type,
        space = space,
        monthFolder = monthFolder,
        lastModified = file.lastModified(),
        isEncrypted = isEncrypted,
        groupPath = groupPath,
        isPinned = isPinned,
        todoState = todoState,
        metaEncrypted = metaEncrypted,
        contentPreview = contentPreview,
    )

/**
 * Flat tag relation: one row per (owner, tag). Composite primary key gives
 * natural deduplication. The `.meta` sidecar on disk is the authority; this
 * table is a rebuildable index keyed by the owner's file path.
 */
@Entity(
    tableName = "tags",
    primaryKeys = ["filePath", "tag"],
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["filePath"],
            childColumns = ["filePath"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tag"])],
)
data class TagEntity(
    val filePath: String,
    val tag: String,
)

/**
 * Scheduled-event relation: one row per event. Addressed by the autogen [id]
 * (the scheduler cancels/re-arms by id). The sidecar remains the authority;
 * this table is a rebuildable index keyed by the owner's file path.
 */
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["filePath"],
            childColumns = ["filePath"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["filePath"]), Index(value = ["due"])],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val due: String, // ISO local date-time, e.g. 2026-06-20T09:00
    val state: String, // EventState name
    val trigger: String, // EventTrigger name
)

/** Encode/decode between [EntryEvent]s and [EventEntity] rows (the sidecar is authority). */
fun EntryEvent.toEventEntity(filePath: String): EventEntity =
    EventEntity(filePath = filePath, due = due.toString(), state = state.name, trigger = trigger.name)

fun EventEntity.toEntryEvent(): EntryEvent =
    EntryEvent(
        due = java.time.LocalDateTime.parse(due),
        state = com.chin.minddump.storage.EventState
            .valueOf(state),
        trigger = com.chin.minddump.storage.EventTrigger
            .valueOf(trigger),
    )
