package com.chin.minddump.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chin.minddump.storage.EntryRole
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.Space
import kotlinx.coroutines.flow.Flow

// ── Statistics query result classes ──

data class DayCount(
    val monthFolder: String,
    val count: Int,
)

data class TypeCount(
    val type: EntryType,
    val count: Int,
)

data class HourCount(
    val hour: Int,
    val count: Int,
)

@Dao
@Suppress("TooManyFunctions")
interface EntryDao {

    @Query("SELECT * FROM entries WHERE space = :space ORDER BY lastModified DESC")
    fun getAll(space: Space): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE space = :space")
    suspend fun getAllSnapshot(space: Space): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE space = :space AND monthFolder = :month ORDER BY lastModified DESC")
    fun getByMonth(space: Space, month: String): Flow<List<EntryEntity>>

    @Query(
        """
        SELECT e.* FROM entries e
        JOIN entries_fts fts ON e.id = fts.rowid
        WHERE fts.entries_fts MATCH :query AND e.space = :space
        ORDER BY e.lastModified DESC
        """,
    )
    fun search(space: Space, query: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE space = :space AND role = :role ORDER BY lastModified DESC")
    fun getByRole(space: Space, role: EntryRole): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE targetTimestamp = :targetTs AND space = :space ORDER BY lastModified DESC")
    fun getCommentsFor(space: Space, targetTs: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE groupPath = :groupPath ORDER BY lastModified DESC")
    fun getEntriesInGroup(groupPath: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE groupPath = :groupPath")
    suspend fun getEntriesInGroupSnapshot(groupPath: String): List<EntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: EntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<EntryEntity>)

    @Delete
    suspend fun delete(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE filePath = :filePath")
    suspend fun deleteByPath(filePath: String)

    @Query("DELETE FROM entries WHERE filePath IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    /** Drop every row — used by the full rebuild-from-disk path. */
    @Query("DELETE FROM entries")
    suspend fun clearAll()

    /** Force a full rebuild of the FTS index from the content table. */
    @Query("INSERT INTO entries_fts(entries_fts) VALUES('rebuild')")
    suspend fun rebuildFtsIndex()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(entries: List<EntryEntity>)

    @Query("SELECT COUNT(*) FROM entries WHERE space = :space")
    suspend fun count(space: Space): Int

    @Query("SELECT COUNT(*) FROM entries")
    suspend fun countAll(): Int

    // ── Statistics queries ──

    /** Entry count grouped by monthFolder, ordered by month descending. */
    @Query(
        """
        SELECT monthFolder, COUNT(*) as count
        FROM entries
        WHERE space = :space
        GROUP BY monthFolder
        ORDER BY monthFolder DESC
        LIMIT :limit
        """,
    )
    fun getEntryCountByDay(space: Space, limit: Int = 90): Flow<List<DayCount>>

    /** Entry count grouped by type. */
    @Query(
        """
        SELECT type, COUNT(*) as count
        FROM entries
        WHERE space = :space
        GROUP BY type
        """,
    )
    fun getEntryCountByType(space: Space): Flow<List<TypeCount>>

    /** Entry count grouped by hour of day.
     *  Timestamp format is yymm-dd-HHMMSS, so hour is at position 7-8 (1-indexed). */
    @Query(
        """
        SELECT CAST(SUBSTR(timestamp, 7, 2) AS INTEGER) as hour, COUNT(*) as count
        FROM entries
        WHERE space = :space
        GROUP BY hour
        ORDER BY hour
        """,
    )
    fun getHourlyDistribution(space: Space): Flow<List<HourCount>>

    /** Total entry count for a space, as Flow for reactivity. */
    @Query("SELECT COUNT(*) FROM entries WHERE space = :space")
    fun countFlow(space: Space): Flow<Int>
}
