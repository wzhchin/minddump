package com.chin.minddump.data

import androidx.room3.Entity
import androidx.room3.Fts4

@Fts4(contentEntity = EntryEntity::class)
@Entity(tableName = "entries_fts")
data class EntryFts(
    val contentPreview: String,
    val monthFolder: String,
)
