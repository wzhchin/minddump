package com.chin.minddump.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
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
    // ── entries (content + group containers) ──

    @Query("SELECT * FROM entries WHERE space = :space ORDER BY lastModified DESC")
    fun getAll(space: Space): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE space = :space")
    suspend fun getAllSnapshot(space: Space): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE space = :space AND type = :type ORDER BY lastModified DESC")
    fun getByType(space: Space, type: EntryType): Flow<List<EntryEntity>>

    /** Group container rows (directory entries). */
    @Query("SELECT * FROM entries WHERE space = :space AND type = 'GROUP' ORDER BY lastModified DESC")
    fun getGroups(space: Space): Flow<List<EntryEntity>>

    /** Direct members of a group (groupPath = the group's path), or all root-level entries when null. */
    @Query("SELECT * FROM entries WHERE space = :space AND groupPath IS :groupPath ORDER BY lastModified DESC")
    fun getMembers(space: Space, groupPath: String?): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE groupPath IS :groupPath")
    suspend fun getMembersSnapshot(groupPath: String?): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE filePath = :filePath LIMIT 1")
    suspend fun findByPath(filePath: String): EntryEntity?

    // Substring search via GLOB on the raw contentPreview column. GLOB compares
    // the original text directly, so CJK characters match as a contiguous run
    // without needing a tokenizer. Both sides LOWER()-ed for ASCII
    // case-insensitivity; the caller passes the query escaped and wrapped.
    @Query(
        """
        SELECT * FROM entries
        WHERE space = :space AND LOWER(contentPreview) GLOB LOWER(:pattern)
        ORDER BY lastModified DESC
        """,
    )
    fun search(space: Space, pattern: String): Flow<List<EntryEntity>>

    /** Owners in [space] carrying [tag]; the repository joins tags to entries by filePath. */
    @Query(
        """
        SELECT DISTINCT e.* FROM entries e
        INNER JOIN tags t ON e.filePath = t.filePath
        WHERE e.space = :space AND t.tag = :tag
        ORDER BY e.lastModified DESC
        """,
    )
    fun getByTag(space: Space, tag: String): Flow<List<EntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: EntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<EntryEntity>)

    @Delete
    suspend fun delete(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE filePath = :filePath")
    suspend fun deleteByPath(filePath: String)

    @Query("DELETE FROM entries WHERE filePath IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    @Query("DELETE FROM entries")
    suspend fun clearEntries()

    @Query("SELECT COUNT(*) FROM entries WHERE space = :space")
    suspend fun count(space: Space): Int

    @Query("SELECT COUNT(*) FROM entries")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM entries WHERE space = :space")
    fun countFlow(space: Space): Flow<Int>

    // ── tags ──

    @Query("SELECT tag FROM tags WHERE filePath = :filePath")
    suspend fun tagsFor(filePath: String): List<String>

    @Query(
        """
        SELECT DISTINCT tag FROM tags
        WHERE filePath IN (SELECT filePath FROM entries WHERE space = :space)
        ORDER BY tag
        """,
    )
    suspend fun distinctTags(space: Space): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Query("DELETE FROM tags WHERE filePath = :filePath")
    suspend fun deleteTagsFor(filePath: String)

    @Query("DELETE FROM tags")
    suspend fun clearTags()

    /** Replace an owner's tag rows atomically. */
    suspend fun setTags(filePath: String, tags: List<String>) {
        deleteTagsFor(filePath)
        if (tags.isNotEmpty()) insertTags(tags.map { TagEntity(filePath, it) })
    }

    // ── events ──

    @Query("SELECT * FROM events WHERE filePath = :filePath")
    suspend fun eventsFor(filePath: String): List<EventEntity>

    @Query("SELECT * FROM events WHERE state = 'PENDING'")
    suspend fun pendingEvents(): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("DELETE FROM events WHERE filePath = :filePath")
    suspend fun deleteEventsFor(filePath: String)

    @Query("DELETE FROM events")
    suspend fun clearEvents()

    /** Replace an owner's event rows atomically, returning the inserted rows (with ids). */
    suspend fun setEvents(filePath: String, events: List<EventEntity>): List<EventEntity> {
        deleteEventsFor(filePath)
        return if (events.isEmpty()) emptyList() else events.also { insertEvents(it) }
    }

    // ── all tables ──

    /** Drop every row across entries/tags/events — used by the full rebuild-from-disk path. */
    suspend fun clearAll() {
        clearTags()
        clearEvents()
        clearEntries()
    }

    /** Force a full rebuild of the FTS index from the content table. */
    @Query("INSERT INTO entries_fts(entries_fts) VALUES('rebuild')")
    suspend fun rebuildFtsIndex()

    // ── Statistics queries ──
    // lastModified is epoch-millis (the file mtime); derive date/hour from it via
    // strftime (unix epoch in seconds = lastModified/1000). 'localtime' converts
    // UTC epoch to the device timezone, matching how the file was captured.

    /** Entry count grouped by YYYY-MM month folder, ordered by month descending. */
    @Query(
        """
        SELECT monthFolder, COUNT(*) as count
        FROM entries
        WHERE space = :space AND type != 'GROUP'
        GROUP BY monthFolder
        ORDER BY monthFolder DESC
        LIMIT :limit
        """,
    )
    fun getEntryCountByDay(space: Space, limit: Int = 90): Flow<List<DayCount>>

    /** Entry count grouped by type (group containers excluded). */
    @Query(
        """
        SELECT type, COUNT(*) as count
        FROM entries
        WHERE space = :space AND type != 'GROUP'
        GROUP BY type
        """,
    )
    fun getEntryCountByType(space: Space): Flow<List<TypeCount>>

    /** Entry count grouped by hour of day, derived from the epoch-millis lastModified. */
    @Query(
        """
        SELECT CAST(strftime('%H', lastModified / 1000, 'unixepoch', 'localtime') AS INTEGER) as hour,
               COUNT(*) as count
        FROM entries
        WHERE space = :space AND type != 'GROUP'
        GROUP BY hour
        ORDER BY hour
        """,
    )
    fun getHourlyDistribution(space: Space): Flow<List<HourCount>>
}
