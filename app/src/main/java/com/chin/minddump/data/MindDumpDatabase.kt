package com.chin.minddump.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [EntryEntity::class, EntryFts::class],
    version = 3,
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

/**
 * Migration 2→3: Restructure for new file naming convention.
 * Old data is incompatible — destructive rebuild.
 * Adds: role, targetTimestamp, groupPath columns.
 * Renames: dateFolder → monthFolder.
 */
@Suppress("MagicNumber")
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new columns
        db.execSQL("ALTER TABLE entries ADD COLUMN role TEXT NOT NULL DEFAULT 'f'")
        db.execSQL("ALTER TABLE entries ADD COLUMN targetTimestamp TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE entries ADD COLUMN groupPath TEXT DEFAULT NULL")

        // Rename dateFolder → monthFolder: SQLite < 3.25 doesn't have ALTER TABLE RENAME COLUMN,
        // so we recreate the table
        db.execSQL(
            """
            CREATE TABLE entries_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                filePath TEXT NOT NULL,
                type TEXT NOT NULL,
                space TEXT NOT NULL,
                monthFolder TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                contentPreview TEXT NOT NULL,
                lastModified INTEGER NOT NULL,
                isEncrypted INTEGER NOT NULL DEFAULT 0,
                role TEXT NOT NULL DEFAULT 'f',
                targetTimestamp TEXT DEFAULT NULL,
                groupPath TEXT DEFAULT NULL
            )
            """.trimIndent(),
        )

        // Copy data — convert dateFolder (YYYY-MM-DD) to monthFolder (YYYY-MM)
        db.execSQL(
            """
            INSERT INTO entries_new (id, filePath, type, space, monthFolder, timestamp, contentPreview, lastModified, isEncrypted, role, targetTimestamp, groupPath)
            SELECT id, filePath, type, space, SUBSTR(dateFolder, 1, 7), timestamp, contentPreview, lastModified, isEncrypted, 'f', NULL, NULL
            FROM entries
            """.trimIndent(),
        )

        // Drop old table and rename
        db.execSQL("DROP TABLE entries")
        db.execSQL("ALTER TABLE entries_new RENAME TO entries")

        // Recreate indexes
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_entries_filePath` ON `entries` (`filePath`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entries_space_monthFolder` ON `entries` (`space`, `monthFolder`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entries_space_type` ON `entries` (`space`, `type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entries_space_role` ON `entries` (`space`, `role`)")

        // Recreate FTS table
        db.execSQL("DROP TABLE IF EXISTS entries_fts")
        db.execSQL(
            """
            CREATE VIRTUAL TABLE entries_fts USING fts4(
                content='entries',
                contentPreview,
                monthFolder
            )
            """.trimIndent(),
        )
        // Rebuild FTS index
        db.execSQL("INSERT INTO entries_fts(entries_fts) VALUES('rebuild')")
    }
}
