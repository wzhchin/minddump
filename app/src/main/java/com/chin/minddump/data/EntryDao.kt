package com.chin.minddump.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chin.minddump.storage.Space
import kotlinx.coroutines.flow.Flow

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
}
