package com.chin.minddump.storage

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileStorageEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var engine: FileStorageEngine
    private lateinit var rootDir: File

    @Before
    fun setUp() {
        rootDir = tempFolder.newFolder("MindDump")

        val prefs = mockk<SharedPreferences>(relaxed = true)
        every { prefs.getString("work_dir", null) } returns rootDir.absolutePath
        every { prefs.contains("work_dir") } returns true

        val context = mockk<Context>()
        every { context.getSharedPreferences("minddump_prefs", Context.MODE_PRIVATE) } returns prefs

        engine = FileStorageEngine(context)
    }

    @Test
    fun `scanEntries returns empty list for empty directory`() {
        val entries = engine.scanEntries(Space.PUBLIC)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `scanEntries returns entries sorted by lastModified newest first`() {
        // Create files with different timestamps
        val dateDir = File(rootDir, "Public/2024-01-01").also { it.mkdirs() }
        val file1 = File(dateDir, "文字_010000.md").also { it.writeText("first"); it.setLastModified(1000) }
        val file2 = File(dateDir, "文字_020000.md").also { it.writeText("second"); it.setLastModified(2000) }
        val file3 = File(dateDir, "文字_030000.md").also { it.writeText("third"); it.setLastModified(500) }

        val entries = engine.scanEntries(Space.PUBLIC)
        assertEquals(3, entries.size)
        assertEquals(file2, entries[0].file) // newest first
        assertEquals(file1, entries[1].file)
        assertEquals(file3, entries[2].file) // oldest last
    }

    @Test
    fun `scanEntries detects correct entry types`() {
        val dateDir = File(rootDir, "Public/2024-01-01").also { it.mkdirs() }
        File(dateDir, "文字_010000.md").writeText("text")
        File(dateDir, "录音_020000.m4a").writeText("audio")
        File(dateDir, "拍照_030000.jpg").writeText("photo")
        File(dateDir, "视频_040000.mp4").writeText("video")
        File(dateDir, "文件_050000_doc.pdf").writeText("file")
        File(dateDir, "random.txt").writeText("unknown")

        val entries = engine.scanEntries(Space.PUBLIC)
        assertEquals(6, entries.size)

        val types = entries.map { it.type }.toSet()
        assertTrue(types.contains(EntryType.TEXT))
        assertTrue(types.contains(EntryType.RECORDING))
        assertTrue(types.contains(EntryType.PHOTO))
        assertTrue(types.contains(EntryType.VIDEO))
        assertTrue(types.contains(EntryType.FILE))
        assertTrue(types.contains(EntryType.UNKNOWN))
    }

    @Test
    fun `scanEntries ignores Private when scanning Public`() {
        val pubDir = File(rootDir, "Public/2024-01-01").also { it.mkdirs() }
        val privDir = File(rootDir, "Private/2024-01-01").also { it.mkdirs() }
        File(pubDir, "文字_010000.md").writeText("public note")
        File(privDir, "文字_020000.md").writeText("private note")

        val entries = engine.scanEntries(Space.PUBLIC)
        assertEquals(1, entries.size)
        assertEquals("public note", entries[0].file.readText())
    }

    @Test
    fun `saveTextEntry creates correct file`() {
        val file = engine.saveTextEntry(Space.PUBLIC, "Hello world")

        assertTrue(file.exists())
        assertEquals("Hello world", file.readText())
        assertTrue(file.name.startsWith("文字_"))
        assertTrue(file.name.endsWith(".md"))
        // File should be under Public/YYYY-MM-DD/
        assertTrue(file.parentFile?.parentFile?.name == "Public")
    }

    @Test
    fun `getRecordingFile returns m4a file in correct directory`() {
        val file = engine.getRecordingFile(Space.PRIVATE)

        assertTrue(file.name.startsWith("录音_"))
        assertTrue(file.name.endsWith(".m4a"))
        assertTrue(file.parentFile?.parentFile?.name == "Private")
    }

    @Test
    fun `getPhotoFile returns jpg file in correct directory`() {
        val file = engine.getPhotoFile(Space.PUBLIC)

        assertTrue(file.name.startsWith("拍照_"))
        assertTrue(file.name.endsWith(".jpg"))
    }

    @Test
    fun `getVideoFile returns mp4 file in correct directory`() {
        val file = engine.getVideoFile(Space.PUBLIC)

        assertTrue(file.name.startsWith("视频_"))
        assertTrue(file.name.endsWith(".mp4"))
    }

    @Test
    fun `deleteEntry removes file from disk`() {
        val file = engine.saveTextEntry(Space.PUBLIC, "to be deleted")
        val entry = MindDumpEntry(
            file = file,
            type = EntryType.TEXT,
            space = Space.PUBLIC,
            dateFolder = file.parentFile!!.name,
            timestamp = "000000"
        )

        val result = engine.deleteEntry(entry)

        assertTrue(result)
        assertFalse(file.exists())
    }

    @Test
    fun `countFiles returns total file count recursively`() {
        val dateDir = File(rootDir, "Public/2024-01-01").also { it.mkdirs() }
        File(dateDir, "文字_010000.md").writeText("a")
        File(dateDir, "文字_020000.md").writeText("b")
        val dateDir2 = File(rootDir, "Private/2024-01-02").also { it.mkdirs() }
        File(dateDir2, "文字_030000.md").writeText("c")

        assertEquals(3, engine.countFiles())
    }

    @Test
    fun `countFiles returns 0 for non-existent directory`() {
        val engineWithEmpty = engine
        rootDir.deleteRecursively()
        assertEquals(0, engineWithEmpty.countFiles())
    }

    @Test
    fun `migrateTo copies files and deletes original`() {
        // Create original structure
        val dateDir = File(rootDir, "Public/2024-01-01").also { it.mkdirs() }
        File(dateDir, "文字_010000.md").writeText("migrate me")

        // Migrate to new location
        val newRoot = tempFolder.newFolder("NewMindDump")
        engine.migrateTo(newRoot)

        // Check new location has the files
        val newFile = File(newRoot, "Public/2024-01-01/文字_010000.md")
        assertTrue(newFile.exists())
        assertEquals("migrate me", newFile.readText())

        // Check original is gone
        assertFalse(File(rootDir, "Public").exists())
    }
}
