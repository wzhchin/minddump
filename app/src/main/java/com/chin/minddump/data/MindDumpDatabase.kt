package com.chin.minddump.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [EntryEntity::class, EntryFts::class],
    version = 2,
    exportSchema = true,
)
abstract class MindDumpDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}

@Suppress("MagicNumber")
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Deduplicate by filePath — keep the row with the lowest id
        db.execSQL(
            """
            DELETE FROM entries
            WHERE id NOT IN (
                SELECT MIN(id) FROM entries GROUP BY filePath
            )
            """.trimIndent(),
        )
        // Unique index on filePath
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_entries_filePath` ON `entries` (`filePath`)")
        // Composite indexes for statistics queries
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entries_space_dateFolder` ON `entries` (`space`, `dateFolder`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entries_space_type` ON `entries` (`space`, `type`)")
    }
}
