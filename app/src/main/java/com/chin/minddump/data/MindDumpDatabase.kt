package com.chin.minddump.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EntryEntity::class, EntryFts::class],
    version = 1,
    exportSchema = true,
)
abstract class MindDumpDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
