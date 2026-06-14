package com.chin.minddump.data

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [EntryEntity::class, EntryFts::class],
    version = 4,
    exportSchema = true,
)
abstract class MindDumpDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
