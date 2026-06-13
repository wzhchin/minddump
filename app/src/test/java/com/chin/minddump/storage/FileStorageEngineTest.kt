package com.chin.minddump.storage

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
        val monthDir = File(rootDir, "Public/2024-01").also { it.mkdirs() }
        val file1 =
            File(monthDir, "2401-01-010000-f.md").also {
                it.writeText("first")
                it.setLastModified(1000)
            }
        val file2 =
            File(monthDir, "2401-01-020000-f.md").also {
                it.writeText("second")
                it.setLastModified(2000)
            }
        val file3 =
            File(monthDir, "2401-01-030000-f.md").also {
                it.writeText("third")
                it.setLastModified(500)
            }

        val entries = engine.scanEntries(Space.PUBLIC)
        assertEquals(3, entries.size)
        assertEquals(file2, entries[0].file) // newest first
        assertEquals(file1, entries[1].file)
        assertEquals(file3, entries[2].file) // oldest last
    }

    @Test
    fun `scanEntries detects correct entry types by extension`() {
        val monthDir = File(rootDir, "Public/2024-01").also { it.mkdirs() }
        File(monthDir, "2401-01-010000-f.md").writeText("text")
        File(monthDir, "2401-01-020000-f.m4a").writeText("audio")
        File(monthDir, "2401-01-030000-f.jpg").writeText("photo")
        File(monthDir, "2401-01-040000-f.mp4").writeText("video")
        File(monthDir, "2401-01-050000-f-report.pdf").writeText("file")

        val entries = engine.scanEntries(Space.PUBLIC)
        assertEquals(5, entries.size)

        val byName = entries.associateBy { it.file.name }
        assertEquals(EntryType.TEXT, byName["2401-01-010000-f.md"]?.type)
        assertEquals(EntryType.RECORDING, byName["2401-01-020000-f.m4a"]?.type)
        assertEquals(EntryType.PHOTO, byName["2401-01-030000-f.jpg"]?.type)
        assertEquals(EntryType.VIDEO, byName["2401-01-040000-f.mp4"]?.type)
        assertEquals(EntryType.FILE, byName["2401-01-050000-f-report.pdf"]?.type)
    }

    @Test
    fun `scanEntries skips non-MindDump files`() {
        val monthDir = File(rootDir, "Public/2024-01").also { it.mkdirs() }
        File(monthDir, "2401-01-010000-f.md").writeText("valid")
        File(monthDir, "readme.txt").writeText("skip me")
        File(monthDir, "文字_010000.md").writeText("old format")

        val entries = engine.scanEntries(Space.PUBLIC)
        assertEquals(1, entries.size)
        assertEquals("2401-01-010000-f.md", entries[0].file.name)
    }

    @Test
    fun `scanEntries ignores Private when scanning Public`() {
        val pubDir = File(rootDir, "Public/2024-01").also { it.mkdirs() }
        val privDir = File(rootDir, "Private/2024-01").also { it.mkdirs() }
        File(pubDir, "2401-01-010000-f.md").writeText("public note")
        File(privDir, "2401-01-020000-f.md").writeText("private note")

        val entries = engine.scanEntries(Space.PUBLIC)
        assertEquals(1, entries.size)
        assertEquals("public note", entries[0].file.readText())
    }

    @Test
    fun `saveTextEntry creates correct file`() {
        val file = engine.saveTextEntry(Space.PUBLIC, "Hello world")

        assertTrue(file.exists())
        assertEquals("Hello world", file.readText())
        assertTrue(file.name.contains("-f."))
        assertTrue(file.name.endsWith(".md"))
        // File should be under Public/YYYY-MM/
        assertTrue(file.parentFile?.parentFile?.name == "Public")
    }

    @Test
    fun `getRecordingFile returns m4a file in correct directory`() {
        val file = engine.getRecordingFile(Space.PRIVATE)

        assertTrue(file.name.contains("-f."))
        assertTrue(file.name.endsWith(".m4a"))
        assertTrue(file.parentFile?.parentFile?.name == "Private")
    }

    @Test
    fun `getPhotoFile returns jpg file in correct directory`() {
        val file = engine.getPhotoFile(Space.PUBLIC)

        assertTrue(file.name.contains("-f."))
        assertTrue(file.name.endsWith(".jpg"))
    }

    @Test
    fun `getVideoFile returns mp4 file in correct directory`() {
        val file = engine.getVideoFile(Space.PUBLIC)

        assertTrue(file.name.contains("-f."))
        assertTrue(file.name.endsWith(".mp4"))
    }

    @Test
    fun `deleteEntry removes file from disk`() {
        val file = engine.saveTextEntry(Space.PUBLIC, "to be deleted")
        val entry =
            MindDumpEntry(
                file = file,
                type = EntryType.TEXT,
                space = Space.PUBLIC,
                monthFolder = file.parentFile!!.name,
                timestamp = "2401-01-010000",
            )

        val result = engine.deleteEntry(entry)

        assertTrue(result)
        assertFalse(file.exists())
    }

    @Test
    fun `countFiles returns total file count recursively`() {
        val monthDir = File(rootDir, "Public/2024-01").also { it.mkdirs() }
        File(monthDir, "2401-01-010000-f.md").writeText("a")
        File(monthDir, "2401-01-020000-f.md").writeText("b")
        val monthDir2 = File(rootDir, "Private/2024-01").also { it.mkdirs() }
        File(monthDir2, "2401-01-030000-f.md").writeText("c")

        assertEquals(3, engine.countFiles())
    }

    @Test
    fun `countFiles returns 0 for non-existent directory`() {
        rootDir.deleteRecursively()
        assertEquals(0, engine.countFiles())
    }

    @Test
    fun `createGroup creates directory with correct naming`() {
        val groupDir = engine.createGroup(Space.PUBLIC, "travel")
        assertTrue(groupDir.exists())
        assertTrue(groupDir.isDirectory)
        assertTrue(groupDir.name.contains("-g-travel"))
        assertTrue(groupDir.parentFile?.parentFile?.name == "Public")
    }

    @Test
    fun `createGroup creates anonymous group`() {
        val groupDir = engine.createGroup(Space.PUBLIC, null)
        assertTrue(groupDir.exists())
        assertTrue(groupDir.name.endsWith("-g"))
    }

    @Test
    fun `moveToGroup moves file into group directory`() {
        val file = engine.saveTextEntry(Space.PUBLIC, "group me")
        val groupDir = engine.createGroup(Space.PUBLIC, null)

        val moved = engine.moveToGroup(file, groupDir)
        assertTrue(moved.exists())
        assertEquals(moved.parentFile, groupDir)
        assertFalse(file.exists())
    }

    @Test
    fun `saveComment creates file with correct naming`() {
        val monthDir = File(rootDir, "Public/2024-01").also { it.mkdirs() }
        File(monthDir, "2401-01-010000-f.md").also { it.writeText("target") }

        val comment = engine.saveComment(monthDir, "2401-01-010000", "a comment")
        assertTrue(comment.exists())
        assertTrue(comment.name.startsWith("2401-01-010000-n-"))
        assertTrue(comment.name.endsWith(".md"))
        assertEquals("a comment", comment.readText())
    }

    @Test
    fun `renameEntry changes originalName portion`() {
        val monthDir = File(rootDir, "Public/2024-01").also { it.mkdirs() }
        val file = File(monthDir, "2401-01-010000-f-oldname.pdf").also { it.writeText("data") }

        val renamed = engine.renameEntry(file, "newname")
        assertTrue(renamed.name.contains("-f-newname."))
        assertTrue(renamed.exists())
        assertFalse(file.exists())
    }

    @Test
    fun `migrateTo copies files and deletes original`() {
        val monthDir = File(rootDir, "Public/2024-01").also { it.mkdirs() }
        File(monthDir, "2401-01-010000-f.md").writeText("migrate me")

        val newRoot = tempFolder.newFolder("NewMindDump")
        engine.migrateTo(newRoot)

        val newFile = File(newRoot, "Public/2024-01/2401-01-010000-f.md")
        assertTrue(newFile.exists())
        assertEquals("migrate me", newFile.readText())
        assertFalse(File(rootDir, "Public").exists())
    }
}
