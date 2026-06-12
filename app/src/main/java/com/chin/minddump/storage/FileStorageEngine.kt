package com.chin.minddump.storage

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles all file operations for MindDump.
 * Directory layout: /sdcard/MindDump/{Public,Private}/YYYY-MM-DD/
 */
class FileStorageEngine(private val context: Context) {

    companion object {
        private const val ROOT_DIR = "MindDump"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val TIME_FORMAT = SimpleDateFormat("HHmmss", Locale.getDefault())

        fun getRootDir(): File {
            return File(Environment.getExternalStorageDirectory(), ROOT_DIR)
        }

        fun getSpaceDir(space: Space, date: String): File {
            return File(getRootDir(), "${space.folderName}/$date")
        }

        fun getTodayDir(space: Space): File {
            return getSpaceDir(space, DATE_FORMAT.format(Date()))
        }
    }

    /**
     * Check if we have external storage access.
     */
    fun hasStoragePermission(): Boolean {
        return Environment.isExternalStorageManager()
    }

    /**
     * Ensure the directory for today exists for the given space.
     */
    fun ensureTodayDir(space: Space): File {
        val dir = getTodayDir(space)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Save a text entry as Markdown.
     */
    fun saveTextEntry(space: Space, content: String): File {
        val dir = ensureTodayDir(space)
        val timestamp = TIME_FORMAT.format(Date())
        val file = File(dir, "文字_$timestamp.md")
        file.writeText(content)
        return file
    }

    /**
     * Get the output file for a recording.
     */
    fun getRecordingFile(space: Space): File {
        val dir = ensureTodayDir(space)
        val timestamp = TIME_FORMAT.format(Date())
        return File(dir, "录音_$timestamp.m4a")
    }

    /**
     * Get the output file for a photo.
     */
    fun getPhotoFile(space: Space): File {
        val dir = ensureTodayDir(space)
        val timestamp = TIME_FORMAT.format(Date())
        return File(dir, "拍照_$timestamp.jpg")
    }

    /**
     * Get the output file for a video.
     */
    fun getVideoFile(space: Space): File {
        val dir = ensureTodayDir(space)
        val timestamp = TIME_FORMAT.format(Date())
        return File(dir, "视频_$timestamp.mp4")
    }

    /**
     * Import a file from a content URI.
     */
    fun importFile(space: Space, uri: Uri, originalFileName: String): File {
        val dir = ensureTodayDir(space)
        val timestamp = TIME_FORMAT.format(Date())
        val destFile = File(dir, "文件_${timestamp}_$originalFileName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        return destFile
    }

    /**
     * Scan all entries for a given space, sorted by lastModified (newest first).
     */
    fun scanEntries(space: Space): List<MindDumpEntry> {
        val spaceDir = File(getRootDir(), space.folderName)
        if (!spaceDir.exists()) return emptyList()

        val entries = mutableListOf<MindDumpEntry>()

        spaceDir.listFiles()
            ?.filter { it.isDirectory }
            ?.forEach { dateDir ->
                val dateFolder = dateDir.name
                dateDir.listFiles()
                    ?.filter { it.isFile }
                    ?.forEach { file ->
                        val type = EntryType.fromFileName(file.name)
                        entries.add(
                            MindDumpEntry(
                                file = file,
                                type = type,
                                space = space,
                                dateFolder = dateFolder,
                                timestamp = extractTimestamp(file.name)
                            )
                        )
                    }
            }

        return entries.sortedByDescending { it.file.lastModified() }
    }

    /**
     * Delete an entry file.
     */
    fun deleteEntry(entry: MindDumpEntry): Boolean {
        return entry.file.delete()
    }

    private fun extractTimestamp(fileName: String): String {
        // Pattern: 类型_HHmmss.ext or 文件_HHmmss_原文件名
        val parts = fileName.split("_")
        return if (parts.size >= 2) parts[1] else ""
    }
}
