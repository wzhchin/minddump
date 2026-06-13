package com.chin.minddump.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.Space
import kotlinx.coroutines.flow.Flow

// ── Statistics query result classes ──

data class DayCount(val dateFolder: String, val count: Int)

data class TypeCount(val type: EntryType, val count: Int)

data class HourCount(val hour: Int, val count: Int)

@Dao
interface EntryDao {

    @Query("SELECT * FROM entries WHERE space = :space ORDER BY lastModified DESC")
    fun getAll(space: Space): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE space = :space AND dateFolder = :date ORDER BY lastModified DESC")
    fun getByDate(space: Space, date: String): Flow<List<EntryEntity>>

    @Query(
        """
        SELECT e.* FROM entries e
        JOIN entries_fts fts ON e.id = fts.rowid
        WHERE fts.entries_fts MATCH :query AND e.space = :space
        ORDER BY e.lastModified DESC
        """,
    )
    fun search(space: Space, query: String): Flow<List<EntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: EntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<EntryEntity>)

    @Delete
    suspend fun delete(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE filePath = :filePath")
    suspend fun deleteByPath(filePath: String)

    @Query("SELECT COUNT(*) FROM entries WHERE space = :space")
    suspend fun count(space: Space): Int

    @Query("SELECT COUNT(*) FROM entries")
    suspend fun countAll(): Int

    // ── Statistics queries ──

    /** Entry count grouped by dateFolder, ordered by date descending. */
    @Query(
        """
        SELECT dateFolder, COUNT(*) as count
        FROM entries
        WHERE space = :space
        GROUP BY dateFolder
        ORDER BY dateFolder DESC
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

    /** Entry count grouped by hour of day (extracted from timestamp HHmmss). */
    @Query(
        """
        SELECT CAST(SUBSTR(timestamp, 1, 2) AS INTEGER) as hour, COUNT(*) as count
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
