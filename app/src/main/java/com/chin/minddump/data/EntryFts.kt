package com.chin.minddump.data

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = EntryEntity::class)
@Entity(tableName = "entries_fts")
data class EntryFts(
    val contentPreview: String,
    val dateFolder: String,
)
