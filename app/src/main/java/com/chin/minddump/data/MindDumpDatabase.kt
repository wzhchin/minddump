package com.chin.minddump.data

import androidx.room3.Database
import androidx.room3.RoomDatabase

/**
 * Version 7: identity is the file path (no synthesized `tid`); the `comments`
 * table is removed; group membership is a single-level `groupPath`; tags/events
 * relations are keyed by `filePath`. See the `restrained-file-storage` OpenSpec
 * change. Schema evolves by destructive rebuild-from-disk (Room3 has no
 * migration machinery; the filesystem is the source of truth).
 */
@Database(
    entities = [EntryEntity::class, TagEntity::class, EventEntity::class, EntryFts::class],
    version = 7,
    exportSchema = true,
)
abstract class MindDumpDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
