package com.chin.minddump.storage

import java.io.File

/**
 * Represents a single entry (file) in MindDump.
 */
data class MindDumpEntry(
    val file: File,
    val type: EntryType,
    val space: Space,
    val dateFolder: String, // YYYY-MM-DD
    val timestamp: String,  // HHmmss
)

enum class Space(val folderName: String) {
    PUBLIC("Public"),
    PRIVATE("Private")
}

enum class EntryType(val prefix: String) {
    TEXT("文字"),
    RECORDING("录音"),
    PHOTO("拍照"),
    VIDEO("视频"),
    FILE("文件"),
    UNKNOWN("未知");

    companion object {
        fun fromFileName(name: String): EntryType {
            return when {
                name.startsWith("文字_") && name.endsWith(".md") -> TEXT
                name.startsWith("录音_") && (name.endsWith(".m4a") || name.endsWith(".aac")) -> RECORDING
                name.startsWith("拍照_") && (name.endsWith(".jpg") || name.endsWith(".jpeg")) -> PHOTO
                name.startsWith("视频_") && name.endsWith(".mp4") -> VIDEO
                name.startsWith("文件_") -> FILE
                else -> UNKNOWN
            }
        }
    }
}
