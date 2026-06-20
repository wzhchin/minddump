package com.chin.minddump.data

import androidx.room3.Entity
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
 * Content + group-container rows. COMMENT rows live in [CommentEntity]; a comment
 * is reconstructed as a [MindDumpEntry] with `role == COMMENT` for the UI.
 *
 * Identity is [tid] (rebuild-stable epoch-millis + same-second offset). Membership
 * and nesting are expressed by [parentId], which references another row's tid (the
 * owning group for a member, the parent group for a nested group; null at root).
 */
@Entity(
    tableName = "entries",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["space", "monthFolder"]),
        Index(value = ["space", "type"]),
        Index(value = ["parentId"]),
    ],
)
data class EntryEntity(
    @PrimaryKey
    val tid: Long,
    val filePath: String,
    val type: EntryType,
    val space: Space,
    val monthFolder: String,
    val lastModified: Long,
    val isEncrypted: Boolean = false,
    val parentId: Long? = null,
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
        tid = tid,
        role = if (type == EntryType.GROUP) EntryRole.GROUP else EntryRole.FILE,
        parentId = parentId,
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
        tid = tid,
        filePath = file.absolutePath,
        type = type,
        space = space,
        monthFolder = monthFolder,
        lastModified = file.lastModified(),
        isEncrypted = isEncrypted,
        parentId = parentId,
        isPinned = isPinned,
        todoState = todoState,
        metaEncrypted = metaEncrypted,
        contentPreview = contentPreview,
    )

/**
 * Comment rows, decoupled from the content/group table. A comment links to its owner
 * by [targetTid] (the owner's tid). It is reconstructed as a [MindDumpEntry] with
 * `role == COMMENT` for the UI, so the existing comment presentation is unchanged.
 *
 * `tid` derives from the comment's own capture timestamp + collision offset (the
 * `nowTs` in `{targetTs}-n-{nowTs}.md`); `targetTid` derives from the owner's
 * `targetTs`. A dangling [targetTid] (owner deleted) renders as an orphan comment.
 */
@Entity(
    tableName = "comments",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["targetTid"]),
    ],
)
data class CommentEntity(
    @PrimaryKey
    val tid: Long,
    val targetTid: Long,
    val filePath: String,
    val space: Space,
    val contentPreview: String = "",
    val lastModified: Long,
    val isEncrypted: Boolean = false,
)

fun CommentEntity.toEntry(): MindDumpEntry =
    MindDumpEntry(
        file = File(filePath),
        type = EntryType.TEXT,
        space = space,
        monthFolder = "", // comments do not carry a month folder; display uses timestamp
        tid = tid,
        role = EntryRole.COMMENT,
        targetTid = targetTid,
    )

/**
 * Flat tag relation: one row per (owner, tag). Composite primary key gives natural
 * deduplication. Replaces the former separator-joined `tags` column (and fixes the
 * empty-`META_TAGS_SEPARATOR` bug). The sidecar on disk remains the authority; this
 * table is a rebuildable index.
 */
@Entity(
    tableName = "tags",
    primaryKeys = ["tid", "tag"],
    foreignKeys = [
        androidx.room3.ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["tid"],
            childColumns = ["tid"],
            onDelete = androidx.room3.ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tag"])],
)
data class TagEntity(
    val tid: Long,
    val tag: String,
)

/**
 * Scheduled-event relation: one row per event. Addressed by the autogen [id] (the
 * scheduler cancels/re-arms by id). The sidecar remains the authority; this table
 * is a rebuildable index. Replaces the former YAML-joined `events` column.
 */
@Entity(
    tableName = "events",
    foreignKeys = [
        androidx.room3.ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["tid"],
            childColumns = ["tid"],
            onDelete = androidx.room3.ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tid"]), Index(value = ["due"])],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tid: Long,
    val due: String, // ISO local date-time, e.g. 2026-06-20T09:00
    val state: String, // EventState name
    val trigger: String, // EventTrigger name
)

/** Encode/decode between [EntryEvent]s and [EventEntity] rows (the sidecar is authority). */
fun EntryEvent.toEventEntity(tid: Long): EventEntity =
    EventEntity(tid = tid, due = due.toString(), state = state.name, trigger = trigger.name)

fun EventEntity.toEntryEvent(): EntryEvent =
    EntryEvent(
        due = java.time.LocalDateTime.parse(due),
        state = com.chin.minddump.storage.EventState
            .valueOf(state),
        trigger = com.chin.minddump.storage.EventTrigger
            .valueOf(trigger),
    )
