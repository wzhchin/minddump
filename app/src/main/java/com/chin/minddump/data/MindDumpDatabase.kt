package com.chin.minddump.data

import androidx.room3.Database
import androidx.room3.RoomDatabase

/**
 * Version 6: the god-table `entries` is split into `entries` (content + group
 * containers), `comments`, `tags`, and `events`. See the `split-entries-god-table`
 * OpenSpec change. Schema evolves by destructive rebuild-from-disk (Room3 has no
 * migration machinery; the filesystem is the source of truth).
 */
@Database(
    entities = [EntryEntity::class, CommentEntity::class, TagEntity::class, EventEntity::class, EntryFts::class],
    version = 6,
    exportSchema = true,
)
abstract class MindDumpDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
